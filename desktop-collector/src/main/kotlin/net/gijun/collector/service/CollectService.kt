package net.gijun.collector.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import net.gijun.collector.lcu.LcuClient

object CollectService {

    private const val MAX_GAMES = 500
    private const val PAGE = 20
    private const val SLEEP_MS = 200L

    private val CUSTOM_QUEUE_IDS = setOf(0, 3130, 3270)

    private val httpClient = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var champCache: Map<String, String>? = null
    private var isCollecting = false

    private suspend fun getChampMap(): Map<String, String> {
        champCache?.let { return it }
        val versionsText: String = httpClient.get("https://ddragon.leagueoflegends.com/api/versions.json").body()
        val versions = json.parseToJsonElement(versionsText).jsonArray
        val latest = versions[0].jsonPrimitive.content
        val champText: String = httpClient.get("https://ddragon.leagueoflegends.com/cdn/$latest/data/ko_KR/champion.json").body()
        val data = json.parseToJsonElement(champText).jsonObject["data"]!!.jsonObject
        val map = mutableMapOf<String, String>()
        data.values.forEach { champ ->
            val obj = champ.jsonObject
            map[obj["key"]!!.jsonPrimitive.content] = obj["id"]!!.jsonPrimitive.content
        }
        champCache = map
        return map
    }

    private fun n(e: JsonElement?): Int = e?.jsonPrimitive?.intOrNull ?: 0
    private fun nL(e: JsonElement?): Long = e?.jsonPrimitive?.longOrNull ?: 0
    private fun b(e: JsonElement?): Boolean = e?.jsonPrimitive?.booleanOrNull ?: false
    private fun s(e: JsonElement?): String? = e?.jsonPrimitive?.contentOrNull

    suspend fun runCollect(send: (type: String, message: String) -> Unit) {
        if (isCollecting) {
            send("error", "이미 수집 중입니다")
            return
        }
        isCollecting = true
        try {
            val lockfilePath = LcuClient.findLockfile()
            if (lockfilePath == null) {
                send("error", "lockfile 없음 — 롤 클라이언트를 실행해주세요")
                return
            }
            val creds = LcuClient.parseLockfile(lockfilePath)

            val summoner: JsonObject
            try {
                summoner = LcuClient.lcuGet(creds.port, creds.password, "/lol-summoner/v1/current-summoner").jsonObject
                send("info", "클라이언트 연결 — ${summoner["gameName"]?.jsonPrimitive?.contentOrNull}#${summoner["tagLine"]?.jsonPrimitive?.contentOrNull}")
            } catch (e: Exception) {
                send("error", "LCU 연결 실패 — port:${creds.port} ${e.message}")
                return
            }

            val champMap = getChampMap()
            val newMatches = mutableListOf<JsonObject>()
            val seenGameIds = mutableSetOf<Long>()
            var begIndex = 0

            while (begIndex < MAX_GAMES) {
                try {
                    delay(SLEEP_MS)
                    val endpoint = "/lol-match-history/v1/products/lol/current-summoner/matches?begIndex=$begIndex&endIndex=${begIndex + PAGE - 1}"
                    val data = LcuClient.lcuGet(creds.port, creds.password, endpoint).jsonObject
                    val games = data["games"]?.jsonObject?.get("games")?.jsonArray ?: break
                    if (games.isEmpty()) break

                    val firstGameId = games[0].jsonObject["gameId"]?.jsonPrimitive?.longOrNull ?: 0
                    if (seenGameIds.contains(firstGameId)) {
                        send("info", "매치 히스토리 끝 (${begIndex}번째에서 중복 감지)")
                        break
                    }
                    games.forEach { seenGameIds.add(it.jsonObject["gameId"]?.jsonPrimitive?.longOrNull ?: 0) }
                    send("progress", "${begIndex}~${begIndex + games.size - 1}번 조회 — ${games.size}건")

                    for (game in games) {
                        val g = game.jsonObject
                        val queueId = n(g["queueId"])
                        if (queueId !in CUSTOM_QUEUE_IDS) continue

                        val gameId = nL(g["gameId"])
                        val matchId = "KR_$gameId"
                        delay(SLEEP_MS)

                        val detail: JsonObject
                        try {
                            detail = LcuClient.lcuGet(creds.port, creds.password, "/lol-match-history/v1/games/$gameId").jsonObject
                        } catch (e: Exception) {
                            send("warn", "$matchId 상세 조회 실패 — ${e.message}")
                            continue
                        }

                        val identityMap = mutableMapOf<Int, JsonObject>()
                        detail["participantIdentities"]?.jsonArray?.forEach { identity ->
                            val obj = identity.jsonObject
                            val pid = n(obj["participantId"])
                            identityMap[pid] = obj["player"]?.jsonObject ?: JsonObject(emptyMap())
                        }

                        val participants = JsonArray(
                            (detail["participants"]?.jsonArray ?: JsonArray(emptyList())).map { p ->
                                val po = p.jsonObject
                                val champId = n(po["championId"]).toString()
                                val identity = identityMap[n(po["participantId"])] ?: JsonObject(emptyMap())
                                val st = po["stats"]?.jsonObject ?: JsonObject(emptyMap())
                                val tl = po["timeline"]?.jsonObject ?: JsonObject(emptyMap())
                                val pPuuid = s(identity["puuid"]) ?: ""
                                val gameName = s(identity["gameName"])
                                val tagLine = s(identity["tagLine"])
                                val riotId = if (gameName != null) "$gameName#${tagLine ?: ""}" else (s(identity["summonerName"]) ?: "???")

                                buildJsonObject {
                                    put("puuid", pPuuid)
                                    put("riotId", riotId)
                                    put("champion", champMap[champId] ?: "Champion_$champId")
                                    put("championId", n(po["championId"]))
                                    put("team", if (n(po["teamId"]) == 100) "blue" else "red")
                                    put("teamId", n(po["teamId"]))
                                    put("spell1Id", n(po["spell1Id"]))
                                    put("spell2Id", n(po["spell2Id"]))
                                    put("win", b(st["win"]))
                                    put("kills", n(st["kills"]))
                                    put("deaths", n(st["deaths"]))
                                    put("assists", n(st["assists"]))
                                    put("damage", n(st["totalDamageDealtToChampions"]))
                                    put("cs", n(st["totalMinionsKilled"]) + n(st["neutralMinionsKilled"]))
                                    put("gold", n(st["goldEarned"]))
                                    put("visionScore", n(st["visionScore"]))
                                    put("champLevel", n(st["champLevel"]))
                                    put("doubleKills", n(st["doubleKills"]))
                                    put("tripleKills", n(st["tripleKills"]))
                                    put("quadraKills", n(st["quadraKills"]))
                                    put("pentaKills", n(st["pentaKills"]))
                                    put("unrealKills", n(st["unrealKills"]))
                                    put("killingSprees", n(st["killingSprees"]))
                                    put("largestKillingSpree", n(st["largestKillingSpree"]))
                                    put("largestMultiKill", n(st["largestMultiKill"]))
                                    put("largestCriticalStrike", n(st["largestCriticalStrike"]))
                                    put("longestTimeSpentLiving", n(st["longestTimeSpentLiving"]))
                                    put("firstBloodKill", b(st["firstBloodKill"]))
                                    put("firstBloodAssist", b(st["firstBloodAssist"]))
                                    put("firstTowerKill", b(st["firstTowerKill"]))
                                    put("firstTowerAssist", b(st["firstTowerAssist"]))
                                    put("firstInhibitorKill", b(st["firstInhibitorKill"]))
                                    put("firstInhibitorAssist", b(st["firstInhibitorAssist"]))
                                    put("inhibitorKills", n(st["inhibitorKills"]))
                                    put("turretKills", n(st["turretKills"]))
                                    put("wardsKilled", n(st["wardsKilled"]))
                                    put("wardsPlaced", n(st["wardsPlaced"]))
                                    put("sightWardsBoughtInGame", n(st["sightWardsBoughtInGame"]))
                                    put("visionWardsBoughtInGame", n(st["visionWardsBoughtInGame"]))
                                    for (i in 0..6) put("item$i", n(st["item$i"]))
                                    for (i in 0..5) {
                                        put("perk$i", n(st["perk$i"]))
                                        for (v in 1..3) put("perk${i}Var$v", n(st["perk${i}Var$v"]))
                                    }
                                    put("perkPrimaryStyle", n(st["perkPrimaryStyle"]))
                                    put("perkSubStyle", n(st["perkSubStyle"]))
                                    put("magicDamageDealt", n(st["magicDamageDealt"]))
                                    put("magicDamageDealtToChampions", n(st["magicDamageDealtToChampions"]))
                                    put("magicalDamageTaken", n(st["magicalDamageTaken"]))
                                    put("physicalDamageDealt", n(st["physicalDamageDealt"]))
                                    put("physicalDamageDealtToChampions", n(st["physicalDamageDealtToChampions"]))
                                    put("physicalDamageTaken", n(st["physicalDamageTaken"]))
                                    put("trueDamageDealt", n(st["trueDamageDealt"]))
                                    put("trueDamageDealtToChampions", n(st["trueDamageDealtToChampions"]))
                                    put("trueDamageTaken", n(st["trueDamageTaken"]))
                                    put("totalDamageDealt", n(st["totalDamageDealt"]))
                                    put("totalDamageDealtToChampions", n(st["totalDamageDealtToChampions"]))
                                    put("totalDamageTaken", n(st["totalDamageTaken"]))
                                    put("damageDealtToObjectives", n(st["damageDealtToObjectives"]))
                                    put("damageDealtToTurrets", n(st["damageDealtToTurrets"]))
                                    put("damageSelfMitigated", n(st["damageSelfMitigated"]))
                                    put("totalHeal", n(st["totalHeal"]))
                                    put("totalUnitsHealed", n(st["totalUnitsHealed"]))
                                    put("timeCCingOthers", n(st["timeCCingOthers"]))
                                    put("totalTimeCrowdControlDealt", n(st["totalTimeCrowdControlDealt"]))
                                    put("neutralMinionsKilled", n(st["neutralMinionsKilled"]))
                                    put("neutralMinionsKilledTeamJungle", n(st["neutralMinionsKilledTeamJungle"]))
                                    put("neutralMinionsKilledEnemyJungle", n(st["neutralMinionsKilledEnemyJungle"]))
                                    put("combatPlayerScore", n(st["combatPlayerScore"]))
                                    put("objectivePlayerScore", n(st["objectivePlayerScore"]))
                                    put("totalPlayerScore", n(st["totalPlayerScore"]))
                                    put("totalScoreRank", n(st["totalScoreRank"]))
                                    put("gameEndedInSurrender", b(st["gameEndedInSurrender"]))
                                    put("gameEndedInEarlySurrender", b(st["gameEndedInEarlySurrender"]))
                                    put("causedEarlySurrender", b(st["causedEarlySurrender"]))
                                    put("earlySurrenderAccomplice", b(st["earlySurrenderAccomplice"]))
                                    put("teamEarlySurrendered", b(st["teamEarlySurrendered"]))
                                    for (i in 1..6) put("playerAugment$i", n(st["playerAugment$i"]))
                                    put("playerSubteamId", n(st["playerSubteamId"]))
                                    put("subteamPlacement", n(st["subteamPlacement"]))
                                    put("roleBoundItem", n(st["roleBoundItem"]))
                                    s(tl["lane"])?.let { put("lane", it) } ?: put("lane", JsonNull)
                                    s(tl["role"])?.let { put("role", it) } ?: put("role", JsonNull)
                                }
                            }
                        )

                        val teams = JsonArray(
                            (detail["teams"]?.jsonArray ?: JsonArray(emptyList())).map { t ->
                                val to = t.jsonObject
                                buildJsonObject {
                                    put("teamId", n(to["teamId"]))
                                    put("win", s(to["win"]) == "Win")
                                    put("baronKills", n(to["baronKills"]))
                                    put("dragonKills", n(to["dragonKills"]))
                                    put("towerKills", n(to["towerKills"]))
                                    put("inhibitorKills", n(to["inhibitorKills"]))
                                    put("riftHeraldKills", n(to["riftHeraldKills"]))
                                    put("hordeKills", n(to["hordeKills"]))
                                    put("firstBlood", b(to["firstBlood"]))
                                    put("firstTower", b(to["firstTower"]))
                                    put("firstBaron", b(to["firstBaron"]))
                                    put("firstInhibitor", b(to["firstInhibitor"]))
                                    put("firstDragon", b(to["firstDargon"])) // LCU 오탈자 유지
                                    put("bans", JsonArray(
                                        (to["bans"]?.jsonArray ?: JsonArray(emptyList()))
                                            .filter { n(it.jsonObject["championId"]) > 0 }
                                            .map { ban ->
                                                val bo = ban.jsonObject
                                                buildJsonObject {
                                                    put("championId", n(bo["championId"]))
                                                    put("championName", champMap[n(bo["championId"]).toString()] ?: "Champion_${n(bo["championId"])}")
                                                    put("pickTurn", n(bo["pickTurn"]))
                                                }
                                            }
                                    ))
                                }
                            }
                        )

                        val match = buildJsonObject {
                            put("matchId", matchId)
                            put("queueId", queueId)
                            put("gameCreation", nL(g["gameCreation"]))
                            put("gameDuration", n(g["gameDuration"]))
                            s(detail["gameMode"])?.let { put("gameMode", it) } ?: put("gameMode", JsonNull)
                            s(detail["gameType"])?.let { put("gameType", it) } ?: put("gameType", JsonNull)
                            s(detail["gameVersion"])?.let { put("gameVersion", it) } ?: put("gameVersion", JsonNull)
                            put("mapId", n(detail["mapId"]))
                            put("seasonId", n(detail["seasonId"]))
                            s(detail["platformId"])?.let { put("platformId", it) } ?: put("platformId", JsonNull)
                            put("participants", participants)
                            put("teams", teams)
                        }
                        newMatches.add(match)

                        val dateStr = java.time.Instant.ofEpochMilli(nL(g["gameCreation"]))
                            .atZone(java.time.ZoneId.of("Asia/Seoul"))
                            .toLocalDate().toString()
                        send("info", "$matchId 저장 ($dateStr)")
                    }

                    if (games.size < PAGE) break
                    begIndex += PAGE
                } catch (e: Exception) {
                    send("warn", "페이지 $begIndex 실패: ${e.message}")
                    break
                }
            }

            if (newMatches.isEmpty()) {
                send("done", "수집 완료 — 내전 0건")
                return
            }

            send("info", "서버 전송 중 (${newMatches.size}건)...")
            try {
                val (saved, skipped) = net.gijun.collector.api.ApiClient.postMatches(newMatches)
                send("done", "완료 — ${saved}건 저장, ${skipped}건 중복 스킵")
            } catch (e: Exception) {
                send("error", "서버 전송 실패 — ${e.message}")
            }
        } finally {
            isCollecting = false
        }
    }
}
