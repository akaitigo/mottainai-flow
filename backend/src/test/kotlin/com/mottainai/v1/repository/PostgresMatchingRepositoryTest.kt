package com.mottainai.v1.repository

import com.mottainai.v1.MatchPair
import com.mottainai.v1.MatchStatus
import com.mottainai.v1.RunMatchingResponse
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@QuarkusTest
class PostgresMatchingRepositoryTest {
    @Inject
    lateinit var repository: MatchingRepository

    @Inject
    lateinit var dataSource: io.agroal.api.AgroalDataSource

    @BeforeEach
    fun cleanup() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM match_results")
            }
        }
    }

    private fun createResponse(matchId: String): RunMatchingResponse =
        RunMatchingResponse
            .newBuilder()
            .setMatchId(matchId)
            .setStatus(MatchStatus.MATCH_STATUS_COMPLETED)
            .addPairs(
                MatchPair
                    .newBuilder()
                    .setSupplyId("supply-1")
                    .setDemandId("demand-1")
                    .setDistanceScore(0.9)
                    .setTimeOverlapScore(0.8)
                    .setCategoryScore(1.0)
                    .setTotalScore(0.89)
                    .setDistanceMeters(1500.0)
                    .build(),
            ).setTotalMatched(1)
            .setTotalUnmatchedSupplies(0)
            .setTotalUnmatchedDemands(0)
            .build()

    @Nested
    inner class SaveAndFindById {
        @Test
        fun `save and retrieve match result`() {
            val response = createResponse("match-1")
            repository.save("match-1", response)

            val found = repository.findById("match-1")
            assertThat(found).isNotNull
            requireNotNull(found)
            assertThat(found.matchId).isEqualTo("match-1")
            assertThat(found.status).isEqualTo(MatchStatus.MATCH_STATUS_COMPLETED)
            assertThat(found.pairsCount).isEqualTo(1)
            assertThat(found.getPairs(0).supplyId).isEqualTo("supply-1")
            assertThat(found.getPairs(0).demandId).isEqualTo("demand-1")
            assertThat(found.totalMatched).isEqualTo(1)
        }

        @Test
        fun `findById returns null for non-existent id`() {
            val found = repository.findById("non-existent")
            assertThat(found).isNull()
        }
    }

    @Nested
    inner class Upsert {
        @Test
        fun `save overwrites existing match result`() {
            val response1 = createResponse("match-1")
            repository.save("match-1", response1)

            val response2 =
                RunMatchingResponse
                    .newBuilder()
                    .setMatchId("match-1")
                    .setStatus(MatchStatus.MATCH_STATUS_COMPLETED)
                    .addPairs(
                        MatchPair
                            .newBuilder()
                            .setSupplyId("supply-2")
                            .setDemandId("demand-2")
                            .setDistanceScore(0.7)
                            .setTimeOverlapScore(0.6)
                            .setCategoryScore(1.0)
                            .setTotalScore(0.75)
                            .setDistanceMeters(3000.0)
                            .build(),
                    ).setTotalMatched(1)
                    .build()
            repository.save("match-1", response2)

            val found = repository.findById("match-1")
            assertThat(found).isNotNull
            requireNotNull(found)
            assertThat(found.getPairs(0).supplyId).isEqualTo("supply-2")
        }
    }
}
