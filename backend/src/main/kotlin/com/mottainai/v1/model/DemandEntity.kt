package com.mottainai.v1.model

import java.time.Instant
import java.util.UUID

/**
 * Domain entity representing a food demand entry.
 */
data class DemandEntity(
    val id: UUID = UUID.randomUUID(),
    val recipientId: String,
    val category: Int,
    val desiredQuantity: Int,
    val unit: String,
    val deliveryWindowStart: Instant,
    val deliveryWindowEnd: Instant,
    val postalCode: String,
    val prefecture: String,
    val city: String,
    val street: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: Int = 1,
    val description: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
