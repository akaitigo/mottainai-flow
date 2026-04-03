package com.mottainai.v1.repository

import com.mottainai.v1.DeliveryStatus
import com.mottainai.v1.service.delivery.DeliveryEntity
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * PostgreSQL implementation of DeliveryRepository.
 * Uses JDBC with parameterized queries for SQL injection prevention.
 */
@ApplicationScoped
class PostgresDeliveryRepository : DeliveryRepository {
    @Inject
    lateinit var dataSource: AgroalDataSource

    private val log: Logger = Logger.getLogger(PostgresDeliveryRepository::class.java)

    @Transactional
    override fun insert(entity: DeliveryEntity): DeliveryEntity {
        val sql =
            """
            INSERT INTO deliveries (
                id, match_id, supply_id, demand_id, driver_id, status,
                pickup_photo_url, delivery_photo_url,
                pickup_quantity, delivery_quantity,
                pickup_condition, delivery_condition,
                pickup_at, delivery_at, notes,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        JdbcHelper.execute(dataSource, sql) { stmt ->
            setInsertParams(stmt, entity)
        }
        return entity
    }

    override fun findById(id: String): DeliveryEntity? =
        JdbcHelper.query(
            dataSource,
            "SELECT * FROM deliveries WHERE id = ?",
            { it.setObject(1, UUID.fromString(id)) },
        ) { rs -> mapRow(rs) }

    @Transactional
    override fun update(
        entity: DeliveryEntity,
        expectedStatus: DeliveryStatus,
    ): Boolean {
        val sql =
            """
            UPDATE deliveries SET
                driver_id = ?, status = ?,
                pickup_photo_url = ?, delivery_photo_url = ?,
                pickup_quantity = ?, delivery_quantity = ?,
                pickup_condition = ?, delivery_condition = ?,
                pickup_at = ?, delivery_at = ?,
                notes = ?, updated_at = ?
            WHERE id = ? AND status = ?
            """.trimIndent()

        val rowsUpdated =
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    setUpdateParams(stmt, entity, expectedStatus)
                    stmt.executeUpdate()
                }
            }

        if (rowsUpdated == 0) {
            log.warnf(
                "Optimistic lock failed for delivery %s: expected status %s",
                entity.id,
                expectedStatus,
            )
        }
        return rowsUpdated > 0
    }

    @Suppress("MagicNumber")
    private fun setInsertParams(
        stmt: PreparedStatement,
        entity: DeliveryEntity,
    ) {
        stmt.setObject(1, entity.id)
        stmt.setString(2, entity.matchId)
        stmt.setObject(3, UUID.fromString(entity.supplyId))
        stmt.setObject(4, UUID.fromString(entity.demandId))
        stmt.setString(5, entity.driverId)
        stmt.setShort(6, entity.status.number.toShort())
        stmt.setString(7, entity.pickupPhotoUrl)
        stmt.setString(8, entity.deliveryPhotoUrl)
        stmt.setInt(9, entity.pickupQuantity)
        stmt.setInt(10, entity.deliveryQuantity)
        stmt.setString(11, entity.pickupCondition)
        stmt.setString(12, entity.deliveryCondition)
        JdbcHelper.setNullableTimestamp(stmt, 13, entity.pickupAt)
        JdbcHelper.setNullableTimestamp(stmt, 14, entity.deliveryAt)
        stmt.setString(15, entity.notes)
        stmt.setTimestamp(16, Timestamp.from(entity.createdAt))
        stmt.setTimestamp(17, Timestamp.from(entity.updatedAt))
    }

    @Suppress("MagicNumber")
    private fun setUpdateParams(
        stmt: PreparedStatement,
        entity: DeliveryEntity,
        expectedStatus: DeliveryStatus,
    ) {
        stmt.setString(1, entity.driverId)
        stmt.setShort(2, entity.status.number.toShort())
        stmt.setString(3, entity.pickupPhotoUrl)
        stmt.setString(4, entity.deliveryPhotoUrl)
        stmt.setInt(5, entity.pickupQuantity)
        stmt.setInt(6, entity.deliveryQuantity)
        stmt.setString(7, entity.pickupCondition)
        stmt.setString(8, entity.deliveryCondition)
        JdbcHelper.setNullableTimestamp(stmt, 9, entity.pickupAt)
        JdbcHelper.setNullableTimestamp(stmt, 10, entity.deliveryAt)
        stmt.setString(11, entity.notes)
        stmt.setTimestamp(12, Timestamp.from(entity.updatedAt))
        stmt.setObject(13, entity.id)
        stmt.setShort(14, expectedStatus.number.toShort())
    }

    private fun mapRow(rs: ResultSet): DeliveryEntity =
        DeliveryEntity(
            id = rs.getObject("id", UUID::class.java),
            matchId = rs.getString("match_id"),
            supplyId = rs.getObject("supply_id", UUID::class.java).toString(),
            demandId = rs.getObject("demand_id", UUID::class.java).toString(),
            driverId = rs.getString("driver_id").orEmpty(),
            status =
                DeliveryStatus.forNumber(rs.getShort("status").toInt())
                    ?: DeliveryStatus.DELIVERY_STATUS_PENDING,
            pickupPhotoUrl = rs.getString("pickup_photo_url").orEmpty(),
            deliveryPhotoUrl = rs.getString("delivery_photo_url").orEmpty(),
            pickupQuantity = rs.getInt("pickup_quantity"),
            deliveryQuantity = rs.getInt("delivery_quantity"),
            pickupCondition = rs.getString("pickup_condition").orEmpty(),
            deliveryCondition = rs.getString("delivery_condition").orEmpty(),
            pickupAt = rs.getTimestamp("pickup_at")?.toInstant(),
            deliveryAt = rs.getTimestamp("delivery_at")?.toInstant(),
            notes = rs.getString("notes").orEmpty(),
            createdAt = JdbcHelper.timestampToInstant(rs.getTimestamp("created_at")),
            updatedAt = JdbcHelper.timestampToInstant(rs.getTimestamp("updated_at")),
        )
}
