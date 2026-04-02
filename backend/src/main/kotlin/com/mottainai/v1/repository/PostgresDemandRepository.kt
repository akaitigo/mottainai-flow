package com.mottainai.v1.repository

import com.mottainai.v1.model.DemandEntity
import com.mottainai.v1.model.PageToken
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * PostgreSQL implementation of DemandRepository.
 * Uses JDBC with parameterized queries for SQL injection prevention.
 */
@ApplicationScoped
class PostgresDemandRepository : DemandRepository {
    @Inject
    lateinit var dataSource: AgroalDataSource

    @Transactional
    override fun insert(entity: DemandEntity): DemandEntity {
        val sql =
            """
            INSERT INTO demands (
                id, recipient_id, category, desired_quantity, unit,
                delivery_window_start, delivery_window_end,
                postal_code, prefecture, city, street,
                latitude, longitude, status, description,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        JdbcHelper.execute(dataSource, sql) { stmt ->
            setInsertParams(stmt, entity)
        }
        return entity
    }

    override fun findById(id: UUID): DemandEntity? =
        JdbcHelper.query(dataSource, "SELECT * FROM demands WHERE id = ?", { it.setObject(1, id) }) { rs ->
            mapRow(rs)
        }

    override fun findFiltered(
        recipientId: String?,
        category: Int?,
        status: Int?,
        pageSize: Int,
        pageToken: PageToken?,
    ): Pair<List<DemandEntity>, Int> {
        val filter = buildFilter(recipientId, category, status)

        val whereClause = if (filter.conditions.isNotEmpty()) "WHERE ${filter.conditions.joinToString(" AND ")}" else ""
        val totalCount =
            JdbcHelper.queryCount(dataSource, "SELECT COUNT(*) FROM demands $whereClause") { stmt ->
                JdbcHelper.setFilterParams(stmt, filter.params)
            }

        val cursorFilter = buildCursorFilter(filter, pageToken)
        val cursorWhere =
            if (cursorFilter.conditions.isNotEmpty()) "WHERE ${cursorFilter.conditions.joinToString(" AND ")}" else ""
        val dataSql = "SELECT * FROM demands $cursorWhere ORDER BY created_at DESC, id DESC LIMIT ?"

        val entities =
            JdbcHelper.queryList(dataSource, dataSql, { stmt ->
                val nextIdx = JdbcHelper.setFilterParams(stmt, cursorFilter.params)
                stmt.setInt(nextIdx, pageSize)
            }) { rs -> mapRow(rs) }

        return Pair(entities, totalCount)
    }

    private fun buildFilter(
        recipientId: String?,
        category: Int?,
        status: Int?,
    ): FilterClause {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (!recipientId.isNullOrBlank()) {
            conditions.add("recipient_id = ?")
            params.add(recipientId)
        }
        if (category != null && category > 0) {
            conditions.add("category = ?")
            params.add(category.toShort())
        }
        if (status != null && status > 0) {
            conditions.add("status = ?")
            params.add(status.toShort())
        }
        return FilterClause(conditions, params)
    }

    private fun buildCursorFilter(
        base: FilterClause,
        pageToken: PageToken?,
    ): FilterClause {
        val conditions = base.conditions.toMutableList()
        val params = base.params.toMutableList()

        if (pageToken != null) {
            conditions.add("(created_at < ? OR (created_at = ? AND id < ?))")
            val ts = Timestamp.from(pageToken.createdAt)
            params.add(ts)
            params.add(ts)
            params.add(UUID.fromString(pageToken.id))
        }
        return FilterClause(conditions, params)
    }

    @Suppress("MagicNumber")
    private fun setInsertParams(
        stmt: PreparedStatement,
        entity: DemandEntity,
    ) {
        stmt.setObject(1, entity.id)
        stmt.setString(2, entity.recipientId)
        stmt.setShort(3, entity.category.toShort())
        stmt.setInt(4, entity.desiredQuantity)
        stmt.setString(5, entity.unit)
        stmt.setTimestamp(6, Timestamp.from(entity.deliveryWindowStart))
        stmt.setTimestamp(7, Timestamp.from(entity.deliveryWindowEnd))
        stmt.setString(8, entity.postalCode)
        stmt.setString(9, entity.prefecture)
        stmt.setString(10, entity.city)
        stmt.setString(11, entity.street)
        stmt.setDouble(12, entity.latitude)
        stmt.setDouble(13, entity.longitude)
        stmt.setShort(14, entity.status.toShort())
        stmt.setString(15, entity.description)
        stmt.setTimestamp(16, Timestamp.from(entity.createdAt))
        stmt.setTimestamp(17, Timestamp.from(entity.updatedAt))
    }

    private fun mapRow(rs: ResultSet): DemandEntity =
        DemandEntity(
            id = rs.getObject("id", UUID::class.java),
            recipientId = rs.getString("recipient_id"),
            category = rs.getShort("category").toInt(),
            desiredQuantity = rs.getInt("desired_quantity"),
            unit = rs.getString("unit"),
            deliveryWindowStart = JdbcHelper.timestampToInstant(rs.getTimestamp("delivery_window_start")),
            deliveryWindowEnd = JdbcHelper.timestampToInstant(rs.getTimestamp("delivery_window_end")),
            postalCode = rs.getString("postal_code").orEmpty(),
            prefecture = rs.getString("prefecture").orEmpty(),
            city = rs.getString("city").orEmpty(),
            street = rs.getString("street").orEmpty(),
            latitude = rs.getDouble("latitude"),
            longitude = rs.getDouble("longitude"),
            status = rs.getShort("status").toInt(),
            description = rs.getString("description").orEmpty(),
            createdAt = JdbcHelper.timestampToInstant(rs.getTimestamp("created_at")),
            updatedAt = JdbcHelper.timestampToInstant(rs.getTimestamp("updated_at")),
        )
}
