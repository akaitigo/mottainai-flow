package com.mottainai.v1.service.delivery

import com.mottainai.v1.DeliveryStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeliveryStateMachineTest {
    @Nested
    inner class TransitionValidation {
        @Test
        fun `PENDING can transition to PICKED_UP`() {
            assertThat(
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_PENDING,
                    DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                ),
            ).isTrue()
        }

        @Test
        fun `PENDING can transition to FAILED`() {
            assertThat(
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_PENDING,
                    DeliveryStatus.DELIVERY_STATUS_FAILED,
                ),
            ).isTrue()
        }

        @Test
        fun `PICKED_UP can transition to DELIVERED`() {
            assertThat(
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                    DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                ),
            ).isTrue()
        }

        @Test
        fun `PICKED_UP can transition to FAILED`() {
            assertThat(
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                    DeliveryStatus.DELIVERY_STATUS_FAILED,
                ),
            ).isTrue()
        }

        @Test
        fun `PENDING cannot transition to DELIVERED directly`() {
            assertThat(
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_PENDING,
                    DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                ),
            ).isFalse()
        }

        @Test
        fun `DELIVERED cannot transition to any status`() {
            assertThat(
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                    DeliveryStatus.DELIVERY_STATUS_PENDING,
                ),
            ).isFalse()
        }

        @Test
        fun `FAILED cannot transition to any status`() {
            assertThat(
                DeliveryStateMachine.canTransition(
                    DeliveryStatus.DELIVERY_STATUS_FAILED,
                    DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                ),
            ).isFalse()
        }
    }

    @Nested
    inner class TerminalState {
        @Test
        fun `DELIVERED is terminal`() {
            assertThat(DeliveryStateMachine.isTerminal(DeliveryStatus.DELIVERY_STATUS_DELIVERED)).isTrue()
        }

        @Test
        fun `FAILED is terminal`() {
            assertThat(DeliveryStateMachine.isTerminal(DeliveryStatus.DELIVERY_STATUS_FAILED)).isTrue()
        }

        @Test
        fun `PENDING is not terminal`() {
            assertThat(DeliveryStateMachine.isTerminal(DeliveryStatus.DELIVERY_STATUS_PENDING)).isFalse()
        }

        @Test
        fun `PICKED_UP is not terminal`() {
            assertThat(DeliveryStateMachine.isTerminal(DeliveryStatus.DELIVERY_STATUS_PICKED_UP)).isFalse()
        }
    }

    @Nested
    inner class ValidNextStatuses {
        @Test
        fun `PENDING has PICKED_UP and FAILED as next`() {
            val next = DeliveryStateMachine.validNextStatuses(DeliveryStatus.DELIVERY_STATUS_PENDING)
            assertThat(next).containsExactlyInAnyOrder(
                DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                DeliveryStatus.DELIVERY_STATUS_FAILED,
            )
        }

        @Test
        fun `DELIVERED has no next statuses`() {
            val next = DeliveryStateMachine.validNextStatuses(DeliveryStatus.DELIVERY_STATUS_DELIVERED)
            assertThat(next).isEmpty()
        }
    }
}
