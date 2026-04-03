package com.mottainai.v1.repository

import com.google.protobuf.util.JsonFormat
import com.mottainai.v1.RunMatchingResponse
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.sql.Timestamp
import java.time.Instant

/**
 * PostgreSQL implementation of MatchingRepository.
 * Serializes RunMatchingResponse as JSON for storage.
 * Uses JDBC with parameterized queries for SQL injection prevention.
 */
@ApplicationScoped
class PostgresMatchingRepository : MatchingRepository {
    @Inject
    lateinit var dataSource: AgroalDataSource

    @Transactional
    override fun save(
        matchId: String,
        response: RunMatchingResponse,
    ) {
        val json = JsonFormat.printer().print(response)
        val now = Timestamp.from(Instant.now())
        upsert(matchId, response.status.number.toShort(), json, now)
    }

    override fun findById(matchId: String): RunMatchingResponse? =
        JdbcHelper.query(
            dataSource,
            "SELECT result_json FROM match_results WHERE id = ?",
            { it.setString(1, matchId) },
        ) { rs ->
            val json = rs.getString("result_json")
            val builder = RunMatchingResponse.newBuilder()
            JsonFormat.parser().merge(json, builder)
            builder.build()
        }

    /**
     * Atomic upsert using standard SQL MERGE (PostgreSQL 15+, H2 2.x).
     * Avoids the SELECT COUNT + INSERT/UPDATE race condition.
     */
    @Suppress("MagicNumber")
    private fun upsert(
        matchId: String,
        status: Short,
        json: String,
        now: Timestamp,
    ) {
        val sql =
            """
            MERGE INTO match_results AS target
            USING (VALUES (?, ?, ?, ?, ?)) AS source (id, status, result_json, created_at, updated_at)
            ON target.id = source.id
            WHEN MATCHED THEN UPDATE SET
                status = source.status,
                result_json = source.result_json,
                updated_at = source.updated_at
            WHEN NOT MATCHED THEN INSERT (id, status, result_json, created_at, updated_at)
                VALUES (source.id, source.status, source.result_json, source.created_at, source.updated_at)
            """.trimIndent()
        JdbcHelper.execute(dataSource, sql) { stmt ->
            stmt.setString(1, matchId)
            stmt.setShort(2, status)
            stmt.setString(3, json)
            stmt.setTimestamp(4, now)
            stmt.setTimestamp(5, now)
        }
    }
}
