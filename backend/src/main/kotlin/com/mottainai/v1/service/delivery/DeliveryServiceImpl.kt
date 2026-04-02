package com.mottainai.v1.service.delivery

import com.mottainai.v1.ConfirmDeliveryRequest
import com.mottainai.v1.ConfirmDeliveryResponse
import com.mottainai.v1.ConfirmPickupRequest
import com.mottainai.v1.ConfirmPickupResponse
import com.mottainai.v1.DeliveryServiceGrpcKt
import com.mottainai.v1.DeliveryStatus
import com.mottainai.v1.GenerateTraceReportRequest
import com.mottainai.v1.GenerateTraceReportResponse
import com.mottainai.v1.GetDeliveryStatusRequest
import com.mottainai.v1.GetDeliveryStatusResponse
import com.mottainai.v1.repository.DeliveryRepository
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcService
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant

/**
 * gRPC service implementation for DeliveryService.
 * Manages delivery confirmations and traceability reports.
 * Uses PostgreSQL-backed DeliveryRepository for persistence.
 */
@GrpcService
class DeliveryServiceImpl : DeliveryServiceGrpcKt.DeliveryServiceCoroutineImplBase() {
    @Inject
    lateinit var deliveryRepository: DeliveryRepository

    @Transactional
    override suspend fun confirmPickup(request: ConfirmPickupRequest): ConfirmPickupResponse {
        validateRequired(request.deliveryId, "delivery_id")
        validateRequired(request.driverId, "driver_id")

        val entity = getDelivery(request.deliveryId)
        requireTransition(entity.status, DeliveryStatus.DELIVERY_STATUS_PICKED_UP)

        val updated =
            entity.copy(
                driverId = request.driverId,
                status = DeliveryStatus.DELIVERY_STATUS_PICKED_UP,
                pickupPhotoUrl = request.photoUrl,
                pickupQuantity = request.quantity,
                pickupCondition = request.condition,
                pickupAt = Instant.now(),
                notes = request.notes,
                updatedAt = Instant.now(),
            )
        deliveryRepository.update(updated)

        return ConfirmPickupResponse.newBuilder().setRecord(DeliveryMapper.toProto(updated)).build()
    }

    @Transactional
    override suspend fun confirmDelivery(request: ConfirmDeliveryRequest): ConfirmDeliveryResponse {
        validateRequired(request.deliveryId, "delivery_id")
        validateRequired(request.driverId, "driver_id")

        val entity = getDelivery(request.deliveryId)
        requireTransition(entity.status, DeliveryStatus.DELIVERY_STATUS_DELIVERED)

        val updated =
            entity.copy(
                status = DeliveryStatus.DELIVERY_STATUS_DELIVERED,
                deliveryPhotoUrl = request.photoUrl,
                deliveryQuantity = request.quantity,
                deliveryCondition = request.condition,
                deliveryAt = Instant.now(),
                notes = request.notes,
                updatedAt = Instant.now(),
            )
        deliveryRepository.update(updated)

        return ConfirmDeliveryResponse.newBuilder().setRecord(DeliveryMapper.toProto(updated)).build()
    }

    override suspend fun getDeliveryStatus(request: GetDeliveryStatusRequest): GetDeliveryStatusResponse {
        validateRequired(request.deliveryId, "delivery_id")
        return GetDeliveryStatusResponse
            .newBuilder()
            .setRecord(DeliveryMapper.toProto(getDelivery(request.deliveryId)))
            .build()
    }

    override suspend fun generateTraceReport(request: GenerateTraceReportRequest): GenerateTraceReportResponse {
        validateRequired(request.deliveryId, "delivery_id")
        val entity = getDelivery(request.deliveryId)

        return GenerateTraceReportResponse
            .newBuilder()
            .setDeliveryId(entity.id.toString())
            .setRecord(DeliveryMapper.toProto(entity))
            .addAllEvents(DeliveryMapper.buildTraceEvents(entity))
            .setGeneratedAt(DeliveryMapper.toTimestamp(Instant.now()))
            .build()
    }

    @Transactional
    fun createDelivery(
        matchId: String,
        supplyId: String,
        demandId: String,
    ): DeliveryEntity {
        val entity = DeliveryEntity(matchId = matchId, supplyId = supplyId, demandId = demandId)
        deliveryRepository.insert(entity)
        return entity
    }

    private fun getDelivery(deliveryId: String): DeliveryEntity =
        deliveryRepository.findById(deliveryId)
            ?: throw StatusRuntimeException(Status.NOT_FOUND.withDescription("Delivery $deliveryId not found"))

    private fun requireTransition(
        current: DeliveryStatus,
        target: DeliveryStatus,
    ) {
        if (!DeliveryStateMachine.canTransition(current, target)) {
            throw StatusRuntimeException(
                Status.FAILED_PRECONDITION.withDescription(
                    "Cannot transition from $current to $target. " +
                        "Valid: ${DeliveryStateMachine.validNextStatuses(current)}",
                ),
            )
        }
    }

    private fun validateRequired(
        value: String,
        fieldName: String,
    ) {
        if (value.isBlank()) {
            throw StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("$fieldName is required"))
        }
    }
}
