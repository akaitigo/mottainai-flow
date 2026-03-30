// Package distance provides geographic distance calculations.
package distance

import "math"

const earthRadiusMeters = 6371000.0

// Haversine calculates the great-circle distance in meters between two points
// given their latitude and longitude in decimal degrees.
func Haversine(lat1, lon1, lat2, lon2 float64) float64 {
	dLat := degreesToRadians(lat2 - lat1)
	dLon := degreesToRadians(lon2 - lon1)

	radLat1 := degreesToRadians(lat1)
	radLat2 := degreesToRadians(lat2)

	a := math.Sin(dLat/2)*math.Sin(dLat/2) +
		math.Cos(radLat1)*math.Cos(radLat2)*math.Sin(dLon/2)*math.Sin(dLon/2)

	c := 2 * math.Atan2(math.Sqrt(a), math.Sqrt(1-a))

	return earthRadiusMeters * c
}

// Matrix builds a symmetric distance matrix for a list of (lat, lon) pairs.
// Returns an NxN matrix where entry [i][j] is the Haversine distance in meters.
func Matrix(points [][2]float64) [][]float64 {
	n := len(points)
	mat := make([][]float64, n)

	for i := range n {
		mat[i] = make([]float64, n)
		for j := range n {
			if i == j {
				mat[i][j] = 0
			} else if j < i {
				mat[i][j] = mat[j][i]
			} else {
				mat[i][j] = Haversine(
					points[i][0], points[i][1],
					points[j][0], points[j][1],
				)
			}
		}
	}

	return mat
}

func degreesToRadians(deg float64) float64 {
	return deg * math.Pi / 180.0
}
