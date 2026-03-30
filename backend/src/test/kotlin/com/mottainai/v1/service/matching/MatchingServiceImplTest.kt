package com.mottainai.v1.service.matching

import com.mottainai.v1.model.DemandEntity
import com.mottainai.v1.model.SupplyEntity
import com.mottainai.v1.repository.InMemoryDemandRepository
import com.mottainai.v1.repository.InMemorySupplyRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class MatchingServiceImplTest {
    private lateinit var service: MatchingServiceImpl
    private lateinit var supplyRepo: InMemorySupplyRepository
    private lateinit var demandRepo: InMemoryDemandRepository

    private val baseTime = Instant.parse("2026-04-01T09:00:00Z")

    @BeforeEach
    fun setup() {
        supplyRepo = InMemorySupplyRepository()
        demandRepo = InMemoryDemandRepository()
        service = MatchingServiceImpl()
    }

    private fun createSupply(
        category: Int = 2,
        lat: Double = 35.6812,
        lon: Double = 139.7671,
    ): SupplyEntity {
        val entity =
            SupplyEntity(
                providerId = "p1",
                itemName = "りんご",
                category = category,
                quantity = 10,
                unit = "kg",
                expiryDate = baseTime.plus(7, ChronoUnit.DAYS),
                pickupWindowStart = baseTime,
                pickupWindowEnd = baseTime.plus(4, ChronoUnit.HOURS),
                postalCode = "100-0001",
                prefecture = "東京都",
                city = "千代田区",
                street = "丸の内",
                latitude = lat,
                longitude = lon,
            )
        return supplyRepo.insert(entity)
    }

    private fun createDemand(
        category: Int = 2,
        lat: Double = 35.6940,
        lon: Double = 139.7536,
    ): DemandEntity {
        val entity =
            DemandEntity(
                recipientId = "r1",
                category = category,
                desiredQuantity = 5,
                unit = "kg",
                deliveryWindowStart = baseTime,
                deliveryWindowEnd = baseTime.plus(4, ChronoUnit.HOURS),
                postalCode = "100-0002",
                prefecture = "東京都",
                city = "千代田区",
                street = "神田",
                latitude = lat,
                longitude = lon,
            )
        return demandRepo.insert(entity)
    }

    @Test
    fun `findMatches returns matches for nearby same-category items`() {
        val supply = createSupply()
        val demand = createDemand()

        val matches =
            service.findMatches(
                listOf(supply),
                listOf(demand),
                50000.0,
                0.5,
            )

        assertThat(matches).hasSize(1)
        assertThat(matches[0].supplyId).isEqualTo(supply.id.toString())
        assertThat(matches[0].demandId).isEqualTo(demand.id.toString())
        assertThat(matches[0].totalScore).isGreaterThan(0.5)
        assertThat(matches[0].categoryScore).isEqualTo(1.0)
    }

    @Test
    fun `findMatches excludes far away items`() {
        val supply = createSupply(lat = 35.6812, lon = 139.7671) // Tokyo
        val demand = createDemand(lat = 34.6937, lon = 135.5023) // Osaka

        val matches =
            service.findMatches(
                listOf(supply),
                listOf(demand),
                10000.0,
                0.0,
            )

        assertThat(matches).isEmpty()
    }

    @Test
    fun `findMatches excludes different categories with high threshold`() {
        val supply = createSupply(category = 2) // FRUITS
        val demand = createDemand(category = 4) // MEAT

        val matches =
            service.findMatches(
                listOf(supply),
                listOf(demand),
                50000.0,
                0.9,
            )

        assertThat(matches).isEmpty()
    }

    @Test
    fun `findMatches performs greedy 1-to-1 assignment`() {
        val s1 = createSupply(lat = 35.6812, lon = 139.7671)
        val s2 = createSupply(lat = 35.6940, lon = 139.7536)
        val d1 = createDemand(lat = 35.6812, lon = 139.7671) // Same as s1

        val matches =
            service.findMatches(
                listOf(s1, s2),
                listOf(d1),
                50000.0,
                0.0,
            )

        // Only 1 demand, so only 1 match
        assertThat(matches).hasSize(1)
    }

    @Test
    fun `findMatches handles empty inputs`() {
        val matches = service.findMatches(emptyList(), emptyList(), 50000.0, 0.0)
        assertThat(matches).isEmpty()
    }

    @Test
    fun `findMatches orders results by total score descending`() {
        val s1 = createSupply(lat = 35.6812, lon = 139.7671)
        val s2 = createSupply(lat = 35.7263, lon = 139.7165)
        val d1 = createDemand(lat = 35.6812, lon = 139.7671) // Exact match with s1
        val d2 = createDemand(lat = 35.7263, lon = 139.7165) // Exact match with s2

        val matches =
            service.findMatches(
                listOf(s1, s2),
                listOf(d1, d2),
                50000.0,
                0.0,
            )

        assertThat(matches).hasSize(2)
        // Both should have high scores (exact location match + same category)
        assertThat(matches[0].totalScore).isGreaterThanOrEqualTo(matches[1].totalScore)
    }
}
