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
import net.gijun.collector.ui.components.SignedLineChart
import net.gijun.collector.ui.components.colSpan
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.winRateColor
import kotlin.math.abs
import kotlin.math.max

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

// 시계열 차트용 스냅샷 (메모리에만, 게임 종료 시 폐기)
private data class Snapshot(
    val gameTime: Float,
    val blueGold: Int,
    val redGold: Int,
    val blueKills: Int,
    val redKills: Int,
    val blueCS: Int,
    val redCS: Int,
)

/**
 * 적 골드는 Live Client API가 노출하지 않으므로 CS/킬/어시 + 시간 기반 패시브 골드로 추정.
 * 양 팀에 동일 공식을 적용하므로 절대값은 부정확해도 차이는 의미 있다.
 *  - CS:    21g/마리 (라인 미니언 평균)
 *  - 킬:    300g (셧다운 보너스 무시)
 *  - 어시:  150g
 *  - 패시브: 2.04g/s, 1:50부터 시작
 */
private fun estimatePlayerGold(p: LiveClientPlayer, gameTime: Double): Int {
    val passive = max(0.0, gameTime - 110.0) * 2.04
    return (p.creepScore * 21 + p.kills * 300 + p.assists * 150 + passive).toInt()
}

private fun estimateTeamGold(players: List<LiveClientPlayer>, gameTime: Double): Int =
    players.sumOf { estimatePlayerGold(it, gameTime) }

/**
 * 최근 이벤트에서 마지막 처치 시간을 찾아 다음 리스폰 시각 계산.
 * 한 번도 잡히지 않았으면 첫 스폰 시각.
 */
private fun nextRespawn(
    events: List<LiveClientEvent>,
    eventName: String,
    firstSpawnSec: Double,
    cooldownSec: Double,
    gameTime: Double,
): Double {
    val lastKill = events.filter { it.eventName == eventName }.maxOfOrNull { it.eventTime }
    return if (lastKill == null) max(firstSpawnSec, gameTime) else lastKill + cooldownSec
}

private fun formatCountdown(remainingSec: Double): String {
    if (remainingSec <= 0) return "지금"
    val m = (remainingSec / 60).toInt()
    val s = (remainingSec % 60).toInt()
    return "${m}:${s.toString().padStart(2, '0')}"
}

@Composable
fun LiveGamePage() {
    var phase by remember { mutableStateOf<String?>(null) }
    var teams by remember { mutableStateOf<List<LiveTeam>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var riotIdMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var statsMap by remember { mutableStateOf<Map<String, PlayerStats>>(emptyMap()) }
    var liveData by remember { mutableStateOf<LiveClientData?>(null) }
    val snapshots = remember { mutableStateListOf<Snapshot>() }
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
        if (phase != "InProgress") {
            liveData = null
            snapshots.clear()
            return@LaunchedEffect
        }
        while (isActive) {
            val ld = LcuClient.getLiveClientData()
            liveData = ld
            if (ld != null) {
                val blue = ld.players.filter { it.team == "ORDER" }
                val red = ld.players.filter { it.team == "CHAOS" }
                val newSnap = Snapshot(
                    gameTime = ld.gameTime.toFloat(),
                    blueGold = estimateTeamGold(blue, ld.gameTime),
                    redGold = estimateTeamGold(red, ld.gameTime),
                    blueKills = blue.sumOf { it.kills },
                    redKills = red.sumOf { it.kills },
                    blueCS = blue.sumOf { it.creepScore },
                    redCS = red.sumOf { it.creepScore },
                )
                // gameTime이 감소하면 새 게임 — 히스토리 리셋
                val last = snapshots.lastOrNull()
                if (last != null && newSnap.gameTime < last.gameTime - 30f) {
                    snapshots.clear()
                }
                // 동일 시각 중복 방지 (1초 미만 간격은 건너뜀)
                if (last == null || newSnap.gameTime - last.gameTime >= 1f) {
                    snapshots.add(newSnap)
                    // 너무 많이 쌓이면 앞에서 thinning (1시간 게임 = 720 점 @ 5초 간격, 충분)
                    if (snapshots.size > 1500) {
                        val thinned = snapshots.filterIndexed { idx, _ -> idx % 2 == 0 }
                        snapshots.clear()
                        snapshots.addAll(thinned)
                    }
                }
            }
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

        // ── Live Client Data: Stat Diff Card (Kill / CS / Estimated Gold) ──
        liveData?.let { ld ->
            val blue = ld.players.filter { it.team == "ORDER" }
            val red = ld.players.filter { it.team == "CHAOS" }
            val killDiff = blue.sumOf { it.kills } - red.sumOf { it.kills }
            val csDiff = blue.sumOf { it.creepScore } - red.sumOf { it.creepScore }
            val goldDiff = estimateTeamGold(blue, ld.gameTime) - estimateTeamGold(red, ld.gameTime)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                        Text("팀 차이", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DiffCell("킬", killDiff.toString(), killDiff)
                        DiffCell("CS", csDiff.toString(), csDiff)
                        DiffCell("골드(추정)", formatGoldShort(goldDiff), goldDiff)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Objective Respawn Timers ──
            val nextDragon = nextRespawn(ld.events, "DragonKill", firstSpawnSec = 300.0, cooldownSec = 300.0, gameTime = ld.gameTime)
            val nextBaron = nextRespawn(ld.events, "BaronKill", firstSpawnSec = 1500.0, cooldownSec = 360.0, gameTime = ld.gameTime)
            val nextHerald = nextRespawn(ld.events, "HeraldKill", firstSpawnSec = 840.0, cooldownSec = 360.0, gameTime = ld.gameTime)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                        Text("오브젝트 리스폰", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ObjectiveCell("드래곤", formatCountdown(nextDragon - ld.gameTime), LolColors.Warning)
                        ObjectiveCell("바론", formatCountdown(nextBaron - ld.gameTime), Color(0xFF9B6FDB))
                        // 전령은 보통 한 번만 등장하지만 후속 패치 가능성 있어 같이 표시
                        ObjectiveCell("전령", formatCountdown(nextHerald - ld.gameTime), LolColors.Info)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Gold Diff Time Series Chart ──
            if (snapshots.size >= 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                            Text("골드 차이 추이", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                            Spacer(Modifier.weight(1f))
                            val cur = goldDiff
                            Text(
                                if (cur >= 0) "블루 +${formatGoldShort(cur)}" else "레드 +${formatGoldShort(-cur)}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (cur >= 0) LolColors.Info else LolColors.Error,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        SignedLineChart(
                            points = snapshots.map { it.gameTime to (it.blueGold - it.redGold).toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            height = 120.dp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val firstT = snapshots.first().gameTime
                            val lastT = snapshots.last().gameTime
                            Text(formatGameTime(firstT), fontSize = 9.sp, color = LolColors.TextDisabled)
                            Text(formatGameTime(lastT), fontSize = 9.sp, color = LolColors.TextDisabled)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
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
                                "BaronKill" -> "바론" to Color(0xFF9B6FDB)
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

@Composable
private fun DiffCell(label: String, value: String, signed: Int) {
    val color = when {
        signed > 0 -> LolColors.Info
        signed < 0 -> LolColors.Error
        else -> LolColors.TextSecondary
    }
    val prefix = if (signed > 0) "+" else if (signed < 0) "" else "±"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = LolColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            "$prefix$value",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color,
        )
    }
}

@Composable
private fun ObjectiveCell(label: String, time: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = color)
        Spacer(Modifier.height(4.dp))
        Text(
            time,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = LolColors.TextPrimary,
        )
    }
}

private fun formatGoldShort(g: Int): String {
    val abs = abs(g)
    return if (abs >= 1000) "%.1fk".format(abs / 1000.0) else abs.toString()
}

private fun formatGameTime(t: Float): String {
    val m = (t / 60).toInt()
    val s = (t % 60).toInt()
    return "${m}:${s.toString().padStart(2, '0')}"
}
