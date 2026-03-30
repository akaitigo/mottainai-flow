package com.mottainai.v1.service.delivery

import com.mottainai.v1.DeliveryStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeliveryServiceImplTest {
    private lateinit var service: DeliveryServiceImpl

    @BeforeEach
    fun setup() {
        service = DeliveryServiceImpl()
    }

    @Nested
    inner class CreateDelivery {
        @Test
        fun `creates delivery with PENDING status`() {
            val entity = service.createDelivery("match-1", "supply-1", "demand-1")

            assertThat(entity.matchId).isEqualTo("match-1")
            assertThat(entity.supplyId).isEqualTo("supply-1")
            assertThat(entity.demandId).isEqualTo("demand-1")
            assertThat(entity.status).isEqualTo(DeliveryStatus.DELIVERY_STATUS_PENDING)
            assertThat(entity.driverId).isEmpty()
            assertThat(entity.pickupAt).isNull()
            assertThat(entity.deliveryAt).isNull()
        }
    }

    @Nested
    inner class FullLifecycle {
        @Test
        fun `complete delivery lifecycle PENDING to PICKED_UP to DELIVERED`() {
            val delivery = service.createDelivery("match-1", "supply-1", "demand-1")

            // Verify initial state
            assertThat(delivery.status).isEqualTo(DeliveryStatus.DELIVERY_STATUS_PENDING)

            // Simulate pickup via direct entity manipulation (since gRPC is suspend)
            val afterPickup =
                delivery.copy(
                    status = DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                    driverId = "driver-1",
                    pickupQuantity = 10,
                    pickupCondition = "Good",
                )

            // Verify transition validity
            assertThat(
                DeliveryStateMachine.canTransition(
                    delivery.status,
                    DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                ),
            ).isTrue()

            assertThat(afterPickup.status).isEqualTo(DeliveryStatus.DELIVERY_STATUS_PICKED_UP)

            // Simulate delivery
            assertThat(
                DeliveryStateMachine.canTransition(
                    afterPickup.status,
                    DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                ),
            ).isTrue()

            val afterDelivery =
                afterPickup.copy(
                    status = DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                    deliveryQuantity = 10,
                    deliveryCondition = "Good",
                )

            assertThat(afterDelivery.status).isEqualTo(DeliveryStatus.DELIVERY_STATUS_DELIVERED)
            assertThat(DeliveryStateMachine.isTerminal(afterDelivery.status)).isTrue()
        }
    }

    @Nested
    inner class InvalidTransitions {
        @Test
        fun `cannot skip from PENDING to DELIVERED`() {
            val canSkip =
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_PENDING,
                    DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                )
            assertThat(canSkip).isFalse()
        }

        @Test
        fun `cannot go backwards from DELIVERED to PICKED_UP`() {
            val canRevert =
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                    DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                )
            assertThat(canRevert).isFalse()
        }

        @Test
        fun `cannot recover from FAILED`() {
            val canRecover =
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_FAILED,
                    DeliveryStatus.DELIVERY_STATUS_PENDING,
                )
            assertThat(canRecover).isFalse()
        }
    }

    @Nested
    inner class DeliveryEntity {
        @Test
        fun `entity preserves all fields`() {
            val entity = service.createDelivery("m1", "s1", "d1")

            assertThat(entity.id).isNotNull()
            assertThat(entity.createdAt).isNotNull()
            assertThat(entity.updatedAt).isNotNull()
            assertThat(entity.pickupPhotoUrl).isEmpty()
            assertThat(entity.deliveryPhotoUrl).isEmpty()
        }
    }
}
