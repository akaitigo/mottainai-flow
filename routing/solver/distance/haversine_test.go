package distance

import (
	"math"
	"testing"
)

func TestHaversine_TokyoToOsaka(t *testing.T) {
	// Tokyo Station: 35.6812, 139.7671
	// Osaka Station: 34.7024, 135.4959
	d := Haversine(35.6812, 139.7671, 34.7024, 135.4959)

	// Expected: ~403 km (great-circle distance)
	expectedKm := 403.0
	tolerance := 5.0 // 5 km tolerance

	actualKm := d / 1000.0
	if math.Abs(actualKm-expectedKm) > tolerance {
		t.Errorf("Tokyo-Osaka distance = %.1f km, want ~%.1f km (±%.1f)", actualKm, expectedKm, tolerance)
	}
}

func TestHaversine_SamePoint(t *testing.T) {
	d := Haversine(35.6812, 139.7671, 35.6812, 139.7671)
	if d != 0 {
		t.Errorf("same point distance = %f, want 0", d)
	}
}

func TestHaversine_ShortDistance(t *testing.T) {
	// Chiyoda-ku to Shinjuku-ku: ~6.3 km
	d := Haversine(35.6940, 139.7536, 35.6938, 139.7034)
	actualKm := d / 1000.0
	if actualKm < 4.0 || actualKm > 8.0 {
		t.Errorf("Chiyoda-Shinjuku distance = %.1f km, want ~5-7 km", actualKm)
	}
}

func TestMatrix_BasicProperties(t *testing.T) {
	points := [][2]float64{
		{35.6812, 139.7671}, // Tokyo
		{34.7024, 135.4959}, // Osaka
		{35.1815, 136.9066}, // Nagoya
	}

	mat := Matrix(points)

	if len(mat) != 3 {
		t.Fatalf("matrix size = %d, want 3", len(mat))
	}

	// Diagonal should be 0
	for i := range 3 {
		if mat[i][i] != 0 {
			t.Errorf("mat[%d][%d] = %f, want 0", i, i, mat[i][i])
		}
	}

	// Should be symmetric
	for i := range 3 {
		for j := range 3 {
			if mat[i][j] != mat[j][i] {
				t.Errorf("mat[%d][%d]=%f != mat[%d][%d]=%f", i, j, mat[i][j], j, i, mat[j][i])
			}
		}
	}

	// Tokyo-Osaka > Tokyo-Nagoya
	if mat[0][1] <= mat[0][2] {
		t.Errorf("Tokyo-Osaka (%f) should be > Tokyo-Nagoya (%f)", mat[0][1], mat[0][2])
	}
}
