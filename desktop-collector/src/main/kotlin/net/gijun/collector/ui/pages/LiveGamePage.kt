package net.gijun.collector.ui.pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import net.gijun.collector.api.ApiClient
import net.gijun.collector.api.PlayerStats
import net.gijun.collector.lcu.LcuClient
import net.gijun.collector.ui.components.ChampionIcon
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LolColors.BgHover, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ChampionIcon(p.championId, 32.dp)
                            Column(Modifier.weight(1f)) {
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
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                topChamps.forEach { c ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        ChampionIcon(c.championId, 24.dp)
                                        Text("${c.winRate.toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = winRateColor(c.winRate))
                                    }
                                }
                            }
                            p.championName?.let {
                                Text(it, fontSize = 11.sp, color = LolColors.Primary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
