package net.gijun.collector.ui.pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import net.gijun.collector.api.ApiClient
import net.gijun.collector.api.PlayerStats
import net.gijun.collector.lcu.LcuClient
import net.gijun.collector.lcu.LiveClientData
import net.gijun.collector.lcu.LiveClientEvent
import net.gijun.collector.lcu.LiveClientPlayer
import net.gijun.collector.ui.components.ChampionIcon
import net.gijun.collector.ui.components.Grid16
import net.gijun.collector.ui.components.colSpan
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.winRateColor

private data class LiveParticipant(
    val summonerName: String,
    val championId: Int,
    val championName: String?,
    val teamId: Int,
)

private data class LiveTeam(
    val teamId: Int,
    val players: List<LiveParticipant>,
)

@Composable
fun LiveGamePage() {
    var phase by remember { mutableStateOf<String?>(null) }
    var teams by remember { mutableStateOf<List<LiveTeam>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var riotIdMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var statsMap by remember { mutableStateOf<Map<String, PlayerStats>>(emptyMap()) }
    var liveData by remember { mutableStateOf<LiveClientData?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    suspend fun load() {
        loading = true
        error = null
        try {
            val data = LcuClient.getLiveGame()
            if (data == null) { phase = null; teams = emptyList(); return }

            phase = data["phase"]?.jsonPrimitive?.contentOrNull
            if (phase != "InProgress") { teams = emptyList(); return }

            val session = data["session"]?.jsonObject
            val gameData = session?.get("gameData")?.jsonObject

            val rawTeams = if (gameData?.get("teamOne") != null || gameData?.get("teamTwo") != null) {
                listOf(
                    LiveTeam(100, (gameData?.get("teamOne")?.jsonArray ?: JsonArray(emptyList())).map { p ->
                        val po = p.jsonObject
                        LiveParticipant(
                            summonerName = po["summonerName"]?.jsonPrimitive?.contentOrNull ?: po["gameName"]?.jsonPrimitive?.contentOrNull ?: "???",
                            championId = po["championId"]?.jsonPrimitive?.intOrNull ?: 0,
                            championName = po["championName"]?.jsonPrimitive?.contentOrNull,
                            teamId = 100,
                        )
                    }),
                    LiveTeam(200, (gameData?.get("teamTwo")?.jsonArray ?: JsonArray(emptyList())).map { p ->
                        val po = p.jsonObject
                        LiveParticipant(
                            summonerName = po["summonerName"]?.jsonPrimitive?.contentOrNull ?: po["gameName"]?.jsonPrimitive?.contentOrNull ?: "???",
                            championId = po["championId"]?.jsonPrimitive?.intOrNull ?: 0,
                            championName = po["championName"]?.jsonPrimitive?.contentOrNull,
                            teamId = 200,
                        )
                    }),
                )
            } else emptyList()

            teams = rawTeams
        } catch (_: Exception) {
            error = "LCU 연결 실패"
        } finally {
            loading = false
        }
    }

    // riotId 매핑 + 내전 통계
    LaunchedEffect(teams) {
        if (teams.isEmpty()) return@LaunchedEffect
        val picks = LcuClient.getCustomMostPicks() ?: return@LaunchedEffect
        val map = mutableMapOf<String, String>()
        (picks.blueTeam + picks.redTeam).forEach { p ->
            if (p.riotId.isNotEmpty() && p.summonerName.isNotEmpty()) {
                map[p.summonerName] = p.riotId
            }
        }
        riotIdMap = map

        val riotIds = map.values.toSet()
        val sm = mutableMapOf<String, PlayerStats>()
        riotIds.forEach { riotId ->
            val stats = ApiClient.fetchPlayerStats(riotId)
            if (stats != null) sm[riotId] = stats
        }
        statsMap = sm
    }

    LaunchedEffect(Unit) { load() }

    // Poll live client data every 5 seconds when game is in progress
    LaunchedEffect(phase) {
        if (phase != "InProgress") { liveData = null; return@LaunchedEffect }
        while (isActive) {
            liveData = LcuClient.getLiveClientData()
            delay(5_000)
        }
    }

    fun teamColor(teamId: Int) = if (teamId == 100) LolColors.Info else LolColors.Error
    fun teamLabel(teamId: Int) = if (teamId == 100) "블루팀" else "레드팀"

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(scrollState)) {
        Text("현재 게임", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("진행 중인 게임의 팀 구성 및 내전 통계", fontSize = 13.sp, color = LolColors.TextSecondary)
        Spacer(Modifier.height(24.dp))

        // 상태 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LolColors.Border),
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("게임 상태", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                OutlinedButton(
                    onClick = { scope.launch { load() } },
                    enabled = !loading,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = LolColors.TextPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text(if (loading) "로딩 중..." else "새로고침", fontSize = 11.sp, color = LolColors.TextPrimary)
                }
            }
            error?.let { Text(it, fontSize = 13.sp, color = LolColors.Error, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)) }
            if (error == null && phase == null && !loading) {
                Text("롤 클라이언트에 연결되지 않았습니다", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp))
            }
            if (phase != null && phase != "InProgress") {
                Row(modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)) {
                    Text("현재 게임 없음 ", fontSize = 13.sp, color = LolColors.TextSecondary)
                    Text("(Phase: $phase)", fontSize = 13.sp, color = LolColors.Primary)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        teams.forEach { team ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("${teamLabel(team.teamId)} (${team.players.size}명)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = teamColor(team.teamId))
                    Spacer(Modifier.height(8.dp))
                    team.players.forEach { p ->
                        val riotId = riotIdMap[p.summonerName]
                        val stats = riotId?.let { statsMap[it] }
                        val eloVal = stats?.elo?.takeIf { it.isFinite() }?.toInt()
                        val eloColor = when {
                            eloVal == null -> LolColors.TextSecondary
                            eloVal >= 1200 -> LolColors.Win
                            eloVal >= 1000 -> LolColors.Primary
                            else -> LolColors.Loss
                        }
                        val topChamps = stats?.championStats?.take(3) ?: emptyList()

                        Grid16(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LolColors.BgHover, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            gap = 10.dp,
                        ) {
                            Box(Modifier.colSpan(1)) { ChampionIcon(p.championId, 32.dp) }
                            Column(Modifier.colSpan(6)) {
                                Text(p.summonerName, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                                    if (eloVal != null) {
                                        Text("Elo $eloVal", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = eloColor)
                                    } else if (stats != null) {
                                        Text("데이터 없음", fontSize = 10.sp, color = LolColors.TextSecondary)
                                    }
                                    stats?.let { Text("${it.games}판 ${it.winRate.toInt()}%", fontSize = 10.sp, color = LolColors.TextSecondary) }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.colSpan(7)) {
                                topChamps.forEach { c ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        ChampionIcon(c.championId, 24.dp)
                                        Text("${c.winRate.toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = winRateColor(c.winRate))
                                    }
                                }
                            }
                            Box(Modifier.colSpan(2)) {
                                p.championName?.let {
                                    Text(it, fontSize = 11.sp, color = LolColors.Primary)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Live Client Data: Real-time Scores ──
        liveData?.let { ld ->
            val gameMinutes = (ld.gameTime / 60).toInt()
            val gameSeconds = (ld.gameTime % 60).toInt()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                        Text("실시간 스코어", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${gameMinutes}:${gameSeconds.toString().padStart(2, '0')}",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = LolColors.TextPrimary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // Team scores summary
                    val orderTeam = ld.players.filter { it.team == "ORDER" }
                    val chaosTeam = ld.players.filter { it.team == "CHAOS" }
                    val orderKills = orderTeam.sumOf { it.kills }
                    val chaosKills = chaosTeam.sumOf { it.kills }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("블루팀", fontSize = 11.sp, color = LolColors.Info)
                            Text("$orderKills", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LolColors.Info)
                        }
                        Text("vs", fontSize = 13.sp, color = LolColors.TextSecondary)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("레드팀", fontSize = 11.sp, color = LolColors.Error)
                            Text("$chaosKills", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LolColors.Error)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(thickness = 1.dp, color = LolColors.Border)
                    Spacer(Modifier.height(8.dp))

                    // Per-player scores
                    ld.players.forEach { player ->
                        val teamColor = if (player.team == "ORDER") LolColors.Info else LolColors.Error
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LolColors.BgHover, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                player.championName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = teamColor,
                                modifier = Modifier.width(80.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                player.summonerName,
                                fontSize = 11.sp,
                                color = LolColors.TextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("Lv${player.level}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = LolColors.TextSecondary)
                            Text(
                                "${player.kills}/${player.deaths}/${player.assists}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = LolColors.TextPrimary,
                            )
                            Text("${player.creepScore}CS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = LolColors.TextDisabled)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Live Client Data: Game Events ──
            val importantEvents = ld.events.filter { event ->
                event.eventName in listOf(
                    "ChampionKill", "DragonKill", "BaronKill", "HeraldKill",
                    "TurretKilled", "InhibKilled", "FirstBlood", "Ace",
                    "Multikill", "FirstBrick",
                )
            }.takeLast(15) // Last 15 important events

            if (importantEvents.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                            Text("게임 이벤트", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                        }
                        Spacer(Modifier.height(8.dp))

                        importantEvents.reversed().forEach { event ->
                            val timeMin = (event.eventTime / 60).toInt()
                            val timeSec = (event.eventTime % 60).toInt()
                            val timeStr = "${timeMin}:${timeSec.toString().padStart(2, '0')}"

                            val (label, color) = when (event.eventName) {
                                "ChampionKill" -> "킬" to LolColors.Error
                                "DragonKill" -> "드래곤" to LolColors.Warning
                                "BaronKill" -> "바론" to Color(0xFFAA44FF)
                                "HeraldKill" -> "전령" to LolColors.Info
                                "TurretKilled" -> "포탑 파괴" to LolColors.TextSecondary
                                "InhibKilled" -> "억제기 파괴" to LolColors.Win
                                "FirstBlood" -> "퍼스트 블러드" to LolColors.Error
                                "Ace" -> "에이스" to LolColors.Win
                                "FirstBrick" -> "첫 포탑" to LolColors.Primary
                                else -> event.eventName to LolColors.TextSecondary
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(timeStr, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = LolColors.TextDisabled, modifier = Modifier.width(36.dp))
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.width(72.dp))
                                Text(
                                    buildString {
                                        event.killerName?.let { append(it) }
                                        if (event.assisters.isNotEmpty()) {
                                            append(" (+${event.assisters.joinToString(", ")})")
                                        }
                                    },
                                    fontSize = 10.sp,
                                    color = LolColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
