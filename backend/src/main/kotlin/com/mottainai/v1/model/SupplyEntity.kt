package com.mottainai.v1.model

import java.time.Instant
import java.util.UUID

/**
 * Domain entity representing a surplus food supply entry.
 */
data class SupplyEntity(
    val id: UUID = UUID.randomUUID(),
    val providerId: String,
    val itemName: String,
    val category: Int,
    val quantity: Int,
    val unit: String,
    val expiryDate: Instant,
    val pickupWindowStart: Instant,
    val pickupWindowEnd: Instant,
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
