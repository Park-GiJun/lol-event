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
}
