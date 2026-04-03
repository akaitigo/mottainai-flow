package com.mottainai.v1.model

import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.time.Instant
import java.util.Base64

/**
 * Keyset pagination token encoded as Base64(created_at|id).
 * Provides O(limit) performance regardless of page depth.
 */
data class PageToken(
    val createdAt: Instant,
    val id: String,
) {
    fun encode(): String {
        val raw = "${createdAt.epochSecond}:${createdAt.nano}|$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    companion object {
        private const val PIPE_SEPARATOR = "|"
        private const val COLON_SEPARATOR = ":"
        private const val EXPECTED_PARTS = 2

        /**
         * Decodes a page token string. Returns null for blank tokens.
         * Throws INVALID_ARGUMENT for non-blank but malformed tokens.
         */
        fun decode(token: String): PageToken? {
            if (token.isBlank()) return null
            return parseToken(token)
                ?: throw StatusRuntimeException(
                    Status.INVALID_ARGUMENT.withDescription(
                        "Invalid page_token: token is malformed or corrupted",
                    ),
                )
        }

        private fun parseToken(token: String): PageToken? =
            runCatching {
                val raw = String(Base64.getUrlDecoder().decode(token))
                val parts =
                    raw
                        .split(PIPE_SEPARATOR, limit = EXPECTED_PARTS)
                        .takeIf { it.size == EXPECTED_PARTS }
                val timeParts =
                    parts
                        ?.get(0)
                        ?.split(COLON_SEPARATOR, limit = EXPECTED_PARTS)
                        ?.takeIf { it.size == EXPECTED_PARTS }
                val seconds = timeParts?.get(0)?.toLongOrNull()
                val nanos = timeParts?.get(1)?.toIntOrNull()
                val tokenId = parts?.get(1)

                if (seconds != null && nanos != null && tokenId != null) {
                    PageToken(Instant.ofEpochSecond(seconds, nanos.toLong()), tokenId)
                } else {
                    null
                }
            }.getOrNull()
    }
}
