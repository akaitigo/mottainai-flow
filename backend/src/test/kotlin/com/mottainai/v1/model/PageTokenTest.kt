package com.mottainai.v1.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class PageTokenTest {
    @Test
    fun `encode and decode round-trips correctly`() {
        val now = Instant.parse("2026-03-30T10:00:00Z")
        val id = "550e8400-e29b-41d4-a716-446655440000"
        val token = PageToken(now, id)

        val encoded = token.encode()
        val decoded = PageToken.decode(encoded)

        assertThat(decoded).isNotNull
        assertThat(decoded?.createdAt).isEqualTo(now)
        assertThat(decoded?.id).isEqualTo(id)
    }

    @Test
    fun `decode returns null for blank string`() {
        assertThat(PageToken.decode("")).isNull()
        assertThat(PageToken.decode("  ")).isNull()
    }

    @Test
    fun `decode returns null for invalid base64`() {
        assertThat(PageToken.decode("not-valid-token!!!")).isNull()
    }

    @Test
    fun `decode returns null for malformed content`() {
        val encoded =
            java.util.Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString("malformed-content".toByteArray())
        assertThat(PageToken.decode(encoded)).isNull()
    }

    @Test
    fun `encode produces URL-safe base64`() {
        val token = PageToken(Instant.now(), "test-id")
        val encoded = token.encode()

        assertThat(encoded).doesNotContain("+")
        assertThat(encoded).doesNotContain("/")
        assertThat(encoded).doesNotContain("=")
    }

    @Test
    fun `different tokens produce different encodings`() {
        val token1 = PageToken(Instant.parse("2026-01-01T00:00:00Z"), "id-1")
        val token2 = PageToken(Instant.parse("2026-01-01T00:00:00Z"), "id-2")
        val token3 = PageToken(Instant.parse("2026-01-02T00:00:00Z"), "id-1")

        assertThat(token1.encode()).isNotEqualTo(token2.encode())
        assertThat(token1.encode()).isNotEqualTo(token3.encode())
    }

    @Test
    fun `preserves nanosecond precision`() {
        val instant = Instant.ofEpochSecond(1711800000L, 123456789L)
        val token = PageToken(instant, "nano-id")

        val decoded = PageToken.decode(token.encode())

        assertThat(decoded).isNotNull
        assertThat(decoded?.createdAt?.epochSecond).isEqualTo(1711800000L)
        assertThat(decoded?.createdAt?.nano).isEqualTo(123456789)
    }
}
