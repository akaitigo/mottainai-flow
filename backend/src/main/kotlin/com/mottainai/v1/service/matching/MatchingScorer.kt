package com.mottainai.v1.service.matching

import com.mottainai.v1.model.DemandEntity
import com.mottainai.v1.model.SupplyEntity
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Calculates matching scores between supply and demand entries.
 * Scores range from 0.0 (worst) to 1.0 (best).
 */
object MatchingScorer {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val DISTANCE_WEIGHT = 0.4
    private const val TIME_OVERLAP_WEIGHT = 0.35
    private const val CATEGORY_WEIGHT = 0.25
    private const val DEGREES_TO_RADIANS = Math.PI / 180.0

    /**
     * Calculates the distance score (0.0 to 1.0) based on haversine distance.
     * Score decreases as distance increases. Returns 0.0 if beyond maxDistance.
     */
    fun distanceScore(
        supply: SupplyEntity,
        demand: DemandEntity,
        maxDistanceMeters: Double,
    ): Double {
        val meters =
            haversineMeters(
                supply.latitude,
                supply.longitude,
                demand.latitude,
                demand.longitude,
            )
        if (meters > maxDistanceMeters) return 0.0
        return 1.0 - (meters / maxDistanceMeters)
    }

    /**
     * Calculates the distance in meters between supply and demand locations.
     */
    fun distanceMeters(
        supply: SupplyEntity,
        demand: DemandEntity,
    ): Double =
        haversineMeters(
            supply.latitude,
            supply.longitude,
            demand.latitude,
            demand.longitude,
        )

    /**
     * Calculates the time window overlap score (0.0 to 1.0).
     * Based on the ratio of overlap duration to the shorter window duration.
     */
    fun timeOverlapScore(
        supply: SupplyEntity,
        demand: DemandEntity,
    ): Double {
        val overlapStart = maxInstant(supply.pickupWindowStart, demand.deliveryWindowStart)
        val overlapEnd = minInstant(supply.pickupWindowEnd, demand.deliveryWindowEnd)

        if (overlapStart >= overlapEnd) return 0.0

        val overlapDuration = overlapEnd.epochSecond - overlapStart.epochSecond
        val supplyDuration = supply.pickupWindowEnd.epochSecond - supply.pickupWindowStart.epochSecond
        val demandDuration = demand.deliveryWindowEnd.epochSecond - demand.deliveryWindowStart.epochSecond
        val shorterDuration = min(supplyDuration, demandDuration)

        return if (shorterDuration <= 0) 0.0 else min(overlapDuration.toDouble() / shorterDuration.toDouble(), 1.0)
    }

    /**
     * Calculates the category match score (0.0 or 1.0).
     */
    fun categoryScore(
        supply: SupplyEntity,
        demand: DemandEntity,
    ): Double = if (supply.category == demand.category) 1.0 else 0.0

    /**
     * Calculates the weighted total score combining all factors.
     */
    fun totalScore(
        distScore: Double,
        timeScore: Double,
        catScore: Double,
    ): Double = distScore * DISTANCE_WEIGHT + timeScore * TIME_OVERLAP_WEIGHT + catScore * CATEGORY_WEIGHT

    private fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = (lat2 - lat1) * DEGREES_TO_RADIANS
        val dLon = (lon2 - lon1) * DEGREES_TO_RADIANS
        val radLat1 = lat1 * DEGREES_TO_RADIANS
        val radLat2 = lat2 * DEGREES_TO_RADIANS

        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(radLat1) * cos(radLat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))

        return EARTH_RADIUS_METERS * c
    }

    private fun maxInstant(
        a: Instant,
        b: Instant,
    ): Instant = if (a > b) a else b

    private fun minInstant(
        a: Instant,
        b: Instant,
    ): Instant = if (a < b) a else b

    private operator fun Instant.compareTo(other: Instant): Int = this.compareTo(other)
}
