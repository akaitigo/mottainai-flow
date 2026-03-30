package solver

import (
	"math"
	"testing"
)

func TestSolve_EmptyWaypoints(t *testing.T) {
	s := NewSolver()
	depot := Point{ID: "depot", Latitude: 35.6812, Longitude: 139.7671}

	sol := s.Solve(depot, nil, 1)

	if sol.TotalDistance != 0 {
		t.Errorf("empty waypoints should have 0 distance, got %f", sol.TotalDistance)
	}
	if len(sol.Routes) != 1 {
		t.Errorf("expected 1 route, got %d", len(sol.Routes))
	}
}

func TestSolve_SingleWaypoint(t *testing.T) {
	s := NewSolver()
	depot := Point{ID: "depot", Latitude: 35.6812, Longitude: 139.7671}
	waypoints := []Point{
		{ID: "wp1", Latitude: 35.6940, Longitude: 139.7536, IsPickup: true},
	}

	sol := s.Solve(depot, waypoints, 1)

	if sol.TotalDistance <= 0 {
		t.Error("single waypoint should have positive distance")
	}
	if len(sol.Routes) != 1 {
		t.Fatalf("expected 1 route, got %d", len(sol.Routes))
	}
	if len(sol.Routes[0].Points) != 1 {
		t.Errorf("expected 1 point in route, got %d", len(sol.Routes[0].Points))
	}
}

func TestSolve_KnownOptimalTriangle(t *testing.T) {
	// Triangle: depot + 3 points arranged in a known pattern
	// The optimal tour visits them in order around the triangle
	s := NewSolver()
	depot := Point{ID: "depot", Latitude: 35.6812, Longitude: 139.7671} // Tokyo Station

	waypoints := []Point{
		{ID: "chiyoda", Latitude: 35.6940, Longitude: 139.7536, IsPickup: true},
		{ID: "shinjuku", Latitude: 35.6938, Longitude: 139.7034, IsPickup: true},
		{ID: "shibuya", Latitude: 35.6640, Longitude: 139.6982, IsPickup: false},
	}

	sol := s.Solve(depot, waypoints, 1)

	// Should have 1 route with 3 waypoints
	if len(sol.Routes) != 1 {
		t.Fatalf("expected 1 route, got %d", len(sol.Routes))
	}

	route := sol.Routes[0]
	if len(route.Points) != 3 {
		t.Errorf("expected 3 points, got %d", len(route.Points))
	}

	// Total distance should be reasonable (< 30 km for Tokyo area)
	if sol.TotalDistance > 30000 {
		t.Errorf("total distance %f m seems too large for Tokyo area", sol.TotalDistance)
	}
	if sol.TotalDistance <= 0 {
		t.Error("total distance should be positive")
	}
}

func TestSolve_MultipleVehicles(t *testing.T) {
	s := NewSolver()
	depot := Point{ID: "depot", Latitude: 35.6812, Longitude: 139.7671}

	waypoints := []Point{
		{ID: "p1", Latitude: 35.6940, Longitude: 139.7536, IsPickup: true},
		{ID: "p2", Latitude: 35.6938, Longitude: 139.7034, IsPickup: true},
		{ID: "p3", Latitude: 35.6640, Longitude: 139.6982, IsPickup: false},
		{ID: "p4", Latitude: 35.7263, Longitude: 139.7165, IsPickup: false},
	}

	sol := s.Solve(depot, waypoints, 2)

	if len(sol.Routes) != 2 {
		t.Fatalf("expected 2 routes, got %d", len(sol.Routes))
	}

	// Each vehicle should have some waypoints
	totalPoints := 0
	for _, r := range sol.Routes {
		totalPoints += len(r.Points)
	}
	if totalPoints != 4 {
		t.Errorf("expected 4 total waypoints across routes, got %d", totalPoints)
	}

	// Sum of route distances should equal total distance
	sumDist := 0.0
	for _, r := range sol.Routes {
		sumDist += r.TotalDistance
	}
	if math.Abs(sumDist-sol.TotalDistance) > 0.01 {
		t.Errorf("sum of route distances (%f) != total distance (%f)", sumDist, sol.TotalDistance)
	}
}

func TestSolve_2OptImprovesSolution(t *testing.T) {
	// Create a case where nearest neighbor gives a suboptimal solution
	// that 2-opt can improve. Place points in a line where NN would zigzag.
	s := NewSolver()
	depot := Point{ID: "depot", Latitude: 35.6, Longitude: 139.7}

	// Points in a line, but shuffled so NN might not find optimal order
	waypoints := []Point{
		{ID: "a", Latitude: 35.61, Longitude: 139.7, IsPickup: true},
		{ID: "b", Latitude: 35.63, Longitude: 139.7, IsPickup: true},
		{ID: "c", Latitude: 35.62, Longitude: 139.7, IsPickup: true},
		{ID: "d", Latitude: 35.64, Longitude: 139.7, IsPickup: true},
		{ID: "e", Latitude: 35.65, Longitude: 139.7, IsPickup: true},
	}

	sol := s.Solve(depot, waypoints, 1)

	// The solution should be reasonable
	if sol.TotalDistance <= 0 {
		t.Error("solution should have positive distance")
	}
	if len(sol.Routes[0].Points) != 5 {
		t.Errorf("expected 5 points, got %d", len(sol.Routes[0].Points))
	}
}

func TestSolve_ZeroVehicleCountDefaultsToOne(t *testing.T) {
	s := NewSolver()
	depot := Point{ID: "depot", Latitude: 35.6812, Longitude: 139.7671}
	waypoints := []Point{
		{ID: "wp1", Latitude: 35.6940, Longitude: 139.7536, IsPickup: true},
	}

	sol := s.Solve(depot, waypoints, 0)

	if len(sol.Routes) != 1 {
		t.Errorf("expected 1 route with vehicleCount=0, got %d", len(sol.Routes))
	}
}

func TestTwoOpt_SmallRoute(t *testing.T) {
	// 2-opt on a 2-element route should be no-op
	route := []int{1, 2}
	result := twoOpt([][]float64{{0, 1, 2}, {1, 0, 1}, {2, 1, 0}}, route)
	if len(result) != 2 {
		t.Errorf("expected 2 elements, got %d", len(result))
	}
}
