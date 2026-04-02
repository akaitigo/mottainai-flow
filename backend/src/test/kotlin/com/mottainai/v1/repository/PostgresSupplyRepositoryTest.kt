package com.mottainai.v1.repository

import com.mottainai.v1.model.PageToken
import com.mottainai.v1.model.SupplyEntity
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@QuarkusTest
class PostgresSupplyRepositoryTest {
    @Inject
    lateinit var repository: SupplyRepository

    @Inject
    lateinit var dataSource: io.agroal.api.AgroalDataSource

    private val baseTime = Instant.parse("2026-04-01T09:00:00Z")

    @BeforeEach
    fun cleanup() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM deliveries")
                stmt.execute("DELETE FROM supplies")
            }
        }
    }

    private fun createEntity(
        providerId: String = "provider-1",
        category: Int = 2,
        status: Int = 1,
    ): SupplyEntity =
        SupplyEntity(
            providerId = providerId,
            itemName = "りんご",
            category = category,
            quantity = 10,
            unit = "kg",
            expiryDate = baseTime.plus(7, ChronoUnit.DAYS),
            pickupWindowStart = baseTime,
            pickupWindowEnd = baseTime.plus(4, ChronoUnit.HOURS),
            postalCode = "100-0001",
            prefecture = "東京都",
            city = "千代田区",
            street = "丸の内1-1-1",
            latitude = 35.6812,
            longitude = 139.7671,
            status = status,
            description = "新鮮なりんご",
        )

    @Nested
    inner class InsertAndFindById {
        @Test
        fun `insert and retrieve by id`() {
            val entity = createEntity()
            val saved = repository.insert(entity)

            val found = repository.findById(saved.id)
            assertThat(found).isNotNull
            requireNotNull(found)
            assertThat(found.id).isEqualTo(entity.id)
            assertThat(found.providerId).isEqualTo("provider-1")
            assertThat(found.itemName).isEqualTo("りんご")
            assertThat(found.category).isEqualTo(2)
            assertThat(found.quantity).isEqualTo(10)
            assertThat(found.unit).isEqualTo("kg")
            assertThat(found.latitude).isEqualTo(35.6812)
            assertThat(found.longitude).isEqualTo(139.7671)
            assertThat(found.status).isEqualTo(1)
            assertThat(found.description).isEqualTo("新鮮なりんご")
        }

        @Test
        fun `findById returns null for non-existent id`() {
            val found = repository.findById(UUID.randomUUID())
            assertThat(found).isNull()
        }
    }

    @Nested
    inner class FindFiltered {
        @Test
        fun `filter by provider_id`() {
            repository.insert(createEntity(providerId = "p1"))
            repository.insert(createEntity(providerId = "p2"))

            val (results, count) =
                repository.findFiltered(
                    providerId = "p1",
                    category = null,
                    status = null,
                    pageSize = 20,
                    pageToken = null,
                )

            assertThat(results).hasSize(1)
            assertThat(count).isEqualTo(1)
            assertThat(results[0].providerId).isEqualTo("p1")
        }

        @Test
        fun `filter by category`() {
            repository.insert(createEntity(category = 2))
            repository.insert(createEntity(category = 3))

            val (results, count) =
                repository.findFiltered(
                    providerId = null,
                    category = 2,
                    status = null,
                    pageSize = 20,
                    pageToken = null,
                )

            assertThat(results).hasSize(1)
            assertThat(count).isEqualTo(1)
            assertThat(results[0].category).isEqualTo(2)
        }

        @Test
        fun `filter by status`() {
            repository.insert(createEntity(status = 1))
            repository.insert(createEntity(status = 2))

            val (results, count) =
                repository.findFiltered(
                    providerId = null,
                    category = null,
                    status = 1,
                    pageSize = 20,
                    pageToken = null,
                )

            assertThat(results).hasSize(1)
            assertThat(count).isEqualTo(1)
            assertThat(results[0].status).isEqualTo(1)
        }

        @Test
        fun `returns all when no filters`() {
            repository.insert(createEntity(providerId = "p1"))
            repository.insert(createEntity(providerId = "p2"))
            repository.insert(createEntity(providerId = "p3"))

            val (results, count) =
                repository.findFiltered(
                    providerId = null,
                    category = null,
                    status = null,
                    pageSize = 20,
                    pageToken = null,
                )

            assertThat(results).hasSize(3)
            assertThat(count).isEqualTo(3)
        }

        @Test
        fun `respects page size`() {
            repeat(5) { repository.insert(createEntity(providerId = "p-$it")) }

            val (results, count) =
                repository.findFiltered(
                    providerId = null,
                    category = null,
                    status = null,
                    pageSize = 2,
                    pageToken = null,
                )

            assertThat(results).hasSize(2)
            assertThat(count).isEqualTo(5)
        }

        @Test
        fun `keyset pagination works`() {
            (0 until 5).forEach { i ->
                val entity =
                    createEntity(providerId = "p-$i").copy(
                        createdAt = baseTime.plusSeconds(i.toLong()),
                    )
                repository.insert(entity)
            }

            // Get first page
            val (page1, _) =
                repository.findFiltered(
                    providerId = null,
                    category = null,
                    status = null,
                    pageSize = 2,
                    pageToken = null,
                )
            assertThat(page1).hasSize(2)

            // Get second page using last item from first page
            val lastItem = page1.last()
            val token = PageToken(lastItem.createdAt, lastItem.id.toString())

            val (page2, _) =
                repository.findFiltered(
                    providerId = null,
                    category = null,
                    status = null,
                    pageSize = 2,
                    pageToken = token,
                )
            assertThat(page2).hasSize(2)

            // Ensure no overlap between pages
            val page1Ids = page1.map { it.id }.toSet()
            val page2Ids = page2.map { it.id }.toSet()
            assertThat(page1Ids.intersect(page2Ids)).isEmpty()
        }

        @Test
        fun `results are ordered by created_at descending`() {
            val e1 = createEntity(providerId = "p1").copy(createdAt = baseTime)
            val e2 = createEntity(providerId = "p2").copy(createdAt = baseTime.plusSeconds(10))
            val e3 = createEntity(providerId = "p3").copy(createdAt = baseTime.plusSeconds(20))
            repository.insert(e1)
            repository.insert(e2)
            repository.insert(e3)

            val (results, _) =
                repository.findFiltered(
                    providerId = null,
                    category = null,
                    status = null,
                    pageSize = 20,
                    pageToken = null,
                )

            // Most recent first
            assertThat(results[0].providerId).isEqualTo("p3")
            assertThat(results[1].providerId).isEqualTo("p2")
            assertThat(results[2].providerId).isEqualTo("p1")
        }
    }
}
