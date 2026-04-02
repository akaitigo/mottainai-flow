package com.mottainai.v1.repository

import com.mottainai.v1.RunMatchingResponse

/**
 * Repository interface for matching results.
 * Stores match execution results for later retrieval.
 */
interface MatchingRepository {
    fun save(
        matchId: String,
        response: RunMatchingResponse,
    )

    fun findById(matchId: String): RunMatchingResponse?
}
