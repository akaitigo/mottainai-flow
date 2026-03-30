package com.mottainai.v1.validation

import com.mottainai.v1.RegisterDemandRequest
import com.mottainai.v1.RegisterSupplyRequest
import java.time.Instant

/**
 * Validates gRPC request messages for the InventoryService.
 * All validation errors are thrown as INVALID_ARGUMENT StatusRuntimeException.
 */
object RequestValidator {
    private const val MAX_ITEM_NAME_LENGTH = 200
    private const val MAX_UNIT_LENGTH = 20
    private const val MAX_DESCRIPTION_LENGTH = 1000
    private const val MAX_ID_LENGTH = 100
    private const val DEFAULT_PAGE_SIZE = 20
    private const val MAX_PAGE_SIZE = 100

    fun validateRegisterSupply(request: RegisterSupplyRequest) {
        val errors = mutableListOf<String>()

        FieldValidator.validateRequiredString(request.providerId, "provider_id", MAX_ID_LENGTH, errors)
        FieldValidator.validateRequiredString(request.itemName, "item_name", MAX_ITEM_NAME_LENGTH, errors)
        FieldValidator.validateCategory(request.category, errors)
        FieldValidator.validatePositiveInt(request.quantity, "quantity", errors)
        FieldValidator.validateRequiredString(request.unit, "unit", MAX_UNIT_LENGTH, errors)
        validateExpiryDate(request, errors)
        validatePickupWindow(request, errors)
        FieldValidator.validateAddress(request.hasAddress(), request.address, errors)
        FieldValidator.validateOptionalString(request.description, "description", MAX_DESCRIPTION_LENGTH, errors)

        FieldValidator.throwIfErrors(errors)
    }

    fun validateRegisterDemand(request: RegisterDemandRequest) {
        val errors = mutableListOf<String>()

        FieldValidator.validateRequiredString(request.recipientId, "recipient_id", MAX_ID_LENGTH, errors)
        FieldValidator.validateCategory(request.category, errors)
        FieldValidator.validatePositiveInt(request.desiredQuantity, "desired_quantity", errors)
        FieldValidator.validateRequiredString(request.unit, "unit", MAX_UNIT_LENGTH, errors)
        validateDeliveryWindow(request, errors)
        FieldValidator.validateAddress(request.hasAddress(), request.address, errors)
        FieldValidator.validateOptionalString(request.description, "description", MAX_DESCRIPTION_LENGTH, errors)

        FieldValidator.throwIfErrors(errors)
    }

    fun validatePageSize(pageSize: Int): Int =
        when {
            pageSize <= 0 -> DEFAULT_PAGE_SIZE
            pageSize > MAX_PAGE_SIZE -> MAX_PAGE_SIZE
            else -> pageSize
        }

    private fun validateExpiryDate(
        request: RegisterSupplyRequest,
        errors: MutableList<String>,
    ) {
        if (!request.hasExpiryDate()) {
            errors.add("expiry_date is required")
        } else {
            val expiry = FieldValidator.toInstant(request.expiryDate)
            if (expiry.isBefore(Instant.now())) {
                errors.add("expiry_date must be in the future")
            }
        }
    }

    private fun validatePickupWindow(
        request: RegisterSupplyRequest,
        errors: MutableList<String>,
    ) {
        if (!request.hasPickupWindow()) {
            errors.add("pickup_window is required")
        } else {
            FieldValidator.validateTimeWindow(request.pickupWindow, "pickup_window", errors)
        }
    }

    private fun validateDeliveryWindow(
        request: RegisterDemandRequest,
        errors: MutableList<String>,
    ) {
        if (!request.hasDeliveryWindow()) {
            errors.add("delivery_window is required")
        } else {
            FieldValidator.validateTimeWindow(request.deliveryWindow, "delivery_window", errors)
        }
    }
}
