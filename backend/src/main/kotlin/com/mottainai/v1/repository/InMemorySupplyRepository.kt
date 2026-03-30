package com.mottainai.v1.repository

import com.mottainai.v1.model.PageToken
import com.mottainai.v1.model.SupplyEntity
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of SupplyRepository.
 * Used for development and testing without a database.
 * Will be replaced by PostGIS-backed implementation when DB is available (Issue #5).
 */
@ApplicationScoped
class InMemorySupplyRepository : SupplyRepository {
    private val store = ConcurrentHashMap<UUID, SupplyEntity>()

    override fun insert(entity: SupplyEntity): SupplyEntity {
        store[entity.id] = entity
        return entity
    }

    override fun findById(id: UUID): SupplyEntity? = store[id]

    override fun findFiltered(
        providerId: String?,
        category: Int?,
        status: Int?,
        pageSize: Int,
        pageToken: PageToken?,
    ): Pair<List<SupplyEntity>, Int> {
        var filtered = store.values.asSequence()

        if (!providerId.isNullOrBlank()) {
            filtered = filtered.filter { it.providerId == providerId }
        }
        if (category != null && category > 0) {
            filtered = filtered.filter { it.category == category }
        }
        if (status != null && status > 0) {
            filtered = filtered.filter { it.status == status }
        }

        val sorted =
            filtered
                .sortedWith(compareByDescending<SupplyEntity> { it.createdAt }.thenByDescending { it.id })
                .toList()

        val totalCount = sorted.size

        val startIndex =
            if (pageToken != null) {
                sorted
                    .indexOfFirst {
                        it.createdAt < pageToken.createdAt ||
                            (it.createdAt == pageToken.createdAt && it.id.toString() < pageToken.id)
                    }.coerceAtLeast(0)
            } else {
                0
            }

        val page = sorted.drop(startIndex).take(pageSize)
        return Pair(page, totalCount)
    }

    fun clear() {
        store.clear()
    }
}
