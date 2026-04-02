package com.mottainai.v1.repository

import io.agroal.api.AgroalDataSource
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.util.UUID

/**
 * Helper for JDBC operations to reduce nesting depth and code duplication.
 * All queries use parameterized PreparedStatements for SQL injection prevention.
 */
internal object JdbcHelper {
    fun <T> query(
        dataSource: AgroalDataSource,
        sql: String,
        paramSetter: (PreparedStatement) -> Unit,
        mapper: (ResultSet) -> T?,
    ): T? =
        executeQuery(dataSource, sql, paramSetter) { rs ->
            if (rs.next()) mapper(rs) else null
        }

    fun <T> queryList(
        dataSource: AgroalDataSource,
        sql: String,
        paramSetter: (PreparedStatement) -> Unit,
        mapper: (ResultSet) -> T,
    ): List<T> =
        executeQuery(dataSource, sql, paramSetter) { rs ->
            val list = mutableListOf<T>()
            while (rs.next()) {
                list.add(mapper(rs))
            }
            list
        } ?: emptyList()

    fun execute(
        dataSource: AgroalDataSource,
        sql: String,
        paramSetter: (PreparedStatement) -> Unit,
    ) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                paramSetter(ps)
                ps.executeUpdate()
            }
        }
    }

    fun queryCount(
        dataSource: AgroalDataSource,
        sql: String,
        paramSetter: (PreparedStatement) -> Unit,
    ): Int = query(dataSource, sql, paramSetter) { rs -> rs.getInt(1) } ?: 0

    fun timestampToInstant(ts: Timestamp?): Instant = ts?.toInstant() ?: Instant.now()

    fun setNullableTimestamp(
        stmt: PreparedStatement,
        index: Int,
        instant: Instant?,
    ) {
        if (instant != null) {
            stmt.setTimestamp(index, Timestamp.from(instant))
        } else {
            stmt.setNull(index, Types.TIMESTAMP)
        }
    }

    /**
     * Incrementally sets parameters on a PreparedStatement.
     * Returns the next available index after all parameters are set.
     */
    fun setFilterParams(
        stmt: PreparedStatement,
        params: List<Any>,
        startIndex: Int = 1,
    ): Int {
        params.forEachIndexed { i, param ->
            val idx = startIndex + i
            when (param) {
                is String -> stmt.setString(idx, param)
                is Short -> stmt.setShort(idx, param)
                is Int -> stmt.setInt(idx, param)
                is Timestamp -> stmt.setTimestamp(idx, param)
                is UUID -> stmt.setObject(idx, param)
                else -> stmt.setObject(idx, param)
            }
        }
        return startIndex + params.size
    }

    private fun <T> executeQuery(
        dataSource: AgroalDataSource,
        sql: String,
        paramSetter: (PreparedStatement) -> Unit,
        processor: (ResultSet) -> T?,
    ): T? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                paramSetter(ps)
                return ps.executeQuery().use { rs -> processor(rs) }
            }
        }
    }
}
