// Package server provides the gRPC server for the routing service.
package server

import (
	"context"
	"fmt"
	"log"
	"net"
	"sync"
	"time"

	"github.com/akaitigo/mottainai-flow/routing/solver"
	"github.com/akaitigo/mottainai-flow/routing/solver/distance"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"

	pb "github.com/akaitigo/mottainai-flow/gen/go/mottainai/v1"
)

const (
	// MaxWaypoints is the maximum number of waypoints allowed per route optimization request.
	// Exceeding this limit results in InvalidArgument to prevent computational DoS.
	MaxWaypoints = 50

	// maxCachedRoutes is the maximum number of route results to cache in memory.
	maxCachedRoutes = 10000

	// routeCacheTTL is the time-to-live for cached route results.
	routeCacheTTL = 30 * time.Minute
)

// routeEntry holds a cached route result with expiration.
type routeEntry struct {
	response  *pb.OptimizeRouteResponse
	expiresAt time.Time
}

// RoutingServer implements the RoutingService gRPC interface.
type RoutingServer struct {
	pb.UnimplementedRoutingServiceServer
	solver solver.Solver
	mu     sync.RWMutex
	routes map[string]routeEntry
}

// NewRoutingServer creates a new RoutingServer.
func NewRoutingServer() *RoutingServer {
	return &RoutingServer{
		solver: solver.NewSolver(),
		routes: make(map[string]routeEntry),
	}
}

// OptimizeRoute computes an optimal route for the given waypoints.
func (s *RoutingServer) OptimizeRoute(
	_ context.Context,
	req *pb.OptimizeRouteRequest,
) (*pb.OptimizeRouteResponse, error) {
	if req.GetRouteId() == "" {
		return nil, status.Error(codes.InvalidArgument, "route_id is required")
	}
	if req.GetDepot() == nil {
		return nil, status.Error(codes.InvalidArgument, "depot is required")
	}
	if len(req.GetWaypoints()) == 0 {
		return nil, status.Error(codes.InvalidArgument, "at least one waypoint is required")
	}
	if len(req.GetWaypoints()) > MaxWaypoints {
		return nil, status.Errorf(codes.InvalidArgument,
			"waypoint count %d exceeds maximum of %d", len(req.GetWaypoints()), MaxWaypoints)
	}

	depot := solver.Point{
		ID:        req.GetDepot().GetId(),
		Latitude:  req.GetDepot().GetLatitude(),
		Longitude: req.GetDepot().GetLongitude(),
		IsPickup:  req.GetDepot().GetIsPickup(),
	}

	waypoints := make([]solver.Point, len(req.GetWaypoints()))
	for i, wp := range req.GetWaypoints() {
		waypoints[i] = solver.Point{
			ID:        wp.GetId(),
			Latitude:  wp.GetLatitude(),
			Longitude: wp.GetLongitude(),
			IsPickup:  wp.GetIsPickup(),
		}
	}

	vehicleCount := int(req.GetVehicleCount())
	if vehicleCount <= 0 {
		vehicleCount = 1
	}

	solution := s.solver.Solve(depot, waypoints, vehicleCount)

	resp := buildResponse(req.GetRouteId(), solution, depot)

	s.mu.Lock()
	// Evict expired entries and enforce max capacity before inserting.
	s.evictExpiredLocked()
	if len(s.routes) >= maxCachedRoutes {
		s.evictOldestLocked()
	}
	s.routes[req.GetRouteId()] = routeEntry{
		response:  resp,
		expiresAt: time.Now().Add(routeCacheTTL),
	}
	s.mu.Unlock()

	return resp, nil
}

// evictExpiredLocked removes all expired entries. Caller must hold s.mu write lock.
func (s *RoutingServer) evictExpiredLocked() {
	now := time.Now()
	for key, entry := range s.routes {
		if now.After(entry.expiresAt) {
			delete(s.routes, key)
		}
	}
}

// evictOldestLocked removes the entry with the earliest expiresAt. Caller must hold s.mu write lock.
func (s *RoutingServer) evictOldestLocked() {
	var oldestKey string
	var oldestTime time.Time
	first := true

	for key, entry := range s.routes {
		if first || entry.expiresAt.Before(oldestTime) {
			oldestKey = key
			oldestTime = entry.expiresAt
			first = false
		}
	}

	if !first {
		delete(s.routes, oldestKey)
	}
}

// GetRouteStatus retrieves the status and result of a route computation.
func (s *RoutingServer) GetRouteStatus(
	_ context.Context,
	req *pb.GetRouteStatusRequest,
) (*pb.GetRouteStatusResponse, error) {
	if req.GetRouteId() == "" {
		return nil, status.Error(codes.InvalidArgument, "route_id is required")
	}

	s.mu.RLock()
	entry, exists := s.routes[req.GetRouteId()]
	s.mu.RUnlock()

	if !exists || time.Now().After(entry.expiresAt) {
		return &pb.GetRouteStatusResponse{
			RouteId: req.GetRouteId(),
			Status:  pb.RouteStatus_ROUTE_STATUS_UNSPECIFIED,
		}, nil
	}

	return &pb.GetRouteStatusResponse{
		RouteId: req.GetRouteId(),
		Status:  entry.response.GetStatus(),
		Result:  entry.response,
	}, nil
}

func buildResponse(
	routeID string,
	solution solver.Solution,
	depot solver.Point,
) *pb.OptimizeRouteResponse {
	vehicleRoutes := make([]*pb.VehicleRoute, len(solution.Routes))

	for i, route := range solution.Routes {
		segments := buildSegments(depot, route.Points)
		order := make([]string, len(route.Points))
		for j, p := range route.Points {
			order[j] = p.ID
		}

		vehicleRoutes[i] = &pb.VehicleRoute{
			VehicleIndex:        int32(route.VehicleIndex),
			Segments:            segments,
			TotalDistanceMeters: route.TotalDistance,
			WaypointOrder:       order,
		}
	}

	return &pb.OptimizeRouteResponse{
		RouteId:             routeID,
		Status:              pb.RouteStatus_ROUTE_STATUS_COMPLETED,
		VehicleRoutes:       vehicleRoutes,
		TotalDistanceMeters: solution.TotalDistance,
	}
}

func buildSegments(depot solver.Point, points []solver.Point) []*pb.RouteSegment {
	if len(points) == 0 {
		return nil
	}

	segments := make([]*pb.RouteSegment, 0, len(points)+1)

	// Depot to first point
	segments = append(segments, &pb.RouteSegment{
		From:           toWaypointPb(depot),
		To:             toWaypointPb(points[0]),
		DistanceMeters: distance.Haversine(depot.Latitude, depot.Longitude, points[0].Latitude, points[0].Longitude),
	})

	// Point to point
	for i := 1; i < len(points); i++ {
		segments = append(segments, &pb.RouteSegment{
			From: toWaypointPb(points[i-1]),
			To:   toWaypointPb(points[i]),
			DistanceMeters: distance.Haversine(
				points[i-1].Latitude, points[i-1].Longitude,
				points[i].Latitude, points[i].Longitude,
			),
		})
	}

	// Last point back to depot
	last := points[len(points)-1]
	segments = append(segments, &pb.RouteSegment{
		From:           toWaypointPb(last),
		To:             toWaypointPb(depot),
		DistanceMeters: distance.Haversine(last.Latitude, last.Longitude, depot.Latitude, depot.Longitude),
	})

	return segments
}

func toWaypointPb(p solver.Point) *pb.Waypoint {
	return &pb.Waypoint{
		Id:          p.ID,
		Latitude:    p.Latitude,
		Longitude:   p.Longitude,
		IsPickup:    p.IsPickup,
		WindowStart: &timestamppb.Timestamp{},
		WindowEnd:   &timestamppb.Timestamp{},
	}
}

// Run starts the gRPC server on the specified port.
func Run(port string) error {
	lis, err := net.Listen("tcp", fmt.Sprintf(":%s", port))
	if err != nil {
		return fmt.Errorf("failed to listen: %w", err)
	}

	grpcServer := grpc.NewServer()
	pb.RegisterRoutingServiceServer(grpcServer, NewRoutingServer())

	log.Printf("Routing service listening on :%s", port)

	return grpcServer.Serve(lis)
}
