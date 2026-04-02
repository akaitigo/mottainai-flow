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
        val exists =
            JdbcHelper.queryCount(
                dataSource,
                "SELECT COUNT(*) FROM match_results WHERE id = ?",
            ) { it.setString(1, matchId) }

        if (exists > 0) {
            updateExisting(matchId, response.status.number.toShort(), json, now)
        } else {
            insertNew(matchId, response.status.number.toShort(), json, now)
        }
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

    @Suppress("MagicNumber")
    private fun updateExisting(
        matchId: String,
        status: Short,
        json: String,
        now: Timestamp,
    ) {
        val sql = "UPDATE match_results SET status = ?, result_json = ?, updated_at = ? WHERE id = ?"
        JdbcHelper.execute(dataSource, sql) { stmt ->
            stmt.setShort(1, status)
            stmt.setString(2, json)
            stmt.setTimestamp(3, now)
            stmt.setString(4, matchId)
        }
    }

    @Suppress("MagicNumber")
    private fun insertNew(
        matchId: String,
        status: Short,
        json: String,
        now: Timestamp,
    ) {
        val sql = "INSERT INTO match_results (id, status, result_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?)"
        JdbcHelper.execute(dataSource, sql) { stmt ->
            stmt.setString(1, matchId)
            stmt.setShort(2, status)
            stmt.setString(3, json)
            stmt.setTimestamp(4, now)
            stmt.setTimestamp(5, now)
        }
    }
}
