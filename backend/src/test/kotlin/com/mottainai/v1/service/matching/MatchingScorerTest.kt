package com.mottainai.v1.service.matching

import com.mottainai.v1.model.DemandEntity
import com.mottainai.v1.model.SupplyEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class MatchingScorerTest {
    private val baseTime = Instant.parse("2026-04-01T09:00:00Z")

    private fun supply(
        lat: Double = 35.6812,
        lon: Double = 139.7671,
        category: Int = 2,
        pickupStart: Instant = baseTime,
        pickupEnd: Instant = baseTime.plus(4, ChronoUnit.HOURS),
    ) = SupplyEntity(
        providerId = "p1",
        itemName = "りんご",
        category = category,
        quantity = 10,
        unit = "kg",
        expiryDate = baseTime.plus(7, ChronoUnit.DAYS),
        pickupWindowStart = pickupStart,
        pickupWindowEnd = pickupEnd,
        postalCode = "100-0001",
        prefecture = "東京都",
        city = "千代田区",
        street = "丸の内",
        latitude = lat,
        longitude = lon,
    )

    private fun demand(
        lat: Double = 35.6940,
        lon: Double = 139.7536,
        category: Int = 2,
        deliveryStart: Instant = baseTime,
        deliveryEnd: Instant = baseTime.plus(4, ChronoUnit.HOURS),
    ) = DemandEntity(
        recipientId = "r1",
        category = category,
        desiredQuantity = 5,
        unit = "kg",
        deliveryWindowStart = deliveryStart,
        deliveryWindowEnd = deliveryEnd,
        postalCode = "100-0002",
        prefecture = "東京都",
        city = "千代田区",
        street = "神田",
        latitude = lat,
        longitude = lon,
    )

    @Nested
    inner class DistanceScoreTests {
        @Test
        fun `nearby locations have high distance score`() {
            val s = supply(lat = 35.6812, lon = 139.7671)
            val d = demand(lat = 35.6940, lon = 139.7536)
            val score = MatchingScorer.distanceScore(s, d, 50000.0)
            assertThat(score).isGreaterThan(0.9)
        }

        @Test
        fun `distant locations have lower distance score`() {
            val s = supply(lat = 35.6812, lon = 139.7671) // Tokyo
            val d = demand(lat = 34.6937, lon = 135.5023) // Osaka
            val score = MatchingScorer.distanceScore(s, d, 500000.0)
            assertThat(score).isBetween(0.1, 0.3)
        }

        @Test
        fun `locations beyond max distance return zero`() {
            val s = supply(lat = 35.6812, lon = 139.7671) // Tokyo
            val d = demand(lat = 34.6937, lon = 135.5023) // Osaka (~400km)
            val score = MatchingScorer.distanceScore(s, d, 10000.0)
            assertThat(score).isEqualTo(0.0)
        }

        @Test
        fun `same location returns max score`() {
            val s = supply(lat = 35.6812, lon = 139.7671)
            val d = demand(lat = 35.6812, lon = 139.7671)
            val score = MatchingScorer.distanceScore(s, d, 50000.0)
            assertThat(score).isCloseTo(1.0, Offset.offset(0.001))
        }
    }

    @Nested
    inner class TimeOverlapScoreTests {
        @Test
        fun `full overlap returns 1_0`() {
            val s = supply(pickupStart = baseTime, pickupEnd = baseTime.plus(4, ChronoUnit.HOURS))
            val d = demand(deliveryStart = baseTime, deliveryEnd = baseTime.plus(4, ChronoUnit.HOURS))
            assertThat(MatchingScorer.timeOverlapScore(s, d)).isCloseTo(1.0, Offset.offset(0.001))
        }

        @Test
        fun `partial overlap returns fraction`() {
            val s =
                supply(
                    pickupStart = baseTime,
                    pickupEnd = baseTime.plus(4, ChronoUnit.HOURS),
                )
            val d =
                demand(
                    deliveryStart = baseTime.plus(2, ChronoUnit.HOURS),
                    deliveryEnd = baseTime.plus(6, ChronoUnit.HOURS),
                )
            val score = MatchingScorer.timeOverlapScore(s, d)
            assertThat(score).isCloseTo(0.5, Offset.offset(0.001))
        }

        @Test
        fun `no overlap returns 0_0`() {
            val s =
                supply(
                    pickupStart = baseTime,
                    pickupEnd = baseTime.plus(2, ChronoUnit.HOURS),
                )
            val d =
                demand(
                    deliveryStart = baseTime.plus(3, ChronoUnit.HOURS),
                    deliveryEnd = baseTime.plus(5, ChronoUnit.HOURS),
                )
            assertThat(MatchingScorer.timeOverlapScore(s, d)).isEqualTo(0.0)
        }
    }

    @Nested
    inner class CategoryScoreTests {
        @Test
        fun `matching categories return 1_0`() {
            val s = supply(category = 2) // FRUITS
            val d = demand(category = 2) // FRUITS
            assertThat(MatchingScorer.categoryScore(s, d)).isEqualTo(1.0)
        }

        @Test
        fun `different categories return 0_0`() {
            val s = supply(category = 2) // FRUITS
            val d = demand(category = 3) // DAIRY
            assertThat(MatchingScorer.categoryScore(s, d)).isEqualTo(0.0)
        }
    }

    @Nested
    inner class TotalScoreTests {
        @Test
        fun `perfect match produces high total score`() {
            val total = MatchingScorer.totalScore(1.0, 1.0, 1.0)
            assertThat(total).isCloseTo(1.0, Offset.offset(0.001))
        }

        @Test
        fun `zero on all factors produces zero`() {
            val total = MatchingScorer.totalScore(0.0, 0.0, 0.0)
            assertThat(total).isEqualTo(0.0)
        }

        @Test
        fun `weights sum to 1_0`() {
            // distance=0.4, time=0.35, category=0.25
            val total = MatchingScorer.totalScore(1.0, 1.0, 1.0)
            assertThat(total).isCloseTo(1.0, Offset.offset(0.001))
        }

        @Test
        fun `category only gives 0_25`() {
            val total = MatchingScorer.totalScore(0.0, 0.0, 1.0)
            assertThat(total).isCloseTo(0.25, Offset.offset(0.001))
        }
    }

    @Nested
    inner class DistanceMetersTests {
        @Test
        fun `distance between nearby points is reasonable`() {
            val s = supply(lat = 35.6812, lon = 139.7671) // Tokyo Station
            val d = demand(lat = 35.6940, lon = 139.7536) // Chiyoda
            val meters = MatchingScorer.distanceMeters(s, d)
            assertThat(meters).isBetween(1500.0, 2500.0)
        }
    }
}
