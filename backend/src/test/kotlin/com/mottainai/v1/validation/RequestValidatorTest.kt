package com.mottainai.v1.validation

import com.google.protobuf.Timestamp
import com.mottainai.v1.Address
import com.mottainai.v1.FoodCategory
import com.mottainai.v1.RegisterDemandRequest
import com.mottainai.v1.RegisterSupplyRequest
import com.mottainai.v1.TimeWindow
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class RequestValidatorTest {
    private val futureTime = Instant.now().plus(7, ChronoUnit.DAYS)
    private val futureTimestamp =
        Timestamp
            .newBuilder()
            .setSeconds(futureTime.epochSecond)
            .setNanos(futureTime.nano)
            .build()

    private val windowStart = Instant.now().plus(1, ChronoUnit.DAYS)
    private val windowEnd = Instant.now().plus(2, ChronoUnit.DAYS)
    private val validTimeWindow =
        TimeWindow
            .newBuilder()
            .setStart(
                Timestamp
                    .newBuilder()
                    .setSeconds(windowStart.epochSecond)
                    .setNanos(windowStart.nano)
                    .build(),
            ).setEnd(
                Timestamp
                    .newBuilder()
                    .setSeconds(windowEnd.epochSecond)
                    .setNanos(windowEnd.nano)
                    .build(),
            ).build()

    private val validAddress =
        Address
            .newBuilder()
            .setPostalCode("100-0001")
            .setPrefecture("東京都")
            .setCity("千代田区")
            .setStreet("丸の内1-1-1")
            .build()

    @Nested
    inner class RegisterSupplyValidation {
        private fun validRequest(): RegisterSupplyRequest =
            RegisterSupplyRequest
                .newBuilder()
                .setProviderId("provider-001")
                .setItemName("りんご")
                .setCategory(FoodCategory.FOOD_CATEGORY_FRUITS)
                .setQuantity(10)
                .setUnit("kg")
                .setExpiryDate(futureTimestamp)
                .setPickupWindow(validTimeWindow)
                .setAddress(validAddress)
                .setDescription("新鮮なりんご")
                .build()

        @Test
        fun `valid request passes validation`() {
            assertThatCode { RequestValidator.validateRegisterSupply(validRequest()) }
                .doesNotThrowAnyException()
        }

        @Test
        fun `blank provider_id is rejected`() {
            val request = validRequest().toBuilder().setProviderId("").build()
            assertInvalidArgument(request, "provider_id is required")
        }

        @Test
        fun `provider_id exceeding max length is rejected`() {
            val request =
                validRequest()
                    .toBuilder()
                    .setProviderId("a".repeat(101))
                    .build()
            assertInvalidArgument(request, "provider_id must be at most 100 characters")
        }

        @Test
        fun `blank item_name is rejected`() {
            val request = validRequest().toBuilder().setItemName("").build()
            assertInvalidArgument(request, "item_name is required")
        }

        @Test
        fun `item_name exceeding max length is rejected`() {
            val request =
                validRequest()
                    .toBuilder()
                    .setItemName("a".repeat(201))
                    .build()
            assertInvalidArgument(request, "item_name must be at most 200 characters")
        }

        @Test
        fun `unspecified category is rejected`() {
            val request =
                validRequest()
                    .toBuilder()
                    .setCategory(FoodCategory.FOOD_CATEGORY_UNSPECIFIED)
                    .build()
            assertInvalidArgument(request, "category is required")
        }

        @Test
        fun `zero quantity is rejected`() {
            val request = validRequest().toBuilder().setQuantity(0).build()
            assertInvalidArgument(request, "quantity must be greater than 0")
        }

        @Test
        fun `negative quantity is rejected`() {
            val request = validRequest().toBuilder().setQuantity(-1).build()
            assertInvalidArgument(request, "quantity must be greater than 0")
        }

        @Test
        fun `blank unit is rejected`() {
            val request = validRequest().toBuilder().setUnit("").build()
            assertInvalidArgument(request, "unit is required")
        }

        @Test
        fun `unit exceeding max length is rejected`() {
            val request =
                validRequest()
                    .toBuilder()
                    .setUnit("a".repeat(21))
                    .build()
            assertInvalidArgument(request, "unit must be at most 20 characters")
        }

        @Test
        fun `past expiry_date is rejected`() {
            val past = Instant.now().minus(1, ChronoUnit.DAYS)
            val pastTimestamp =
                Timestamp
                    .newBuilder()
                    .setSeconds(past.epochSecond)
                    .setNanos(past.nano)
                    .build()
            val request = validRequest().toBuilder().setExpiryDate(pastTimestamp).build()
            assertInvalidArgument(request, "expiry_date must be in the future")
        }

        @Test
        fun `invalid time window is rejected`() {
            val invertedWindow =
                TimeWindow
                    .newBuilder()
                    .setStart(validTimeWindow.end)
                    .setEnd(validTimeWindow.start)
                    .build()
            val request = validRequest().toBuilder().setPickupWindow(invertedWindow).build()
            assertInvalidArgument(request, "pickup_window.start must be before pickup_window.end")
        }

        @Test
        fun `missing address postal_code is rejected`() {
            val noPostal = validAddress.toBuilder().setPostalCode("").build()
            val request = validRequest().toBuilder().setAddress(noPostal).build()
            assertInvalidArgument(request, "address.postal_code is required")
        }

        @Test
        fun `description exceeding max length is rejected`() {
            val request =
                validRequest()
                    .toBuilder()
                    .setDescription("a".repeat(1001))
                    .build()
            assertInvalidArgument(request, "description must be at most 1000 characters")
        }

        @Test
        fun `multiple errors are reported together`() {
            val request =
                RegisterSupplyRequest
                    .newBuilder()
                    .setProviderId("")
                    .setItemName("")
                    .setQuantity(0)
                    .setUnit("")
                    .build()
            assertThatThrownBy { RequestValidator.validateRegisterSupply(request) }
                .isInstanceOf(StatusRuntimeException::class.java)
                .extracting { (it as StatusRuntimeException).status.code }
                .isEqualTo(Status.INVALID_ARGUMENT.code)
        }

        private fun assertInvalidArgument(
            request: RegisterSupplyRequest,
            expectedMessage: String,
        ) {
            assertThatThrownBy { RequestValidator.validateRegisterSupply(request) }
                .isInstanceOf(StatusRuntimeException::class.java)
                .satisfies({ ex ->
                    val sre = ex as StatusRuntimeException
                    assertThat(sre.status.code).isEqualTo(Status.INVALID_ARGUMENT.code)
                    assertThat(sre.status.description).contains(expectedMessage)
                })
        }
    }

    @Nested
    inner class RegisterDemandValidation {
        private fun validRequest(): RegisterDemandRequest =
            RegisterDemandRequest
                .newBuilder()
                .setRecipientId("recipient-001")
                .setCategory(FoodCategory.FOOD_CATEGORY_VEGETABLES)
                .setDesiredQuantity(5)
                .setUnit("kg")
                .setDeliveryWindow(validTimeWindow)
                .setAddress(validAddress)
                .build()

        @Test
        fun `valid request passes validation`() {
            assertThatCode { RequestValidator.validateRegisterDemand(validRequest()) }
                .doesNotThrowAnyException()
        }

        @Test
        fun `blank recipient_id is rejected`() {
            val request = validRequest().toBuilder().setRecipientId("").build()
            assertInvalidArgument(request, "recipient_id is required")
        }

        @Test
        fun `unspecified category is rejected`() {
            val request =
                validRequest()
                    .toBuilder()
                    .setCategory(FoodCategory.FOOD_CATEGORY_UNSPECIFIED)
                    .build()
            assertInvalidArgument(request, "category is required")
        }

        @Test
        fun `zero desired_quantity is rejected`() {
            val request = validRequest().toBuilder().setDesiredQuantity(0).build()
            assertInvalidArgument(request, "desired_quantity must be greater than 0")
        }

        @Test
        fun `blank unit is rejected`() {
            val request = validRequest().toBuilder().setUnit("").build()
            assertInvalidArgument(request, "unit is required")
        }

        private fun assertInvalidArgument(
            request: RegisterDemandRequest,
            expectedMessage: String,
        ) {
            assertThatThrownBy { RequestValidator.validateRegisterDemand(request) }
                .isInstanceOf(StatusRuntimeException::class.java)
                .satisfies({ ex ->
                    val sre = ex as StatusRuntimeException
                    assertThat(sre.status.code).isEqualTo(Status.INVALID_ARGUMENT.code)
                    assertThat(sre.status.description).contains(expectedMessage)
                })
        }
    }

    @Nested
    inner class PageSizeValidation {
        @Test
        fun `zero page size defaults to 20`() {
            assertThat(RequestValidator.validatePageSize(0)).isEqualTo(20)
        }

        @Test
        fun `negative page size defaults to 20`() {
            assertThat(RequestValidator.validatePageSize(-5)).isEqualTo(20)
        }

        @Test
        fun `page size above 100 is capped to 100`() {
            assertThat(RequestValidator.validatePageSize(200)).isEqualTo(100)
        }

        @Test
        fun `valid page size is returned as-is`() {
            assertThat(RequestValidator.validatePageSize(50)).isEqualTo(50)
        }
    }
}
