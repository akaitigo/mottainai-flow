package com.mottainai.v1.service.delivery

import com.mottainai.v1.DeliveryStatus
import java.time.Instant
import java.util.UUID

/**
 * Domain entity representing a delivery record.
 */
data class DeliveryEntity(
    val id: UUID = UUID.randomUUID(),
    val matchId: String,
    val supplyId: String,
    val demandId: String,
    val driverId: String = "",
    val status: DeliveryStatus = DeliveryStatus.DELIVERY_STATUS_PENDING,
    val pickupPhotoUrl: String = "",
    val deliveryPhotoUrl: String = "",
    val pickupQuantity: Int = 0,
    val deliveryQuantity: Int = 0,
    val pickupCondition: String = "",
    val deliveryCondition: String = "",
    val pickupAt: Instant? = null,
    val deliveryAt: Instant? = null,
    val notes: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
