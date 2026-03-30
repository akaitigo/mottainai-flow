package com.mottainai.v1.service

import com.mottainai.v1.Address
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeocodingServiceTest {
    private val service = GeocodingService()

    @Test
    fun `known Tokyo address returns correct coordinates`() {
        val address =
            Address
                .newBuilder()
                .setPostalCode("100-0001")
                .setPrefecture("東京都")
                .setCity("千代田区")
                .setStreet("丸の内1-1-1")
                .build()

        val result = service.geocode(address)

        assertThat(result.location.latitude).isEqualTo(35.6940)
        assertThat(result.location.longitude).isEqualTo(139.7536)
        assertThat(result.postalCode).isEqualTo("100-0001")
        assertThat(result.prefecture).isEqualTo("東京都")
    }

    @Test
    fun `known Osaka address returns correct coordinates`() {
        val address =
            Address
                .newBuilder()
                .setPostalCode("530-0001")
                .setPrefecture("大阪府")
                .setCity("大阪市")
                .setStreet("北区梅田1-1-1")
                .build()

        val result = service.geocode(address)

        assertThat(result.location.latitude).isEqualTo(34.6937)
        assertThat(result.location.longitude).isEqualTo(135.5023)
    }

    @Test
    fun `unknown city falls back to prefecture center`() {
        val address =
            Address
                .newBuilder()
                .setPostalCode("160-0000")
                .setPrefecture("東京都")
                .setCity("三鷹市")
                .setStreet("下連雀1-1-1")
                .build()

        val result = service.geocode(address)

        // Falls back to Tokyo center
        assertThat(result.location.latitude).isEqualTo(35.6812)
        assertThat(result.location.longitude).isEqualTo(139.7671)
    }

    @Test
    fun `unknown prefecture falls back to default Tokyo`() {
        val address =
            Address
                .newBuilder()
                .setPostalCode("900-0000")
                .setPrefecture("沖縄県")
                .setCity("那覇市")
                .setStreet("おもろまち1-1-1")
                .build()

        val result = service.geocode(address)

        // Falls back to default (Tokyo Station)
        assertThat(result.location.latitude).isEqualTo(35.6812)
        assertThat(result.location.longitude).isEqualTo(139.7671)
    }

    @Test
    fun `geocode preserves original address fields`() {
        val address =
            Address
                .newBuilder()
                .setPostalCode("150-0002")
                .setPrefecture("東京都")
                .setCity("渋谷区")
                .setStreet("渋谷2-21-1")
                .build()

        val result = service.geocode(address)

        assertThat(result.postalCode).isEqualTo("150-0002")
        assertThat(result.prefecture).isEqualTo("東京都")
        assertThat(result.city).isEqualTo("渋谷区")
        assertThat(result.street).isEqualTo("渋谷2-21-1")
        assertThat(result.hasLocation()).isTrue()
    }

    @Test
    fun `lookupCoordinates returns correct pair for known location`() {
        val coords = service.lookupCoordinates("東京都", "新宿区")
        assertThat(coords.first).isEqualTo(35.6938)
        assertThat(coords.second).isEqualTo(139.7034)
    }
}
