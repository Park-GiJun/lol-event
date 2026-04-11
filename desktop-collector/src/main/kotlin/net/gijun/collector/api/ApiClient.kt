package net.gijun.collector.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

object ApiClient {

    private const val BASE_URL = "https://api.gijun.net/api"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(this@ApiClient.json) }
    }

    suspend fun fetchStatsList(): StatsListResult {
        val response: HttpResponse = client.get("$BASE_URL/stats?mode=all")
        val wrapper = json.decodeFromString<ApiResponse<StatsListResult>>(response.body<String>())
        return wrapper.data ?: StatsListResult()
    }

    suspend fun fetchPlayerStats(riotId: String, mode: String = "all"): PlayerStats? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/player/${riotId.encodeURLPath()}?mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<PlayerStats>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchMatchDetail(matchId: String): MatchDetail? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/matches/${matchId.encodeURLPath()}")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<MatchDetail>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchMatchup(vsChampion: String, mode: String = "normal", samePosition: Boolean = true): MatchupResult? {
        return try {
            val url = buildString {
                append("$BASE_URL/stats/matchup?vsChampion=${vsChampion.encodeURLParameter()}&mode=$mode")
                if (samePosition) append("&samePosition=true")
            }
            val response: HttpResponse = client.get(url)
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<MatchupResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) {
            null
        }
    }

    // ── 듀오 시너지 ──
    suspend fun fetchDuoSynergy(mode: String = "normal", minGames: Int = 2): DuoSynergyResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/duo?mode=$mode&minGames=$minGames")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<DuoSynergyResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 라이벌 매치업 ──
    suspend fun fetchRivalMatchup(mode: String = "normal", minGames: Int = 2): RivalMatchupResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/rival?mode=$mode&minGames=$minGames")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<RivalMatchupResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 챔피언 티어 ──
    suspend fun fetchChampionTier(mode: String = "normal", minGames: Int = 3): ChampionTierResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/champion-tier?mode=$mode&minGames=$minGames")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<ChampionTierResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 밴 분석 ──
    suspend fun fetchBanAnalysis(mode: String = "normal"): BanAnalysisResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/ban-analysis?mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<BanAnalysisResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── Elo 리더보드 ──
    suspend fun fetchEloLeaderboard(): EloLeaderboardResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/elo-leaderboard")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<EloLeaderboardResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── Elo 히스토리 ──
    suspend fun fetchEloHistory(riotId: String, limit: Int = 20): EloHistoryResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/elo-history/${riotId.encodeURLPath()}?limit=$limit")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<EloHistoryResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 플레이어 스트릭 ──
    suspend fun fetchPlayerStreak(riotId: String, mode: String = "normal"): PlayerStreakResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/streak/${riotId.encodeURLPath()}?mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<PlayerStreakResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 어워즈 ──
    suspend fun fetchAwards(mode: String = "normal"): AwardsResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/awards?mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<AwardsResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 멀티킬 하이라이트 ──
    suspend fun fetchMultikillHighlights(mode: String = "normal"): MultikillHighlightsResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/multikill?mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<MultikillHighlightsResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── MVP 랭킹 ──
    suspend fun fetchMvpRanking(mode: String = "normal"): MvpRankingResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/mvp?mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<MvpRankingResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 포지션별 챔피언 풀 ──
    suspend fun fetchPositionPool(mode: String = "normal"): PositionPoolResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/position-pool?mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<PositionPoolResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 플레이스타일 DNA ──
    suspend fun fetchPlaystyleDna(mode: String = "normal"): PlaystyleDnaResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/playstyle-dna?mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<PlaystyleDnaResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 전체 개요 ──
    suspend fun fetchOverview(mode: String = "normal"): OverviewResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/overview?mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<OverviewResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── 플레이어 비교 ──
    suspend fun fetchCompare(player1: String, player2: String, mode: String = "normal"): CompareResult? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/stats/compare?player1=${player1.encodeURLParameter()}&player2=${player2.encodeURLParameter()}&mode=$mode")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<CompareResult>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    suspend fun postMatches(matches: List<JsonObject>): Pair<Int, Int> {
        val body = buildJsonObject {
            put("matches", JsonArray(matches))
        }
        val response: HttpResponse = client.post("$BASE_URL/matches/bulk") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val result = json.parseToJsonElement(response.body<String>()).jsonObject
        val data = result["data"]?.jsonObject
        val saved = data?.get("saved")?.jsonPrimitive?.intOrNull ?: 0
        val skipped = data?.get("skipped")?.jsonPrimitive?.intOrNull ?: 0
        return saved to skipped
    }

    // ── 매치 목록 (경기 기록) ──
    suspend fun fetchRecentMatches(mode: String = "normal", limit: Int = 30): MatchListResult {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/matches?mode=$mode&limit=$limit")
            if (!response.status.isSuccess()) return MatchListResult()
            val wrapper = json.decodeFromString<ApiResponse<MatchListResult>>(response.body<String>())
            wrapper.data ?: MatchListResult()
        } catch (_: Exception) { MatchListResult() }
    }

    // ── Riot API 프로필 (랭크 + 숙련도) ──
    suspend fun fetchRiotProfile(riotId: String): RiotProfile? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/riot/profile/${riotId.encodeURLPath()}")
            if (!response.status.isSuccess()) return null
            val wrapper = json.decodeFromString<ApiResponse<RiotProfile>>(response.body<String>())
            wrapper.data
        } catch (_: Exception) { null }
    }

    // ── Riot API 프로필 일괄 조회 ──
    suspend fun fetchRiotProfiles(riotIds: List<String>): Map<String, RiotProfile> {
        return try {
            val body = buildJsonObject { put("riotIds", JsonArray(riotIds.map { JsonPrimitive(it) })) }
            val response: HttpResponse = client.post("$BASE_URL/riot/profiles/bulk") {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            if (!response.status.isSuccess()) return emptyMap()
            val wrapper = json.decodeFromString<ApiResponse<Map<String, RiotProfile>>>(response.body<String>())
            wrapper.data ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
    }
}
