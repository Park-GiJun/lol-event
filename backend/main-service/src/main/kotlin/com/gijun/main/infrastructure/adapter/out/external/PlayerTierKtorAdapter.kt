package com.gijun.main.infrastructure.adapter.out.external

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.application.port.out.external.PlayerTier
import com.gijun.main.application.port.out.external.PlayerTierKtorPort
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Riot ID 로 계정을 찾고(account-v1), 거기서 받은 puuid 로 티어를 묻는다(league-v4).
 *
 * DB 의 puuid 는 롤 클라이언트(LCU)가 준 것이라 Riot API 의 puuid 와 다르다. 그래서 매번 Riot ID 에서 출발한다.
 *
 * 한 사람에 두 번 나가고 예측 한 번에 열 명을 묻는다. 티어는 하루에 크게 움직이지 않으므로 오래 기억해 둔다 —
 * 그러지 않으면 같은 사람들로 몇 번만 물어도 호출 한도(2분에 100번)에 닿는다.
 */
@Component
class PlayerTierKtorAdapter(
    @Value("\${riot.api.key:}") private val apiKey: String,
    private val client: HttpClient,
    private val objectMapper: ObjectMapper,
) : PlayerTierKtorPort {
    private val log = LoggerFactory.getLogger(javaClass)

    private class Cached(
        val tier: PlayerTier?,
        val expiresAt: Long,
    )

    private val cache = ConcurrentHashMap<String, Cached>()

    override fun findTier(riotId: String): PlayerTier? {
        val now = System.currentTimeMillis()
        cache[riotId]?.takeIf { it.expiresAt > now }?.let { return it.tier }

        val fetched =
            runCatching { fetch(riotId) }
                .onFailure { log.warn("티어 조회 실패 ($riotId): ${it.message}") }
        // 실패는 잠깐만 기억한다. 키가 만료됐을 때 물을 때마다 스무 번씩 다시 나가지 않게 하되, 고치면 곧 풀리게.
        val ttl = if (fetched.isSuccess) TTL_MILLIS else FAILURE_TTL_MILLIS
        return fetched.getOrNull().also { cache[riotId] = Cached(it, now + ttl) }
    }

    /** @return 배치를 안 본 사람은 null. 계정을 못 찾았거나 응답이 이상하면 예외. */
    private fun fetch(riotId: String): PlayerTier? =
        runBlocking {
            val name = riotId.substringBeforeLast('#')
            val tag = riotId.substringAfterLast('#', "")
            require(name.isNotBlank() && tag.isNotBlank()) { "Riot ID 형식이 아니다" }

            val account = getJson("$ASIA/riot/account/v1/accounts/by-riot-id/${encode(name)}/${encode(tag)}")
            val puuid = objectMapper.readTree(account).path("puuid").asText()
            require(puuid.isNotBlank()) { "계정 응답에 puuid 가 없다" }

            val entries =
                objectMapper.readTree(getJson("$KR/lol/league/v4/entries/by-puuid/$puuid")).associate { entry ->
                    entry.path("queueType").asText() to
                        PlayerTier(
                            queue = entry.path("queueType").asText(),
                            tier = entry.path("tier").asText(),
                            division = entry.path("rank").asText(),
                            lp = entry.path("leaguePoints").asInt(),
                        )
                }
            entries[QUEUE_SOLO] ?: entries[QUEUE_FLEX]
        }

    private suspend fun getJson(url: String): String {
        val response = client.get(url) { header("X-Riot-Token", apiKey) }
        check(response.status.isSuccess()) { "Riot API ${response.status.value}" }
        return response.bodyAsText()
    }

    // URLEncoder 는 폼 규칙이라 빈칸을 + 로 바꾼다. 경로에서는 %20 이어야 한다.
    private fun encode(part: String): String = URLEncoder.encode(part, Charsets.UTF_8).replace("+", "%20")

    private companion object {
        const val ASIA = "https://asia.api.riotgames.com"
        const val KR = "https://kr.api.riotgames.com"
        const val QUEUE_SOLO = "RANKED_SOLO_5x5"
        const val QUEUE_FLEX = "RANKED_FLEX_SR"
        const val TTL_MILLIS = 6 * 60 * 60 * 1000L
        const val FAILURE_TTL_MILLIS = 5 * 60 * 1000L
    }
}
