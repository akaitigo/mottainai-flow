package com.mottainai.v1.repository

import com.mottainai.v1.model.PageToken
import com.mottainai.v1.model.SupplyEntity
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * PostgreSQL implementation of SupplyRepository.
 * Uses JDBC with parameterized queries for SQL injection prevention.
 */
@ApplicationScoped
class PostgresSupplyRepository : SupplyRepository {
    @Inject
    lateinit var dataSource: AgroalDataSource

    @Transactional
    override fun insert(entity: SupplyEntity): SupplyEntity {
        val sql =
            """
            INSERT INTO supplies (
                id, provider_id, item_name, category, quantity, unit,
                expiry_date, pickup_window_start, pickup_window_end,
                postal_code, prefecture, city, street,
                latitude, longitude, status, description,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        JdbcHelper.execute(dataSource, sql) { stmt ->
            setInsertParams(stmt, entity)
        }
        return entity
    }

    override fun findById(id: UUID): SupplyEntity? =
        JdbcHelper.query(dataSource, "SELECT * FROM supplies WHERE id = ?", { it.setObject(1, id) }) { rs ->
            mapRow(rs)
        }

    override fun findFiltered(
        providerId: String?,
        category: Int?,
        status: Int?,
        pageSize: Int,
        pageToken: PageToken?,
    ): Pair<List<SupplyEntity>, Int> {
        val filter = buildFilter(providerId, category, status)

        val whereClause = if (filter.conditions.isNotEmpty()) "WHERE ${filter.conditions.joinToString(" AND ")}" else ""
        val totalCount =
            JdbcHelper.queryCount(dataSource, "SELECT COUNT(*) FROM supplies $whereClause") { stmt ->
                JdbcHelper.setFilterParams(stmt, filter.params)
            }

        val cursorFilter = buildCursorFilter(filter, pageToken)
        val cursorWhere =
            if (cursorFilter.conditions.isNotEmpty()) "WHERE ${cursorFilter.conditions.joinToString(" AND ")}" else ""
        val dataSql = "SELECT * FROM supplies $cursorWhere ORDER BY created_at DESC, id DESC LIMIT ?"

        val entities =
            JdbcHelper.queryList(dataSource, dataSql, { stmt ->
                val nextIdx = JdbcHelper.setFilterParams(stmt, cursorFilter.params)
                stmt.setInt(nextIdx, pageSize)
            }) { rs -> mapRow(rs) }

        return Pair(entities, totalCount)
    }

    private fun buildFilter(
        providerId: String?,
        category: Int?,
        status: Int?,
    ): FilterClause {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (!providerId.isNullOrBlank()) {
            conditions.add("provider_id = ?")
            params.add(providerId)
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
        entity: SupplyEntity,
    ) {
        stmt.setObject(1, entity.id)
        stmt.setString(2, entity.providerId)
        stmt.setString(3, entity.itemName)
        stmt.setShort(4, entity.category.toShort())
        stmt.setInt(5, entity.quantity)
        stmt.setString(6, entity.unit)
        stmt.setTimestamp(7, Timestamp.from(entity.expiryDate))
        stmt.setTimestamp(8, Timestamp.from(entity.pickupWindowStart))
        stmt.setTimestamp(9, Timestamp.from(entity.pickupWindowEnd))
        stmt.setString(10, entity.postalCode)
        stmt.setString(11, entity.prefecture)
        stmt.setString(12, entity.city)
        stmt.setString(13, entity.street)
        stmt.setDouble(14, entity.latitude)
        stmt.setDouble(15, entity.longitude)
        stmt.setShort(16, entity.status.toShort())
        stmt.setString(17, entity.description)
        stmt.setTimestamp(18, Timestamp.from(entity.createdAt))
        stmt.setTimestamp(19, Timestamp.from(entity.updatedAt))
    }

    private fun mapRow(rs: ResultSet): SupplyEntity =
        SupplyEntity(
            id = rs.getObject("id", UUID::class.java),
            providerId = rs.getString("provider_id"),
            itemName = rs.getString("item_name"),
            category = rs.getShort("category").toInt(),
            quantity = rs.getInt("quantity"),
            unit = rs.getString("unit"),
            expiryDate = JdbcHelper.timestampToInstant(rs.getTimestamp("expiry_date")),
            pickupWindowStart = JdbcHelper.timestampToInstant(rs.getTimestamp("pickup_window_start")),
            pickupWindowEnd = JdbcHelper.timestampToInstant(rs.getTimestamp("pickup_window_end")),
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

/**
 * Internal data class for building SQL WHERE clauses with parameters.
 */
internal data class FilterClause(
    val conditions: List<String>,
    val params: List<Any>,
)
