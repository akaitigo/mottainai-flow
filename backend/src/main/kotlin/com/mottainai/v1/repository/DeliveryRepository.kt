package com.mottainai.v1.repository

import com.mottainai.v1.service.delivery.DeliveryEntity

/**
 * Repository interface for delivery records.
 * Provides CRUD operations for delivery lifecycle management.
 */
interface DeliveryRepository {
    fun insert(entity: DeliveryEntity): DeliveryEntity

    fun findById(id: String): DeliveryEntity?

    fun update(entity: DeliveryEntity): DeliveryEntity
}
