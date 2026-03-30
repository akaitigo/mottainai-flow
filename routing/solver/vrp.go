// Package solver implements Vehicle Routing Problem (VRP) algorithms.
package solver

import (
	"github.com/akaitigo/mottainai-flow/routing/solver/distance"
)

// Point represents a geographic location with an ID and time window.
type Point struct {
	ID        string
	Latitude  float64
	Longitude float64
	IsPickup  bool
}

// Route represents an ordered sequence of points assigned to one vehicle.
type Route struct {
	VehicleIndex  int
	Points        []Point
	TotalDistance float64
}

// Solution represents the complete routing solution.
type Solution struct {
	Routes        []Route
	TotalDistance float64
}

// Solver defines the interface for VRP solvers.
type Solver interface {
	Solve(depot Point, waypoints []Point, vehicleCount int) Solution
}

// NearestNeighborSolver implements VRP using Nearest Neighbor + 2-opt improvement.
type NearestNeighborSolver struct{}

// NewSolver creates a new VRP solver instance.
func NewSolver() Solver {
	return &NearestNeighborSolver{}
}

// Solve computes an optimized route using Nearest Neighbor heuristic
// followed by 2-opt improvement.
func (s *NearestNeighborSolver) Solve(depot Point, waypoints []Point, vehicleCount int) Solution {
	if len(waypoints) == 0 {
		return Solution{
			Routes:        []Route{{VehicleIndex: 0, Points: []Point{depot}, TotalDistance: 0}},
			TotalDistance: 0,
		}
	}

	if vehicleCount <= 0 {
		vehicleCount = 1
	}

	// Build distance matrix including depot at index 0
	allPoints := make([]Point, 0, len(waypoints)+1)
	allPoints = append(allPoints, depot)
	allPoints = append(allPoints, waypoints...)

	coords := make([][2]float64, len(allPoints))
	for i, p := range allPoints {
		coords[i] = [2]float64{p.Latitude, p.Longitude}
	}
	distMatrix := distance.Matrix(coords)

	// Assign waypoints to vehicles using round-robin nearest neighbor
	assignments := assignWaypoints(distMatrix, len(waypoints), vehicleCount)

	routes := make([]Route, vehicleCount)
	totalDist := 0.0

	for v := range vehicleCount {
		assigned := assignments[v]
		if len(assigned) == 0 {
			routes[v] = Route{VehicleIndex: v, TotalDistance: 0}
			continue
		}

		// Build route using nearest neighbor
		order := nearestNeighborRoute(distMatrix, assigned)

		// Improve with 2-opt
		order = twoOpt(distMatrix, order)

		// Calculate total distance including depot return
		routePoints := make([]Point, len(order))
		routeDist := distMatrix[0][order[0]] // depot to first
		for i, idx := range order {
			routePoints[i] = allPoints[idx]
			if i > 0 {
				routeDist += distMatrix[order[i-1]][idx]
			}
		}
		routeDist += distMatrix[order[len(order)-1]][0] // last to depot

		routes[v] = Route{
			VehicleIndex:  v,
			Points:        routePoints,
			TotalDistance: routeDist,
		}
		totalDist += routeDist
	}

	return Solution{
		Routes:        routes,
		TotalDistance: totalDist,
	}
}

// assignWaypoints distributes waypoints to vehicles using greedy nearest-first.
func assignWaypoints(distMatrix [][]float64, waypointCount, vehicleCount int) [][]int {
	assignments := make([][]int, vehicleCount)
	for i := range assignments {
		assignments[i] = make([]int, 0)
	}

	visited := make([]bool, waypointCount+1)
	visited[0] = true // depot

	// Track each vehicle's current position (starts at depot = 0)
	currentPos := make([]int, vehicleCount)

	for assigned := 0; assigned < waypointCount; assigned++ {
		vehicle := assigned % vehicleCount

		bestIdx := -1
		bestDist := 0.0

		for i := 1; i <= waypointCount; i++ {
			if visited[i] {
				continue
			}
			d := distMatrix[currentPos[vehicle]][i]
			if bestIdx == -1 || d < bestDist {
				bestIdx = i
				bestDist = d
			}
		}

		if bestIdx != -1 {
			visited[bestIdx] = true
			assignments[vehicle] = append(assignments[vehicle], bestIdx)
			currentPos[vehicle] = bestIdx
		}
	}

	return assignments
}

// nearestNeighborRoute orders waypoint indices using nearest neighbor heuristic.
// Starts from depot (index 0).
func nearestNeighborRoute(distMatrix [][]float64, waypointIndices []int) []int {
	if len(waypointIndices) <= 1 {
		return waypointIndices
	}

	remaining := make(map[int]bool, len(waypointIndices))
	for _, idx := range waypointIndices {
		remaining[idx] = true
	}

	order := make([]int, 0, len(waypointIndices))
	current := 0 // start at depot

	for len(remaining) > 0 {
		bestIdx := -1
		bestDist := 0.0

		for idx := range remaining {
			d := distMatrix[current][idx]
			if bestIdx == -1 || d < bestDist {
				bestIdx = idx
				bestDist = d
			}
		}

		order = append(order, bestIdx)
		delete(remaining, bestIdx)
		current = bestIdx
	}

	return order
}

// twoOpt improves a route by reversing segments that reduce total distance.
func twoOpt(distMatrix [][]float64, route []int) []int {
	if len(route) < 3 {
		return route
	}

	improved := true
	for improved {
		improved = false
		for i := 0; i < len(route)-1; i++ {
			for j := i + 2; j < len(route); j++ {
				if improvesRoute(distMatrix, route, i, j) {
					reverseSegment(route, i+1, j)
					improved = true
				}
			}
		}
	}

	return route
}

// improvesRoute checks if reversing the segment between i+1 and j reduces distance.
func improvesRoute(distMatrix [][]float64, route []int, i, j int) bool {
	prevI := 0 // depot
	if i > 0 {
		prevI = route[i-1]
	}

	nextJ := 0 // back to depot
	if j < len(route)-1 {
		nextJ = route[j+1]
	}

	oldDist := distMatrix[prevI][route[i]] + distMatrix[route[j]][nextJ]
	newDist := distMatrix[prevI][route[j]] + distMatrix[route[i]][nextJ]

	return newDist < oldDist
}

// reverseSegment reverses elements in route[left:right+1].
func reverseSegment(route []int, left, right int) {
	for left < right {
		route[left], route[right] = route[right], route[left]
		left++
		right--
	}
}
