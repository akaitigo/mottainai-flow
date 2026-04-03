package com.mottainai.v1.repository

import com.mottainai.v1.DeliveryStatus
import com.mottainai.v1.model.SupplyEntity
import com.mottainai.v1.service.delivery.DeliveryEntity
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@QuarkusTest
class PostgresDeliveryRepositoryTest {
    @Inject
    lateinit var repository: DeliveryRepository

    @Inject
    lateinit var supplyRepository: SupplyRepository

    @Inject
    lateinit var demandRepository: DemandRepository

    @Inject
    lateinit var dataSource: io.agroal.api.AgroalDataSource

    private val baseTime = Instant.parse("2026-04-01T09:00:00Z")

    private lateinit var supplyId: UUID
    private lateinit var demandId: UUID

    @BeforeEach
    fun cleanup() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM deliveries")
                stmt.execute("DELETE FROM supplies")
                stmt.execute("DELETE FROM demands")
            }
        }
        // Create supply and demand for FK references
        val supply =
            supplyRepository.insert(
                com.mottainai.v1.model.SupplyEntity(
                    providerId = "p1",
                    itemName = "りんご",
                    category = 2,
                    quantity = 10,
                    unit = "kg",
                    expiryDate = baseTime.plus(7, ChronoUnit.DAYS),
                    pickupWindowStart = baseTime,
                    pickupWindowEnd = baseTime.plus(4, ChronoUnit.HOURS),
                    postalCode = "100-0001",
                    prefecture = "東京都",
                    city = "千代田区",
                    street = "丸の内",
                ),
            )
        supplyId = supply.id

        val demand =
            demandRepository.insert(
                com.mottainai.v1.model.DemandEntity(
                    recipientId = "r1",
                    category = 2,
                    desiredQuantity = 5,
                    unit = "kg",
                    deliveryWindowStart = baseTime,
                    deliveryWindowEnd = baseTime.plus(4, ChronoUnit.HOURS),
                    postalCode = "100-0002",
                    prefecture = "東京都",
                    city = "千代田区",
                    street = "神田",
                ),
            )
        demandId = demand.id
    }

    private fun createDeliveryEntity(): DeliveryEntity =
        DeliveryEntity(
            matchId = "match-1",
            supplyId = supplyId.toString(),
            demandId = demandId.toString(),
        )

    @Nested
    inner class InsertAndFindById {
        @Test
        fun `insert and retrieve by id`() {
            val entity = createDeliveryEntity()
            repository.insert(entity)

            val found = repository.findById(entity.id.toString())
            assertThat(found).isNotNull
            requireNotNull(found)
            assertThat(found.id).isEqualTo(entity.id)
            assertThat(found.matchId).isEqualTo("match-1")
            assertThat(found.supplyId).isEqualTo(supplyId.toString())
            assertThat(found.demandId).isEqualTo(demandId.toString())
            assertThat(found.status).isEqualTo(DeliveryStatus.DELIVERY_STATUS_PENDING)
            assertThat(found.driverId).isEmpty()
            assertThat(found.pickupAt).isNull()
            assertThat(found.deliveryAt).isNull()
        }

        @Test
        fun `findById returns null for non-existent id`() {
            val found = repository.findById(UUID.randomUUID().toString())
            assertThat(found).isNull()
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update delivery status and fields`() {
            val entity = createDeliveryEntity()
            repository.insert(entity)

            val pickupTime = Instant.now()
            val updated =
                entity.copy(
                    driverId = "driver-1",
                    status = DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                    pickupPhotoUrl = "https://example.com/photo.jpg",
                    pickupQuantity = 10,
                    pickupCondition = "Good",
                    pickupAt = pickupTime,
                    notes = "Picked up successfully",
                    updatedAt = Instant.now(),
                )
            val success = repository.update(updated, DeliveryStatus.DELIVERY_STATUS_PENDING)
            assertThat(success).isTrue()

            val found = repository.findById(entity.id.toString())
            assertThat(found).isNotNull
            requireNotNull(found)
            assertThat(found.driverId).isEqualTo("driver-1")
            assertThat(found.status).isEqualTo(DeliveryStatus.DELIVERY_STATUS_PICKED_UP)
            assertThat(found.pickupPhotoUrl).isEqualTo("https://example.com/photo.jpg")
            assertThat(found.pickupQuantity).isEqualTo(10)
            assertThat(found.pickupCondition).isEqualTo("Good")
            assertThat(found.pickupAt).isNotNull
            assertThat(found.notes).isEqualTo("Picked up successfully")
        }

        @Test
        fun `update fails when expected status does not match`() {
            val entity = createDeliveryEntity()
            repository.insert(entity)

            val updated =
                entity.copy(
                    driverId = "driver-1",
                    status = DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                    updatedAt = Instant.now(),
                )
            // Entity is PENDING, but we claim it should be PICKED_UP -> must fail
            val success = repository.update(updated, DeliveryStatus.DELIVERY_STATUS_PICKED_UP)
            assertThat(success).isFalse()

            // Verify state unchanged
            val found = repository.findById(entity.id.toString())
            requireNotNull(found)
            assertThat(found.status).isEqualTo(DeliveryStatus.DELIVERY_STATUS_PENDING)
        }

        @Test
        fun `full lifecycle update from PENDING to DELIVERED`() {
            val entity = createDeliveryEntity()
            repository.insert(entity)

            // Pickup
            val afterPickup =
                entity.copy(
                    driverId = "driver-1",
                    status = DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                    pickupQuantity = 10,
                    pickupCondition = "Good",
                    pickupAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            assertThat(repository.update(afterPickup, DeliveryStatus.DELIVERY_STATUS_PENDING)).isTrue()

            // Delivery
            val afterDelivery =
                afterPickup.copy(
                    status = DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                    deliveryQuantity = 10,
                    deliveryCondition = "Good",
                    deliveryAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            assertThat(repository.update(afterDelivery, DeliveryStatus.DELIVERY_STATUS_PICKED_UP)).isTrue()

            val found = repository.findById(entity.id.toString())
            assertThat(found).isNotNull
            requireNotNull(found)
            assertThat(found.status).isEqualTo(DeliveryStatus.DELIVERY_STATUS_DELIVERED)
            assertThat(found.pickupAt).isNotNull
            assertThat(found.deliveryAt).isNotNull
        }
    }
}
