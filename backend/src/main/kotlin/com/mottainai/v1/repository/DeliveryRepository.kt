package com.mottainai.v1.repository

import com.mottainai.v1.DeliveryStatus
import com.mottainai.v1.service.delivery.DeliveryEntity

/**
 * Repository interface for delivery records.
 * Provides CRUD operations for delivery lifecycle management.
 */
interface DeliveryRepository {
    fun insert(entity: DeliveryEntity): DeliveryEntity

    fun findById(id: String): DeliveryEntity?

    /**
     * Updates a delivery record with optimistic locking.
     * @param entity the updated entity to persist
     * @param expectedStatus the status the row must currently have; prevents race conditions
     * @return true if exactly one row was updated, false if the precondition failed
     */
    fun update(
        entity: DeliveryEntity,
        expectedStatus: DeliveryStatus,
    ): Boolean
}
