package com.mottainai.v1.service.delivery

import com.mottainai.v1.DeliveryStatus

/**
 * State machine for delivery status transitions.
 * Valid transitions: PENDING -> PICKED_UP -> DELIVERED | FAILED
 *                    PENDING -> FAILED
 */
object DeliveryStateMachine {
    private val validTransitions: Map<DeliveryStatus, Set<DeliveryStatus>> =
        mapOf(
            DeliveryStatus.DELIVERY_STATUS_PENDING to
                setOf(
                    DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                    DeliveryStatus.DELIVERY_STATUS_FAILED,
                ),
            DeliveryStatus.DELIVERY_STATUS_PICKED_UP to
                setOf(
                    DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                    DeliveryStatus.DELIVERY_STATUS_FAILED,
                ),
        )

    /**
     * Checks if a transition from [current] to [target] is valid.
     */
    fun canTransition(
        current: DeliveryStatus,
        target: DeliveryStatus,
    ): Boolean {
        val allowed = validTransitions[current] ?: return false
        return target in allowed
    }

    /**
     * Returns the set of valid next statuses from the given [current] status.
     */
    fun validNextStatuses(current: DeliveryStatus): Set<DeliveryStatus> = validTransitions[current] ?: emptySet()

    /**
     * Returns true if the status is a terminal state (no further transitions).
     */
    fun isTerminal(status: DeliveryStatus): Boolean =
        status == DeliveryStatus.DELIVERY_STATUS_DELIVERED ||
            status == DeliveryStatus.DELIVERY_STATUS_FAILED
}
