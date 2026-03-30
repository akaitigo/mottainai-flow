package com.mottainai.v1.service

import com.google.protobuf.Timestamp
import com.mottainai.v1.Address
import com.mottainai.v1.Demand
import com.mottainai.v1.DemandStatus
import com.mottainai.v1.FoodCategory
import com.mottainai.v1.GeoLocation
import com.mottainai.v1.InventoryServiceGrpcKt
import com.mottainai.v1.ListDemandsRequest
import com.mottainai.v1.ListDemandsResponse
import com.mottainai.v1.ListSuppliesRequest
import com.mottainai.v1.ListSuppliesResponse
import com.mottainai.v1.RegisterDemandRequest
import com.mottainai.v1.RegisterDemandResponse
import com.mottainai.v1.RegisterSupplyRequest
import com.mottainai.v1.RegisterSupplyResponse
import com.mottainai.v1.Supply
import com.mottainai.v1.SupplyStatus
import com.mottainai.v1.TimeWindow
import com.mottainai.v1.model.DemandEntity
import com.mottainai.v1.model.PageToken
import com.mottainai.v1.model.SupplyEntity
import com.mottainai.v1.repository.DemandRepository
import com.mottainai.v1.repository.SupplyRepository
import com.mottainai.v1.validation.RequestValidator
import io.quarkus.grpc.GrpcService
import jakarta.inject.Inject
import java.time.Instant

/**
 * gRPC service implementation for InventoryService.
 * Handles registration and listing of food supply and demand.
 */
@GrpcService
class InventoryServiceImpl : InventoryServiceGrpcKt.InventoryServiceCoroutineImplBase() {
    @Inject
    lateinit var supplyRepository: SupplyRepository

    @Inject
    lateinit var demandRepository: DemandRepository

    @Inject
    lateinit var geocodingService: GeocodingService

    override suspend fun registerSupply(request: RegisterSupplyRequest): RegisterSupplyResponse {
        RequestValidator.validateRegisterSupply(request)

        val geocodedAddress = geocodingService.geocode(request.address)

        val entity =
            SupplyEntity(
                providerId = request.providerId,
                itemName = request.itemName,
                category = request.category.number,
                quantity = request.quantity,
                unit = request.unit,
                expiryDate = toInstant(request.expiryDate),
                pickupWindowStart = toInstant(request.pickupWindow.start),
                pickupWindowEnd = toInstant(request.pickupWindow.end),
                postalCode = geocodedAddress.postalCode,
                prefecture = geocodedAddress.prefecture,
                city = geocodedAddress.city,
                street = geocodedAddress.street,
                latitude = geocodedAddress.location.latitude,
                longitude = geocodedAddress.location.longitude,
                description = request.description,
            )

        val saved = supplyRepository.insert(entity)

        return RegisterSupplyResponse
            .newBuilder()
            .setSupply(toSupplyProto(saved))
            .build()
    }

    override suspend fun registerDemand(request: RegisterDemandRequest): RegisterDemandResponse {
        RequestValidator.validateRegisterDemand(request)

        val geocodedAddress = geocodingService.geocode(request.address)

        val entity =
            DemandEntity(
                recipientId = request.recipientId,
                category = request.category.number,
                desiredQuantity = request.desiredQuantity,
                unit = request.unit,
                deliveryWindowStart = toInstant(request.deliveryWindow.start),
                deliveryWindowEnd = toInstant(request.deliveryWindow.end),
                postalCode = geocodedAddress.postalCode,
                prefecture = geocodedAddress.prefecture,
                city = geocodedAddress.city,
                street = geocodedAddress.street,
                latitude = geocodedAddress.location.latitude,
                longitude = geocodedAddress.location.longitude,
                description = request.description,
            )

        val saved = demandRepository.insert(entity)

        return RegisterDemandResponse
            .newBuilder()
            .setDemand(toDemandProto(saved))
            .build()
    }

    override suspend fun listSupplies(request: ListSuppliesRequest): ListSuppliesResponse {
        val pageSize = RequestValidator.validatePageSize(request.pageSize)
        val pageToken = PageToken.decode(request.pageToken)

        val (supplies, totalCount) =
            supplyRepository.findFiltered(
                providerId = request.providerId.ifBlank { null },
                category = request.category.number.takeIf { it > 0 },
                status = request.status.number.takeIf { it > 0 },
                pageSize = pageSize,
                pageToken = pageToken,
            )

        val nextPageToken =
            if (supplies.size == pageSize && supplies.isNotEmpty()) {
                val last = supplies.last()
                PageToken(last.createdAt, last.id.toString()).encode()
            } else {
                ""
            }

        return ListSuppliesResponse
            .newBuilder()
            .addAllSupplies(supplies.map { toSupplyProto(it) })
            .setNextPageToken(nextPageToken)
            .setTotalCount(totalCount)
            .build()
    }

    override suspend fun listDemands(request: ListDemandsRequest): ListDemandsResponse {
        val pageSize = RequestValidator.validatePageSize(request.pageSize)
        val pageToken = PageToken.decode(request.pageToken)

        val (demands, totalCount) =
            demandRepository.findFiltered(
                recipientId = request.recipientId.ifBlank { null },
                category = request.category.number.takeIf { it > 0 },
                status = request.status.number.takeIf { it > 0 },
                pageSize = pageSize,
                pageToken = pageToken,
            )

        val nextPageToken =
            if (demands.size == pageSize && demands.isNotEmpty()) {
                val last = demands.last()
                PageToken(last.createdAt, last.id.toString()).encode()
            } else {
                ""
            }

        return ListDemandsResponse
            .newBuilder()
            .addAllDemands(demands.map { toDemandProto(it) })
            .setNextPageToken(nextPageToken)
            .setTotalCount(totalCount)
            .build()
    }

    private fun toSupplyProto(entity: SupplyEntity): Supply =
        Supply
            .newBuilder()
            .setId(entity.id.toString())
            .setProviderId(entity.providerId)
            .setItemName(entity.itemName)
            .setCategory(FoodCategory.forNumber(entity.category))
            .setQuantity(entity.quantity)
            .setUnit(entity.unit)
            .setExpiryDate(toTimestamp(entity.expiryDate))
            .setPickupWindow(
                TimeWindow
                    .newBuilder()
                    .setStart(toTimestamp(entity.pickupWindowStart))
                    .setEnd(toTimestamp(entity.pickupWindowEnd))
                    .build(),
            ).setAddress(
                Address
                    .newBuilder()
                    .setPostalCode(entity.postalCode)
                    .setPrefecture(entity.prefecture)
                    .setCity(entity.city)
                    .setStreet(entity.street)
                    .setLocation(
                        GeoLocation
                            .newBuilder()
                            .setLatitude(entity.latitude)
                            .setLongitude(entity.longitude)
                            .build(),
                    ).build(),
            ).setStatus(SupplyStatus.forNumber(entity.status))
            .setDescription(entity.description)
            .setCreatedAt(toTimestamp(entity.createdAt))
            .setUpdatedAt(toTimestamp(entity.updatedAt))
            .build()

    private fun toDemandProto(entity: DemandEntity): Demand =
        Demand
            .newBuilder()
            .setId(entity.id.toString())
            .setRecipientId(entity.recipientId)
            .setCategory(FoodCategory.forNumber(entity.category))
            .setDesiredQuantity(entity.desiredQuantity)
            .setUnit(entity.unit)
            .setDeliveryWindow(
                TimeWindow
                    .newBuilder()
                    .setStart(toTimestamp(entity.deliveryWindowStart))
                    .setEnd(toTimestamp(entity.deliveryWindowEnd))
                    .build(),
            ).setAddress(
                Address
                    .newBuilder()
                    .setPostalCode(entity.postalCode)
                    .setPrefecture(entity.prefecture)
                    .setCity(entity.city)
                    .setStreet(entity.street)
                    .setLocation(
                        GeoLocation
                            .newBuilder()
                            .setLatitude(entity.latitude)
                            .setLongitude(entity.longitude)
                            .build(),
                    ).build(),
            ).setStatus(DemandStatus.forNumber(entity.status))
            .setDescription(entity.description)
            .setCreatedAt(toTimestamp(entity.createdAt))
            .setUpdatedAt(toTimestamp(entity.updatedAt))
            .build()

    private fun toInstant(ts: Timestamp): Instant = Instant.ofEpochSecond(ts.seconds, ts.nanos.toLong())

    private fun toTimestamp(instant: Instant): Timestamp =
        Timestamp
            .newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()
}
