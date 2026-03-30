package com.mottainai.v1.repository

import com.mottainai.v1.model.DemandEntity
import com.mottainai.v1.model.PageToken
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of DemandRepository.
 * Used for development and testing without a database.
 * Will be replaced by PostGIS-backed implementation when DB is available (Issue #5).
 */
@ApplicationScoped
class InMemoryDemandRepository : DemandRepository {
    private val store = ConcurrentHashMap<UUID, DemandEntity>()

    override fun insert(entity: DemandEntity): DemandEntity {
        store[entity.id] = entity
        return entity
    }

    override fun findById(id: UUID): DemandEntity? = store[id]

    override fun findFiltered(
        recipientId: String?,
        category: Int?,
        status: Int?,
        pageSize: Int,
        pageToken: PageToken?,
    ): Pair<List<DemandEntity>, Int> {
        var filtered = store.values.asSequence()

        if (!recipientId.isNullOrBlank()) {
            filtered = filtered.filter { it.recipientId == recipientId }
        }
        if (category != null && category > 0) {
            filtered = filtered.filter { it.category == category }
        }
        if (status != null && status > 0) {
            filtered = filtered.filter { it.status == status }
        }

        val sorted =
            filtered
                .sortedWith(compareByDescending<DemandEntity> { it.createdAt }.thenByDescending { it.id })
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
