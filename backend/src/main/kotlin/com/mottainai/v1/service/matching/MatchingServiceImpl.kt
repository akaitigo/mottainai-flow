package com.mottainai.v1.service.matching

import com.mottainai.v1.GetMatchResultRequest
import com.mottainai.v1.GetMatchResultResponse
import com.mottainai.v1.MatchPair
import com.mottainai.v1.MatchStatus
import com.mottainai.v1.MatchingServiceGrpcKt
import com.mottainai.v1.RunMatchingRequest
import com.mottainai.v1.RunMatchingResponse
import com.mottainai.v1.model.DemandEntity
import com.mottainai.v1.model.PageToken
import com.mottainai.v1.model.SupplyEntity
import com.mottainai.v1.repository.DemandRepository
import com.mottainai.v1.repository.MatchingRepository
import com.mottainai.v1.repository.SupplyRepository
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcService
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger

/**
 * gRPC service implementation for MatchingService.
 * Matches available supplies with active demands based on distance,
 * time window overlap, and food category.
 * Uses PostgreSQL-backed MatchingRepository for persistence.
 */
@GrpcService
class MatchingServiceImpl : MatchingServiceGrpcKt.MatchingServiceCoroutineImplBase() {
    @Inject
    lateinit var supplyRepository: SupplyRepository

    @Inject
    lateinit var demandRepository: DemandRepository

    @Inject
    lateinit var matchingRepository: MatchingRepository

    private val log: Logger = Logger.getLogger(MatchingServiceImpl::class.java)

    @Transactional
    override suspend fun runMatching(request: RunMatchingRequest): RunMatchingResponse {
        if (request.matchId.isBlank()) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("match_id is required"),
            )
        }

        val maxDistance =
            if (request.maxDistanceMeters > 0) {
                request.maxDistanceMeters
            } else {
                DEFAULT_MAX_DISTANCE
            }

        val minScore =
            if (request.minScoreThreshold > 0) {
                request.minScoreThreshold
            } else {
                DEFAULT_MIN_SCORE
            }

        val supplies = fetchAllSupplies()
        val demands = fetchAllDemands()

        log.infof("Matching %d supplies with %d demands", supplies.size, demands.size)

        val pairs = findMatches(supplies, demands, maxDistance, minScore)

        val matchedSupplyIds = pairs.map { it.supplyId }.toSet()
        val matchedDemandIds = pairs.map { it.demandId }.toSet()

        val response =
            RunMatchingResponse
                .newBuilder()
                .setMatchId(request.matchId)
                .setStatus(MatchStatus.MATCH_STATUS_COMPLETED)
                .addAllPairs(pairs)
                .setTotalMatched(pairs.size)
                .setTotalUnmatchedSupplies(supplies.size - matchedSupplyIds.size)
                .setTotalUnmatchedDemands(demands.size - matchedDemandIds.size)
                .build()

        matchingRepository.save(request.matchId, response)
        return response
    }

    override suspend fun getMatchResult(request: GetMatchResultRequest): GetMatchResultResponse {
        if (request.matchId.isBlank()) {
            throw StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("match_id is required"),
            )
        }

        val result = matchingRepository.findById(request.matchId)

        return if (result != null) {
            GetMatchResultResponse
                .newBuilder()
                .setMatchId(request.matchId)
                .setStatus(result.status)
                .setResult(result)
                .build()
        } else {
            GetMatchResultResponse
                .newBuilder()
                .setMatchId(request.matchId)
                .setStatus(MatchStatus.MATCH_STATUS_UNSPECIFIED)
                .build()
        }
    }

    internal fun findMatches(
        supplies: List<SupplyEntity>,
        demands: List<DemandEntity>,
        maxDistance: Double,
        minScore: Double,
    ): List<MatchPair> {
        val candidates = buildCandidates(supplies, demands, maxDistance, minScore)

        // Greedy assignment: best score first, each supply/demand matched at most once
        candidates.sortByDescending { it.totalScore }

        return greedyAssign(candidates)
    }

    private fun buildCandidates(
        supplies: List<SupplyEntity>,
        demands: List<DemandEntity>,
        maxDistance: Double,
        minScore: Double,
    ): MutableList<MatchPair> {
        val candidates = mutableListOf<MatchPair>()

        for (supply in supplies) {
            for (demand in demands) {
                scorePair(supply, demand, maxDistance, minScore)?.let { pair ->
                    candidates.add(pair)
                    trimCandidatesIfNeeded(candidates)
                }
            }
        }
        return candidates
    }

    private fun scorePair(
        supply: SupplyEntity,
        demand: DemandEntity,
        maxDistance: Double,
        minScore: Double,
    ): MatchPair? {
        val distScore = MatchingScorer.distanceScore(supply, demand, maxDistance)
        val timeScore = MatchingScorer.timeOverlapScore(supply, demand)
        val catScore = MatchingScorer.categoryScore(supply, demand)
        val total = MatchingScorer.totalScore(distScore, timeScore, catScore)

        return if (distScore <= 0.0 || total < minScore) {
            null
        } else {
            MatchPair
                .newBuilder()
                .setSupplyId(supply.id.toString())
                .setDemandId(demand.id.toString())
                .setDistanceScore(distScore)
                .setTimeOverlapScore(timeScore)
                .setCategoryScore(catScore)
                .setTotalScore(total)
                .setDistanceMeters(MatchingScorer.distanceMeters(supply, demand))
                .build()
        }
    }

    /**
     * Guard against memory exhaustion: keep only top candidates by score.
     */
    private fun trimCandidatesIfNeeded(candidates: MutableList<MatchPair>) {
        if (candidates.size > MAX_CANDIDATE_PAIRS) {
            candidates.sortByDescending { it.totalScore }
            while (candidates.size > MAX_CANDIDATE_PAIRS) {
                candidates.removeAt(candidates.lastIndex)
            }
        }
    }

    private fun greedyAssign(candidates: List<MatchPair>): List<MatchPair> {
        val matchedSupplies = mutableSetOf<String>()
        val matchedDemands = mutableSetOf<String>()
        val result = mutableListOf<MatchPair>()

        for (pair in candidates) {
            if (pair.supplyId in matchedSupplies || pair.demandId in matchedDemands) {
                continue
            }
            result.add(pair)
            matchedSupplies.add(pair.supplyId)
            matchedDemands.add(pair.demandId)
        }

        return result
    }

    /**
     * Fetches all available supplies by paginating through the repository.
     */
    private fun fetchAllSupplies(): List<SupplyEntity> {
        val all = mutableListOf<SupplyEntity>()
        var pageToken: PageToken? = null
        do {
            val (page, _) =
                supplyRepository.findFiltered(
                    providerId = null,
                    category = null,
                    status = AVAILABLE_STATUS,
                    pageSize = PAGE_SIZE,
                    pageToken = pageToken,
                )
            all.addAll(page)
            pageToken =
                if (page.size == PAGE_SIZE) {
                    val last = page.last()
                    PageToken(last.createdAt, last.id.toString())
                } else {
                    null
                }
        } while (pageToken != null)
        return all
    }

    /**
     * Fetches all active demands by paginating through the repository.
     */
    private fun fetchAllDemands(): List<DemandEntity> {
        val all = mutableListOf<DemandEntity>()
        var pageToken: PageToken? = null
        do {
            val (page, _) =
                demandRepository.findFiltered(
                    recipientId = null,
                    category = null,
                    status = ACTIVE_STATUS,
                    pageSize = PAGE_SIZE,
                    pageToken = pageToken,
                )
            all.addAll(page)
            pageToken =
                if (page.size == PAGE_SIZE) {
                    val last = page.last()
                    PageToken(last.createdAt, last.id.toString())
                } else {
                    null
                }
        } while (pageToken != null)
        return all
    }

    companion object {
        private const val DEFAULT_MAX_DISTANCE = 50_000.0
        private const val DEFAULT_MIN_SCORE = 0.5
        private const val PAGE_SIZE = 1000
        private const val MAX_CANDIDATE_PAIRS = 10_000
        private const val AVAILABLE_STATUS = 1
        private const val ACTIVE_STATUS = 1
    }
}
