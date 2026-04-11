package net.gijun.collector.lcu

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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

data class RunePage(
    val id: Long,
    val name: String,
    val primaryStyleId: Int,
    val subStyleId: Int,
    val selectedPerkIds: List<Int>,
    val current: Boolean,
)

data class LiveClientPlayer(
    val summonerName: String,
    val championName: String,
    val team: String,
    val level: Int,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val creepScore: Int,
)

data class LiveClientEvent(
    val eventName: String,
    val eventTime: Double,
    val killerName: String?,
    val assisters: List<String>,
)

data class LiveClientData(
    val players: List<LiveClientPlayer>,
    val events: List<LiveClientEvent>,
    val gameTime: Double,
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

    // ── Rune API ────────────────────────────────────

    suspend fun getCurrentRunePage(): RunePage? {
        val creds = getCredentials() ?: return null
        return try {
            val data = lcuGet(creds.port, creds.password, "/lol-perks/v1/currentpage").jsonObject
            parseRunePage(data)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getRunePages(): List<RunePage> {
        val creds = getCredentials() ?: return emptyList()
        return try {
            val data = lcuGet(creds.port, creds.password, "/lol-perks/v1/pages")
            data.jsonArray.mapNotNull { parseRunePage(it.jsonObject) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseRunePage(obj: JsonObject): RunePage? {
        val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return null
        return RunePage(
            id = id,
            name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
            primaryStyleId = obj["primaryStyleId"]?.jsonPrimitive?.intOrNull ?: 0,
            subStyleId = obj["subStyleId"]?.jsonPrimitive?.intOrNull ?: 0,
            selectedPerkIds = obj["selectedPerkIds"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList(),
            current = obj["current"]?.jsonPrimitive?.booleanOrNull ?: false,
        )
    }

    suspend fun deleteRunePage(pageId: Long): Boolean {
        val creds = getCredentials() ?: return false
        return try {
            val token = Base64.getEncoder().encodeToString("riot:${creds.password}".toByteArray())
            val response = httpClient.delete("https://127.0.0.1:${creds.port}/lol-perks/v1/pages/$pageId") {
                header("Authorization", "Basic $token")
            }
            response.status.isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createRunePage(name: String, primaryStyleId: Int, subStyleId: Int, perkIds: List<Int>): Boolean {
        val creds = getCredentials() ?: return false
        return try {
            val body = buildJsonObject {
                put("name", name)
                put("primaryStyleId", primaryStyleId)
                put("subStyleId", subStyleId)
                put("selectedPerkIds", JsonArray(perkIds.map { JsonPrimitive(it) }))
            }
            val token = Base64.getEncoder().encodeToString("riot:${creds.password}".toByteArray())
            val response = httpClient.post("https://127.0.0.1:${creds.port}/lol-perks/v1/pages") {
                header("Authorization", "Basic $token")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            response.status.isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Apply runes by deleting the current editable page and creating a new one.
     */
    suspend fun applyRunePage(name: String, primaryStyleId: Int, subStyleId: Int, perkIds: List<Int>): Boolean {
        // Find first editable (non-preset) page and delete it to make room
        val pages = getRunePages()
        val editable = pages.filter { !it.name.startsWith("*") && it.id > 0 }
        if (editable.isNotEmpty()) {
            deleteRunePage(editable.last().id)
        }
        return createRunePage(name, primaryStyleId, subStyleId, perkIds)
    }

    // ── Live Client Data API (port 2999) ─────────────

    suspend fun getLiveClientData(): LiveClientData? {
        return try {
            val response = httpClient.get("https://127.0.0.1:2999/liveclientdata/allgamedata")
            val text = response.body<String>()
            val root = Json.parseToJsonElement(text).jsonObject

            val gameTime = root["gameData"]?.jsonObject?.get("gameTime")?.jsonPrimitive?.doubleOrNull ?: 0.0

            val players = root["allPlayers"]?.jsonArray?.map { p ->
                val po = p.jsonObject
                val scores = po["scores"]?.jsonObject
                LiveClientPlayer(
                    summonerName = po["riotIdGameName"]?.jsonPrimitive?.contentOrNull
                        ?: po["summonerName"]?.jsonPrimitive?.contentOrNull ?: "???",
                    championName = po["championName"]?.jsonPrimitive?.contentOrNull ?: "",
                    team = po["team"]?.jsonPrimitive?.contentOrNull ?: "",
                    level = po["level"]?.jsonPrimitive?.intOrNull ?: 0,
                    kills = scores?.get("kills")?.jsonPrimitive?.intOrNull ?: 0,
                    deaths = scores?.get("deaths")?.jsonPrimitive?.intOrNull ?: 0,
                    assists = scores?.get("assists")?.jsonPrimitive?.intOrNull ?: 0,
                    creepScore = scores?.get("creepScore")?.jsonPrimitive?.intOrNull ?: 0,
                )
            } ?: emptyList()

            val events = root["events"]?.jsonObject?.get("Events")?.jsonArray?.map { e ->
                val eo = e.jsonObject
                LiveClientEvent(
                    eventName = eo["EventName"]?.jsonPrimitive?.contentOrNull ?: "",
                    eventTime = eo["EventTime"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    killerName = eo["KillerName"]?.jsonPrimitive?.contentOrNull,
                    assisters = eo["Assisters"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                )
            } ?: emptyList()

            LiveClientData(players, events, gameTime)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getLivePlayerScores(summonerName: String): JsonObject? {
        return try {
            val encoded = java.net.URLEncoder.encode(summonerName, "UTF-8")
            val response = httpClient.get("https://127.0.0.1:2999/liveclientdata/playerscores?summonerName=$encoded")
            val text = response.body<String>()
            Json.parseToJsonElement(text).jsonObject
        } catch (_: Exception) {
            null
        }
    }
}
