package net.gijun.collector.lcu

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class LcuCredentials(val port: String, val password: String)

data class LcuStatus(
    val connected: Boolean,
    val gameName: String? = null,
    val tagLine: String? = null,
    val puuid: String? = null,
    val reason: String? = null,
)

data class ChampSelectSlot(
    val cellId: Int,
    val summonerId: Long? = null,
    val championId: Int,
    val assignedPosition: String,
    val riotId: String,
    val summonerName: String,
    val isMe: Boolean,
)

data class ChampSelectFull(
    val myTeam: List<ChampSelectSlot>,
    val theirTeam: List<ChampSelectSlot>,
    val bans: List<BannedChamp>,
    val phase: String,
    val timer: Int,
)

data class BannedChamp(val championId: Int, val team: String)

data class TeamMemberInfo(
    val summonerName: String,
    val riotId: String,
    val isMe: Boolean,
)

data class CustomTeamsResult(
    val phase: String,
    val blueTeam: List<TeamMemberInfo>,
    val redTeam: List<TeamMemberInfo>,
)

object LcuClient {

    private val LOCKFILE_PATHS = listOf(
        "C:/Riot Games/League of Legends/lockfile",
        "C:/Program Files/Riot Games/League of Legends/lockfile",
        "C:/Program Files (x86)/Riot Games/League of Legends/lockfile",
        "${System.getenv("LOCALAPPDATA") ?: ""}/Riot Games/League of Legends/lockfile",
    )

    private val summonerInfoCache = ConcurrentHashMap<Long, Pair<String, String>>() // summonerId -> (riotId, summonerName)

    private val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
    }

    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                sslSocketFactory(sslContext.socketFactory, trustAllManager)
                hostnameVerifier { _, _ -> true }
            }
        }
    }

    fun findLockfile(): String? =
        LOCKFILE_PATHS.firstOrNull { File(it).exists() }

    fun parseLockfile(path: String): LcuCredentials {
        val parts = File(path).readText().split(":")
        return LcuCredentials(port = parts[2].trim(), password = parts[3].trim())
    }

    private fun getCredentials(): LcuCredentials? {
        val path = findLockfile() ?: return null
        return parseLockfile(path)
    }

    suspend fun lcuGet(port: String, password: String, endpoint: String): JsonElement {
        val token = Base64.getEncoder().encodeToString("riot:$password".toByteArray())
        val response: HttpResponse = httpClient.get("https://127.0.0.1:$port$endpoint") {
            header("Authorization", "Basic $token")
        }
        val text = response.body<String>()
        return Json.parseToJsonElement(text)
    }

    suspend fun getStatus(): LcuStatus {
        val creds = getCredentials()
            ?: return LcuStatus(connected = false, reason = "lockfile 없음 — 롤 클라이언트를 실행해주세요")
        return try {
            val data = lcuGet(creds.port, creds.password, "/lol-summoner/v1/current-summoner").jsonObject
            LcuStatus(
                connected = true,
                gameName = data["gameName"]?.jsonPrimitive?.contentOrNull ?: data["displayName"]?.jsonPrimitive?.contentOrNull,
                tagLine = data["tagLine"]?.jsonPrimitive?.contentOrNull ?: "",
                puuid = data["puuid"]?.jsonPrimitive?.contentOrNull,
            )
        } catch (_: Exception) {
            LcuStatus(connected = false, reason = "클라이언트 응답 없음 — 로그인 확인")
        }
    }

    suspend fun getLiveGame(): JsonObject? {
        val creds = getCredentials() ?: return null
        return try {
            val phaseRaw = lcuGet(creds.port, creds.password, "/lol-gameflow/v1/gameflow-phase")
            val phase = phaseRaw.jsonPrimitive.content.replace("\"", "").trim()
            if (phase != "InProgress") {
                return buildJsonObject { put("phase", phase) }
            }
            val session = lcuGet(creds.port, creds.password, "/lol-gameflow/v1/session").jsonObject
            buildJsonObject {
                put("phase", phase)
                put("session", session)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getGamePhase(): String? {
        val creds = getCredentials() ?: return null
        return try {
            val raw = lcuGet(creds.port, creds.password, "/lol-gameflow/v1/gameflow-phase")
            raw.jsonPrimitive.content.replace("\"", "").trim()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun summonerIdToInfo(port: String, password: String, summonerId: Long): Pair<String, String>? {
        val cached = summonerInfoCache[summonerId]
        return try {
            val summoner = lcuGet(port, password, "/lol-summoner/v1/summoners/$summonerId").jsonObject
            val gameName = summoner["gameName"]?.jsonPrimitive?.contentOrNull ?: ""
            val tagLine = summoner["tagLine"]?.jsonPrimitive?.contentOrNull ?: ""
            val displayName = summoner["displayName"]?.jsonPrimitive?.contentOrNull ?: gameName
            val riotId = if (gameName.isNotEmpty()) "$gameName#$tagLine" else displayName
            val result = riotId to (displayName.ifEmpty { gameName })
            if (riotId.isNotEmpty()) summonerInfoCache[summonerId] = result
            result
        } catch (_: Exception) {
            cached
        }
    }

    suspend fun cacheLobbyMembers() {
        val creds = getCredentials() ?: return
        try {
            val lobby = lcuGet(creds.port, creds.password, "/lol-lobby/v2/lobby").jsonObject
            val gameConfig = lobby["gameConfig"]?.jsonObject ?: return
            val team100 = gameConfig["customTeam100"]?.jsonArray ?: JsonArray(emptyList())
            val team200 = gameConfig["customTeam200"]?.jsonArray ?: JsonArray(emptyList())
            (team100 + team200).forEach { member ->
                val sid = member.jsonObject["summonerId"]?.jsonPrimitive?.longOrNull ?: return@forEach
                summonerIdToInfo(creds.port, creds.password, sid)
            }
        } catch (_: Exception) {
            // 로비 미진입
        }
    }

    suspend fun getChampSelectFull(): ChampSelectFull? {
        val creds = getCredentials() ?: return null
        return try {
            val session = lcuGet(creds.port, creds.password, "/lol-champ-select/v1/session").jsonObject
            val myTeamRaw = session["myTeam"]?.jsonArray ?: JsonArray(emptyList())
            val theirTeamRaw = session["theirTeam"]?.jsonArray ?: JsonArray(emptyList())
            val localCellId = session["localPlayerCellId"]?.jsonPrimitive?.intOrNull
            val timerObj = session["timer"]?.jsonObject
            val phase = timerObj?.get("phase")?.jsonPrimitive?.contentOrNull ?: ""
            val remaining = timerObj?.get("adjustedTimeLeftInPhase")?.jsonPrimitive?.longOrNull ?: 0

            suspend fun resolveSlot(s: JsonElement): ChampSelectSlot {
                val slot = s.jsonObject
                val summonerId = slot["summonerId"]?.jsonPrimitive?.longOrNull
                val championId = slot["championId"]?.jsonPrimitive?.intOrNull ?: 0
                val cellId = slot["cellId"]?.jsonPrimitive?.intOrNull ?: 0
                val assignedPosition = slot["assignedPosition"]?.jsonPrimitive?.contentOrNull ?: ""
                val isMe = cellId == localCellId
                val (riotId, summonerName) = if (summonerId != null) {
                    summonerIdToInfo(creds.port, creds.password, summonerId) ?: ("" to "")
                } else ("" to "")
                return ChampSelectSlot(cellId, summonerId, championId, assignedPosition, riotId, summonerName, isMe)
            }

            var banIdx = 0
            val bans = mutableListOf<BannedChamp>()
            session["actions"]?.jsonArray?.forEach { group ->
                group.jsonArray.forEach { a ->
                    val action = a.jsonObject
                    if (action["type"]?.jsonPrimitive?.contentOrNull == "ban"
                        && action["completed"]?.jsonPrimitive?.booleanOrNull == true
                    ) {
                        val champId = action["championId"]?.jsonPrimitive?.intOrNull ?: 0
                        if (champId > 0) {
                            bans.add(BannedChamp(champId, if (banIdx < 5) "blue" else "red"))
                            banIdx++
                        }
                    }
                }
            }

            val myTeam = myTeamRaw.map { resolveSlot(it) }
            val theirTeam = theirTeamRaw.map { resolveSlot(it) }

            ChampSelectFull(myTeam, theirTeam, bans, phase, (remaining / 1000).toInt())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getCustomMostPicks(): CustomTeamsResult? {
        val creds = getCredentials() ?: return null
        return try {
            val phaseRaw = lcuGet(creds.port, creds.password, "/lol-gameflow/v1/gameflow-phase")
            val phase = phaseRaw.jsonPrimitive.content.replace("\"", "").trim()

            when (phase) {
                "Lobby" -> {
                    val lobby = lcuGet(creds.port, creds.password, "/lol-lobby/v2/lobby").jsonObject
                    val gameConfig = lobby["gameConfig"]?.jsonObject
                    val localMember = lobby["localMember"]?.jsonObject
                    val mySummonerId = localMember?.get("summonerId")?.jsonPrimitive?.longOrNull

                    val team100 = gameConfig?.get("customTeam100")?.jsonArray ?: JsonArray(emptyList())
                    val team200 = gameConfig?.get("customTeam200")?.jsonArray ?: JsonArray(emptyList())

                    suspend fun toInfo(m: JsonElement): TeamMemberInfo? {
                        val sid = m.jsonObject["summonerId"]?.jsonPrimitive?.longOrNull ?: return null
                        val (riotId, summonerName) = summonerIdToInfo(creds.port, creds.password, sid) ?: return null
                        return TeamMemberInfo(summonerName, riotId, sid == mySummonerId)
                    }

                    CustomTeamsResult(phase, team100.mapNotNull { toInfo(it) }, team200.mapNotNull { toInfo(it) })
                }

                "ChampSelect" -> {
                    val session = lcuGet(creds.port, creds.password, "/lol-gameflow/v1/session").jsonObject
                    val queueId = session["gameData"]?.jsonObject?.get("queue")?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
                    if (queueId != null && queueId != 0) return CustomTeamsResult(phase, emptyList(), emptyList())

                    val champSession = lcuGet(creds.port, creds.password, "/lol-champ-select/v1/session").jsonObject
                    val myTeamRaw = champSession["myTeam"]?.jsonArray ?: JsonArray(emptyList())
                    val theirTeamRaw = champSession["theirTeam"]?.jsonArray ?: JsonArray(emptyList())
                    val localCellId = champSession["localPlayerCellId"]?.jsonPrimitive?.intOrNull
                    val iAmBlue = localCellId == null || localCellId < 5

                    suspend fun toInfo(s: JsonElement): TeamMemberInfo? {
                        val sid = s.jsonObject["summonerId"]?.jsonPrimitive?.longOrNull ?: return null
                        val (riotId, summonerName) = summonerIdToInfo(creds.port, creds.password, sid) ?: return null
                        return TeamMemberInfo(summonerName, riotId, s.jsonObject["cellId"]?.jsonPrimitive?.intOrNull == localCellId)
                    }

                    val myInfos = myTeamRaw.mapNotNull { toInfo(it) }
                    val theirInfos = theirTeamRaw.mapNotNull { toInfo(it) }
                    val (blue, red) = if (iAmBlue) myInfos to theirInfos else theirInfos to myInfos
                    CustomTeamsResult(phase, blue, red)
                }

                "InProgress" -> {
                    val session = lcuGet(creds.port, creds.password, "/lol-gameflow/v1/session").jsonObject
                    val gameData = session["gameData"]?.jsonObject
                    val queueId = gameData?.get("queue")?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
                    if (queueId != null && queueId != 0) return CustomTeamsResult(phase, emptyList(), emptyList())

                    val teamOne = gameData?.get("teamOne")?.jsonArray ?: JsonArray(emptyList())
                    val teamTwo = gameData?.get("teamTwo")?.jsonArray ?: JsonArray(emptyList())
                    val mySummonerId = session["localPlayer"]?.jsonObject?.get("summonerId")?.jsonPrimitive?.longOrNull

                    suspend fun toInfo(p: JsonElement): TeamMemberInfo? {
                        val sid = p.jsonObject["summonerId"]?.jsonPrimitive?.longOrNull ?: return null
                        val (riotId, summonerName) = summonerIdToInfo(creds.port, creds.password, sid) ?: return null
                        return TeamMemberInfo(summonerName, riotId, sid == mySummonerId)
                    }

                    CustomTeamsResult(phase, teamOne.mapNotNull { toInfo(it) }, teamTwo.mapNotNull { toInfo(it) })
                }

                else -> CustomTeamsResult(phase, emptyList(), emptyList())
            }
        } catch (_: Exception) {
            null
        }
    }
}
