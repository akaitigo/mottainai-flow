package com.mottainai.v1.validation

import com.google.protobuf.Timestamp
import com.mottainai.v1.Address
import com.mottainai.v1.FoodCategory
import com.mottainai.v1.TimeWindow
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.time.Instant

/**
 * Reusable field-level validators for gRPC request messages.
 */
internal object FieldValidator {
    private const val MAX_POSTAL_CODE_LENGTH = 10
    private const val MAX_PREFECTURE_LENGTH = 10
    private const val MAX_CITY_LENGTH = 50
    private const val MAX_STREET_LENGTH = 200

    fun validateRequiredString(
        value: String,
        fieldName: String,
        maxLength: Int,
        errors: MutableList<String>,
    ) {
        if (value.isBlank()) {
            errors.add("$fieldName is required")
        } else if (value.length > maxLength) {
            errors.add("$fieldName must be at most $maxLength characters")
        }
    }

    fun validateOptionalString(
        value: String,
        fieldName: String,
        maxLength: Int,
        errors: MutableList<String>,
    ) {
        if (value.length > maxLength) {
            errors.add("$fieldName must be at most $maxLength characters")
        }
    }

    fun validateCategory(
        category: FoodCategory,
        errors: MutableList<String>,
    ) {
        if (category == FoodCategory.FOOD_CATEGORY_UNSPECIFIED) {
            errors.add("category is required")
        }
    }

    fun validatePositiveInt(
        value: Int,
        fieldName: String,
        errors: MutableList<String>,
    ) {
        if (value <= 0) {
            errors.add("$fieldName must be greater than 0")
        }
    }

    fun validateTimeWindow(
        window: TimeWindow,
        fieldName: String,
        errors: MutableList<String>,
    ) {
        if (!window.hasStart()) {
            errors.add("$fieldName.start is required")
            return
        }
        if (!window.hasEnd()) {
            errors.add("$fieldName.end is required")
            return
        }
        val start = toInstant(window.start)
        val end = toInstant(window.end)
        if (!start.isBefore(end)) {
            errors.add("$fieldName.start must be before $fieldName.end")
        }
    }

    fun validateAddress(
        hasAddress: Boolean,
        address: Address,
        errors: MutableList<String>,
    ) {
        if (!hasAddress) {
            errors.add("address is required")
            return
        }
        validateRequiredString(address.postalCode, "address.postal_code", MAX_POSTAL_CODE_LENGTH, errors)
        validateRequiredString(address.prefecture, "address.prefecture", MAX_PREFECTURE_LENGTH, errors)
        validateRequiredString(address.city, "address.city", MAX_CITY_LENGTH, errors)
        validateOptionalString(address.street, "address.street", MAX_STREET_LENGTH, errors)
    }

    fun throwIfErrors(errors: List<String>) {
        if (errors.isNotEmpty()) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription(errors.joinToString("; ")),
            )
        }
    }

    fun toInstant(ts: Timestamp): Instant = Instant.ofEpochSecond(ts.seconds, ts.nanos.toLong())
}
