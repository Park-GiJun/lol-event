package com.gijun.lolml.extract

import com.gijun.lolml.data.QueueRank
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * 내전 밖의 정보(랭크 게임 티어)를 Riot API 에서 읽는다. 읽기만 한다.
 *
 * DB 의 `puuid` 는 롤 클라이언트(LCU)가 준 것이라 Riot API 의 puuid 와 다르다.
 * 그래서 Riot ID 로 계정을 먼저 찾고, 거기서 받은 puuid 로 티어를 묻는다.
 */
class RiotApi(
    private val apiKey: String,
) {
    private val client = HttpClient.newHttpClient()

    /** @return Riot API 의 puuid. 그런 Riot ID 가 없으면(이름을 바꿨으면) null. */
    fun puuid(riotId: String): String? {
        val name = riotId.substringBeforeLast('#')
        val tag = riotId.substringAfterLast('#', "")
        if (name.isBlank() || tag.isBlank()) return null
        val body = get("$ASIA/riot/account/v1/accounts/by-riot-id/${encode(name)}/${encode(tag)}") ?: return null
        return (Json.parse(body) as Map<*, *>)["puuid"] as String
    }

    /** 큐 이름(`RANKED_SOLO_5x5`, `RANKED_FLEX_SR`) → 지금의 티어. 배치를 안 본 큐는 없다. */
    fun ranks(puuid: String): Map<String, QueueRank> {
        val body = get("$KR/lol/league/v4/entries/by-puuid/$puuid") ?: return emptyMap()
        return (Json.parse(body) as List<*>).filterIsInstance<Map<*, *>>().associate { entry ->
            entry["queueType"] as String to
                QueueRank(
                    tier = entry["tier"] as String,
                    division = entry["rank"] as String,
                    lp = (entry["leaguePoints"] as Double).toInt(),
                    wins = (entry["wins"] as Double).toInt(),
                    losses = (entry["losses"] as Double).toInt(),
                )
        }
    }

    /** @return 응답 본문. 404 면 null. 호출 한도(429)에 걸리면 서버가 말한 만큼 쉬고 다시 묻는다. */
    private fun get(url: String): String? {
        val request = HttpRequest.newBuilder(URI.create(url)).header("X-Riot-Token", apiKey).build()
        repeat(MAX_ATTEMPTS) {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            when (response.statusCode()) {
                200 -> {
                    return response.body()
                }

                404 -> {
                    return null
                }

                429 -> {
                    val retryAfterSec = response.headers().firstValue("Retry-After").orElse("5")
                    Thread.sleep((retryAfterSec.toLong() + 1) * 1000)
                }

                401, 403 -> {
                    error("Riot API 키가 틀렸거나 만료됐다 (${response.statusCode()})")
                }

                else -> {
                    error("Riot API ${response.statusCode()}: ${response.body().take(200)}")
                }
            }
        }
        error("Riot API 호출 한도가 풀리지 않는다: $url")
    }

    // URLEncoder 는 폼 규칙이라 빈칸을 + 로 바꾼다. 경로에서는 %20 이어야 한다.
    private fun encode(part: String): String = URLEncoder.encode(part, Charsets.UTF_8).replace("+", "%20")

    companion object {
        private const val ASIA = "https://asia.api.riotgames.com"
        private const val KR = "https://kr.api.riotgames.com"
        private const val MAX_ATTEMPTS = 5

        /** `RIOT_API_KEY` */
        fun fromEnv(): RiotApi = RiotApi(System.getenv("RIOT_API_KEY")?.takeIf { it.isNotBlank() } ?: error("환경변수 RIOT_API_KEY 가 없다"))
    }
}
