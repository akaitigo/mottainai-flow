package com.mottainai.v1.service

import com.mottainai.v1.Address
import com.mottainai.v1.GeoLocation
import io.grpc.Status
import io.grpc.StatusRuntimeException
import jakarta.enterprise.context.ApplicationScoped

/**
 * Geocoding service that converts addresses to geographic coordinates.
 * Current implementation uses a simple lookup table for known Japanese addresses.
 * In production, this would integrate with an external geocoding API
 * (e.g., Google Maps Geocoding API, Yahoo! Japan Geocoding API).
 */
@ApplicationScoped
class GeocodingService {
    /**
     * Geocodes an address and returns a new Address with the location populated.
     * Throws INVALID_ARGUMENT if the address cannot be resolved.
     */
    fun geocode(address: Address): Address {
        val location = lookupCoordinates(address.prefecture, address.city)
        return address
            .toBuilder()
            .setLocation(
                GeoLocation
                    .newBuilder()
                    .setLatitude(location.first)
                    .setLongitude(location.second)
                    .build(),
            ).build()
    }

    internal fun lookupCoordinates(
        prefecture: String,
        city: String,
    ): Pair<Double, Double> {
        val key = "$prefecture$city"
        return KNOWN_LOCATIONS[key] ?: getPrefectureCenter(prefecture)
    }

    private fun getPrefectureCenter(prefecture: String): Pair<Double, Double> =
        PREFECTURE_CENTERS[prefecture]
            ?: throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription(
                    "Cannot geocode address: unsupported prefecture '$prefecture'. " +
                        "Supported: ${PREFECTURE_CENTERS.keys.joinToString()}",
                ),
            )

    companion object {
        private val KNOWN_LOCATIONS =
            mapOf(
                "東京都千代田区" to Pair(35.6940, 139.7536),
                "東京都新宿区" to Pair(35.6938, 139.7034),
                "東京都渋谷区" to Pair(35.6640, 139.6982),
                "東京都港区" to Pair(35.6581, 139.7514),
                "東京都中央区" to Pair(35.6709, 139.7719),
                "東京都豊島区" to Pair(35.7263, 139.7165),
                "大阪府大阪市" to Pair(34.6937, 135.5023),
                "神奈川県横浜市" to Pair(35.4437, 139.6380),
                "愛知県名古屋市" to Pair(35.1815, 136.9066),
                "北海道札幌市" to Pair(43.0621, 141.3544),
                "福岡県福岡市" to Pair(33.5902, 130.4017),
            )

        private val PREFECTURE_CENTERS =
            mapOf(
                "東京都" to Pair(35.6812, 139.7671),
                "大阪府" to Pair(34.6937, 135.5023),
                "神奈川県" to Pair(35.4478, 139.6425),
                "愛知県" to Pair(35.1802, 136.9066),
                "北海道" to Pair(43.0621, 141.3544),
                "福岡県" to Pair(33.5902, 130.4017),
                "京都府" to Pair(35.0116, 135.7681),
                "兵庫県" to Pair(34.6913, 135.1830),
                "埼玉県" to Pair(35.8617, 139.6455),
                "千葉県" to Pair(35.6050, 140.1233),
            )
    }
}
