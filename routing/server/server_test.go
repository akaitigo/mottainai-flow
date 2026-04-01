package server

import (
	"context"
	"fmt"
	"strings"
	"testing"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	pb "github.com/akaitigo/mottainai-flow/gen/go/mottainai/v1"
)

func makeWaypoints(n int) []*pb.Waypoint {
	wps := make([]*pb.Waypoint, n)
	for i := range n {
		wps[i] = &pb.Waypoint{
			Id:        fmt.Sprintf("wp-%d", i),
			Latitude:  35.68 + float64(i)*0.001,
			Longitude: 139.76 + float64(i)*0.001,
			IsPickup:  i%2 == 0,
		}
	}
	return wps
}

func TestOptimizeRoute_WaypointLimitExceeded(t *testing.T) {
	srv := NewRoutingServer()

	req := &pb.OptimizeRouteRequest{
		RouteId:      "route-dos",
		Depot:        &pb.Waypoint{Id: "depot", Latitude: 35.68, Longitude: 139.76},
		Waypoints:    makeWaypoints(MaxWaypoints + 1),
		VehicleCount: 1,
	}

	_, err := srv.OptimizeRoute(context.Background(), req)
	if err == nil {
		t.Fatal("expected error for exceeding waypoint limit, got nil")
	}

	st, ok := status.FromError(err)
	if !ok {
		t.Fatalf("expected gRPC status error, got: %v", err)
	}
	if st.Code() != codes.InvalidArgument {
		t.Errorf("expected code InvalidArgument, got %v", st.Code())
	}
	if !strings.Contains(st.Message(), "exceeds maximum") {
		t.Errorf("expected message to mention limit, got: %q", st.Message())
	}
}

func TestOptimizeRoute_WaypointAtLimit(t *testing.T) {
	srv := NewRoutingServer()

	req := &pb.OptimizeRouteRequest{
		RouteId:      "route-at-limit",
		Depot:        &pb.Waypoint{Id: "depot", Latitude: 35.68, Longitude: 139.76},
		Waypoints:    makeWaypoints(MaxWaypoints),
		VehicleCount: 1,
	}

	resp, err := srv.OptimizeRoute(context.Background(), req)
	if err != nil {
		t.Fatalf("expected no error at exact limit, got: %v", err)
	}
	if resp.GetRouteId() != "route-at-limit" {
		t.Errorf("expected route_id %q, got %q", "route-at-limit", resp.GetRouteId())
	}
}

func TestOptimizeRoute_WaypointBelowLimit(t *testing.T) {
	srv := NewRoutingServer()

	req := &pb.OptimizeRouteRequest{
		RouteId:      "route-normal",
		Depot:        &pb.Waypoint{Id: "depot", Latitude: 35.68, Longitude: 139.76},
		Waypoints:    makeWaypoints(5),
		VehicleCount: 1,
	}

	resp, err := srv.OptimizeRoute(context.Background(), req)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if resp.GetRouteId() != "route-normal" {
		t.Errorf("expected route_id %q, got %q", "route-normal", resp.GetRouteId())
	}
}
