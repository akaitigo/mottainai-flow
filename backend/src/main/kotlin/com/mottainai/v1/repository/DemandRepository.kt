package com.mottainai.v1.repository

import com.mottainai.v1.model.DemandEntity
import com.mottainai.v1.model.PageToken
import java.util.UUID

/**
 * Repository interface for demand entries.
 * Implementations may use PostgreSQL+PostGIS or in-memory storage.
 */
interface DemandRepository {
    fun insert(entity: DemandEntity): DemandEntity

    fun findById(id: UUID): DemandEntity?

    fun findFiltered(
        recipientId: String?,
        category: Int?,
        status: Int?,
        pageSize: Int,
        pageToken: PageToken?,
    ): Pair<List<DemandEntity>, Int>
}
