package com.mottainai.v1.repository

import com.mottainai.v1.model.PageToken
import com.mottainai.v1.model.SupplyEntity
import java.util.UUID

/**
 * Repository interface for supply entries.
 * Implementations may use PostgreSQL+PostGIS or in-memory storage.
 */
interface SupplyRepository {
    fun insert(entity: SupplyEntity): SupplyEntity

    fun findById(id: UUID): SupplyEntity?

    fun findFiltered(
        providerId: String?,
        category: Int?,
        status: Int?,
        pageSize: Int,
        pageToken: PageToken?,
    ): Pair<List<SupplyEntity>, Int>
}
