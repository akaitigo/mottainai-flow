package com.mottainai.v1.service.delivery

import com.google.protobuf.Timestamp
import com.mottainai.v1.DeliveryRecord
import com.mottainai.v1.TraceEvent
import java.time.Instant

/**
 * Maps between DeliveryEntity and protobuf messages.
 */
internal object DeliveryMapper {
    fun toProto(entity: DeliveryEntity): DeliveryRecord {
        val builder =
            DeliveryRecord
                .newBuilder()
                .setId(entity.id.toString())
                .setMatchId(entity.matchId)
                .setSupplyId(entity.supplyId)
                .setDemandId(entity.demandId)
                .setDriverId(entity.driverId)
                .setStatus(entity.status)
                .setPickupPhotoUrl(entity.pickupPhotoUrl)
                .setDeliveryPhotoUrl(entity.deliveryPhotoUrl)
                .setPickupQuantity(entity.pickupQuantity)
                .setDeliveryQuantity(entity.deliveryQuantity)
                .setPickupCondition(entity.pickupCondition)
                .setDeliveryCondition(entity.deliveryCondition)
                .setNotes(entity.notes)
                .setCreatedAt(toTimestamp(entity.createdAt))
                .setUpdatedAt(toTimestamp(entity.updatedAt))

        entity.pickupAt?.let { builder.setPickupAt(toTimestamp(it)) }
        entity.deliveryAt?.let { builder.setDeliveryAt(toTimestamp(it)) }

        return builder.build()
    }

    fun buildTraceEvents(entity: DeliveryEntity): List<TraceEvent> {
        val events = mutableListOf<TraceEvent>()

        events.add(
            TraceEvent
                .newBuilder()
                .setTimestamp(toTimestamp(entity.createdAt))
                .setEventType("CREATED")
                .setDescription("Delivery created from match ${entity.matchId}")
                .build(),
        )

        if (entity.pickupAt != null) {
            events.add(
                TraceEvent
                    .newBuilder()
                    .setTimestamp(toTimestamp(entity.pickupAt))
                    .setEventType("PICKED_UP")
                    .setDescription(
                        "Picked up ${entity.pickupQuantity} items. " +
                            "Condition: ${entity.pickupCondition}",
                    ).setActorId(entity.driverId)
                    .build(),
            )
        }

        if (entity.deliveryAt != null) {
            events.add(
                TraceEvent
                    .newBuilder()
                    .setTimestamp(toTimestamp(entity.deliveryAt))
                    .setEventType("DELIVERED")
                    .setDescription(
                        "Delivered ${entity.deliveryQuantity} items. " +
                            "Condition: ${entity.deliveryCondition}",
                    ).setActorId(entity.driverId)
                    .build(),
            )
        }

        return events
    }

    fun toTimestamp(instant: Instant): Timestamp =
        Timestamp
            .newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()
}
