package com.mottainai.v1.repository

import com.mottainai.v1.model.DemandEntity
import com.mottainai.v1.model.PageToken
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
class PostgresDemandRepositoryTest {
    @Inject
    lateinit var repository: DemandRepository

    @Inject
    lateinit var dataSource: io.agroal.api.AgroalDataSource

    private val baseTime = Instant.parse("2026-04-01T09:00:00Z")

    @BeforeEach
    fun cleanup() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM deliveries")
                stmt.execute("DELETE FROM demands")
            }
        }
    }

    private fun createEntity(
        recipientId: String = "recipient-1",
        category: Int = 2,
        status: Int = 1,
    ): DemandEntity =
        DemandEntity(
            recipientId = recipientId,
            category = category,
            desiredQuantity = 5,
            unit = "kg",
            deliveryWindowStart = baseTime,
            deliveryWindowEnd = baseTime.plus(4, ChronoUnit.HOURS),
            postalCode = "100-0002",
            prefecture = "東京都",
            city = "千代田区",
            street = "神田1-1-1",
            latitude = 35.6940,
            longitude = 139.7536,
            status = status,
            description = "野菜が必要",
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
            assertThat(found.recipientId).isEqualTo("recipient-1")
            assertThat(found.category).isEqualTo(2)
            assertThat(found.desiredQuantity).isEqualTo(5)
            assertThat(found.unit).isEqualTo("kg")
            assertThat(found.latitude).isEqualTo(35.6940)
            assertThat(found.longitude).isEqualTo(139.7536)
            assertThat(found.status).isEqualTo(1)
            assertThat(found.description).isEqualTo("野菜が必要")
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
        fun `filter by recipient_id`() {
            repository.insert(createEntity(recipientId = "r1"))
            repository.insert(createEntity(recipientId = "r2"))

            val (results, count) =
                repository.findFiltered(
                    recipientId = "r1",
                    category = null,
                    status = null,
                    pageSize = 20,
                    pageToken = null,
                )

            assertThat(results).hasSize(1)
            assertThat(count).isEqualTo(1)
            assertThat(results[0].recipientId).isEqualTo("r1")
        }

        @Test
        fun `filter by category`() {
            repository.insert(createEntity(category = 2))
            repository.insert(createEntity(category = 4))

            val (results, count) =
                repository.findFiltered(
                    recipientId = null,
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
                    recipientId = null,
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
            repository.insert(createEntity(recipientId = "r1"))
            repository.insert(createEntity(recipientId = "r2"))
            repository.insert(createEntity(recipientId = "r3"))

            val (results, count) =
                repository.findFiltered(
                    recipientId = null,
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
            repeat(5) { repository.insert(createEntity(recipientId = "r-$it")) }

            val (results, count) =
                repository.findFiltered(
                    recipientId = null,
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
                    createEntity(recipientId = "r-$i").copy(
                        createdAt = baseTime.plusSeconds(i.toLong()),
                    )
                repository.insert(entity)
            }

            // Get first page
            val (page1, _) =
                repository.findFiltered(
                    recipientId = null,
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
                    recipientId = null,
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
            val e1 = createEntity(recipientId = "r1").copy(createdAt = baseTime)
            val e2 = createEntity(recipientId = "r2").copy(createdAt = baseTime.plusSeconds(10))
            val e3 = createEntity(recipientId = "r3").copy(createdAt = baseTime.plusSeconds(20))
            repository.insert(e1)
            repository.insert(e2)
            repository.insert(e3)

            val (results, _) =
                repository.findFiltered(
                    recipientId = null,
                    category = null,
                    status = null,
                    pageSize = 20,
                    pageToken = null,
                )

            // Most recent first
            assertThat(results[0].recipientId).isEqualTo("r3")
            assertThat(results[1].recipientId).isEqualTo("r2")
            assertThat(results[2].recipientId).isEqualTo("r1")
        }
    }
}
