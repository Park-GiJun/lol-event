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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.gijun.collector.api.*
import net.gijun.collector.lcu.LcuClient
import net.gijun.collector.lcu.LobbyCache
import net.gijun.collector.lcu.TeamMemberInfo
import net.gijun.collector.ui.components.ChampionIcon
import net.gijun.collector.ui.components.Grid16
import net.gijun.collector.ui.components.colSpan
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.winRateColor
import java.awt.Desktop
import java.net.URI
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow

private data class PlayerData(
    val summonerName: String,
    val riotId: String,
    val isMe: Boolean,
    val championStats: List<ChampionStat>?,
    val fullStats: PlayerStats? = null,
    val riotProfile: RiotProfile? = null,
)

@Composable
fun CustomGamePage() {
    var blueTeam by remember { mutableStateOf<List<PlayerData>>(emptyList()) }
    var redTeam by remember { mutableStateOf<List<PlayerData>>(emptyList()) }
    var phase by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf<String?>(null) }
    var duoSynergies by remember { mutableStateOf<List<DuoSynergy>>(emptyList()) }
    var rivalMatchups by remember { mutableStateOf<List<RivalEntry>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        error = null
        try {
            val result = LcuClient.getCustomMostPicks()
            if (result == null) { error = "LCU 연결 실패"; return }
            phase = result.phase

            suspend fun toPlayerData(m: TeamMemberInfo): PlayerData {
                val fullStats = try { ApiClient.fetchPlayerStats(m.riotId, "normal") } catch (_: Exception) { null }
                return PlayerData(m.summonerName, m.riotId, m.isMe, fullStats?.championStats?.take(6), fullStats)
            }

            val allRiotIds = (result.blueTeam + result.redTeam).map { it.riotId }.filter { it.isNotEmpty() }
            val riotProfiles = try { ApiClient.fetchRiotProfiles(allRiotIds) } catch (_: Exception) { emptyMap() }

            suspend fun toPlayerDataWithProfile(m: TeamMemberInfo): PlayerData {
                val fullStats = try { ApiClient.fetchPlayerStats(m.riotId, "normal") } catch (_: Exception) { null }
                return PlayerData(m.summonerName, m.riotId, m.isMe, fullStats?.championStats?.take(6), fullStats, riotProfiles[m.riotId])
            }

            blueTeam = result.blueTeam.map { toPlayerDataWithProfile(it) }
            redTeam = result.redTeam.map { toPlayerDataWithProfile(it) }
            lastUpdated = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            // Populate LobbyCache
            if (result.blueTeam.isNotEmpty() || result.redTeam.isNotEmpty()) {
                LobbyCache.updateFromLobby(result.blueTeam, result.redTeam)
                // Store PlayerStats in cache
                (blueTeam + redTeam).forEach { p ->
                    if (p.riotId.isNotEmpty() && p.fullStats != null) {
                        LobbyCache.playerStats[p.riotId] = p.fullStats
                    }
                }
                // Fetch duo synergy and rival matchups
                try {
                    val duoResult = ApiClient.fetchDuoSynergy()
                    val allRiotIds = (result.blueTeam + result.redTeam).map { it.riotId }.toSet()
                    duoSynergies = duoResult?.duos?.filter { it.player1 in allRiotIds && it.player2 in allRiotIds } ?: emptyList()
                    LobbyCache.duoSynergies = duoSynergies
                } catch (_: Exception) {}
                try {
                    val rivalResult = ApiClient.fetchRivalMatchup()
                    val blueIds = result.blueTeam.map { it.riotId }.toSet()
                    val redIds = result.redTeam.map { it.riotId }.toSet()
                    rivalMatchups = rivalResult?.rivals?.filter {
                        (it.player1 in blueIds && it.player2 in redIds) || (it.player2 in blueIds && it.player1 in redIds)
                    } ?: emptyList()
                    LobbyCache.rivalMatchups = rivalMatchups
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            error = "데이터 로드 실패"
        } finally {
            loading = false
        }
    }

    // 자동 새로고침 10초
    LaunchedEffect(Unit) {
        while (isActive) {
            load()
            delay(10_000)
        }
    }

    val phaseLabel = mapOf("Lobby" to "대기방", "ChampSelect" to "챔피언 선택", "InProgress" to "게임 중", "None" to "대기 중")
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(scrollState)) {
        Text("내전 분석", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("블루팀 / 레드팀 모스트픽", fontSize = 13.sp, color = LolColors.TextSecondary)
        Spacer(Modifier.height(24.dp))

        // 상태 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LolColors.Border),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    phaseLabel[phase] ?: phase.ifEmpty { "연결 중..." },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LolColors.Primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lastUpdated?.let { Text(it, fontSize = 11.sp, color = LolColors.TextSecondary) }
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
            }
            error?.let { Text(it, fontSize = 13.sp, color = LolColors.Error, modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)) }
        }

        Spacer(Modifier.height(16.dp))

        val hasData = blueTeam.isNotEmpty() || redTeam.isNotEmpty()
        if (!hasData && !loading && phase.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Text("팀 정보가 없습니다", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(24.dp))
            }
        }

        if (hasData) {
            Grid16(modifier = Modifier.fillMaxWidth(), gap = 16.dp) {
                TeamColumn("블루팀", LolColors.Blue, blueTeam, Modifier.colSpan(8))
                TeamColumn("레드팀", LolColors.Red, redTeam, Modifier.colSpan(8))
            }

            Spacer(Modifier.height(24.dp))
            Text("내전 데이터 분석", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LolColors.Primary)
            Spacer(Modifier.height(4.dp))
            Text("Elo 랭킹 · 듀오 시너지 · 라이벌 매치업 · 팀 전력 비교", fontSize = 12.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(16.dp))

            // Team Elo comparison
            TeamEloComparisonCard(blueTeam, redTeam)
            Spacer(Modifier.height(16.dp))

            // Player rankings
            PlayerRankingCard(blueTeam + redTeam)
            Spacer(Modifier.height(16.dp))

            // Duo synergy
            if (duoSynergies.isNotEmpty()) {
                Grid16(modifier = Modifier.fillMaxWidth(), gap = 16.dp) {
                    DuoSynergyCard("블루팀 듀오 시너지", LolColors.Blue, duoSynergies, blueTeam.map { it.riotId }.toSet(), Modifier.colSpan(8))
                    DuoSynergyCard("레드팀 듀오 시너지", LolColors.Red, duoSynergies, redTeam.map { it.riotId }.toSet(), Modifier.colSpan(8))
                }
                Spacer(Modifier.height(16.dp))
            }

            // Rival matchups
            if (rivalMatchups.isNotEmpty()) {
                RivalMatchupCard(rivalMatchups)
                Spacer(Modifier.height(16.dp))
            }

            // Top 3 champions per player
            TopChampsCard(blueTeam, redTeam)
        }
    }
}

// ── Team Elo Comparison ──────────────────────────────

@Composable
private fun TeamEloComparisonCard(blueTeam: List<PlayerData>, redTeam: List<PlayerData>) {
    val blueElos = blueTeam.mapNotNull { it.fullStats?.elo?.takeIf { e -> e.isFinite() } }
    val redElos = redTeam.mapNotNull { it.fullStats?.elo?.takeIf { e -> e.isFinite() } }
    val blueAvg = if (blueElos.isNotEmpty()) blueElos.average() else 1000.0
    val redAvg = if (redElos.isNotEmpty()) redElos.average() else 1000.0
    val winProb = 1.0 / (1.0 + 10.0.pow(-(blueAvg - redAvg) / 400.0))
    val blueWinPct = (winProb * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("팀 전력 비교", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            Spacer(Modifier.height(12.dp))

            if (blueElos.isEmpty() && redElos.isEmpty()) {
                Text("Elo 데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("블루팀", fontSize = 11.sp, color = LolColors.Blue)
                        Text("${blueAvg.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LolColors.Blue)
                        Text("평균 Elo (${blueElos.size}명)", fontSize = 10.sp, color = LolColors.TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("승률 예측", fontSize = 10.sp, color = LolColors.TextSecondary)
                        val probColor = when {
                            blueWinPct >= 60 -> LolColors.Blue
                            blueWinPct <= 40 -> LolColors.Red
                            else -> LolColors.Primary
                        }
                        Text("${blueWinPct}% : ${100 - blueWinPct}%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = probColor)
                        val diff = (blueAvg - redAvg).toInt()
                        val diffLabel = if (diff >= 0) "+$diff" else "$diff"
                        Text("Elo 차이: $diffLabel", fontSize = 10.sp, color = LolColors.TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("레드팀", fontSize = 11.sp, color = LolColors.Red)
                        Text("${redAvg.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LolColors.Red)
                        Text("평균 Elo (${redElos.size}명)", fontSize = 10.sp, color = LolColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                val blueFraction = winProb.toFloat().coerceIn(0.05f, 0.95f)
                Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                    Box(Modifier.weight(blueFraction).fillMaxHeight().background(LolColors.Blue, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)))
                    Box(Modifier.weight(1f - blueFraction).fillMaxHeight().background(LolColors.Red, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)))
                }
            }
        }
    }
}

// ── Player Ranking ──────────────────────────────────

@Composable
private fun PlayerRankingCard(allPlayers: List<PlayerData>) {
    val ranked = allPlayers
        .filter { it.fullStats != null }
        .sortedByDescending { it.fullStats?.elo ?: 0.0 }

    if (ranked.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("플레이어 Elo 랭킹", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            Spacer(Modifier.height(4.dp))
            Text("내전 데이터 기반 Elo · 승률 · KDA", fontSize = 11.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(12.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(20.dp))
                Text("소환사", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.weight(1f))
                Text("솔랭", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(70.dp))
                Text("Elo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(40.dp))
                Text("전적", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(50.dp))
                Text("승률", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(36.dp))
                Text("KDA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(40.dp))
            }
            ranked.forEachIndexed { index, p ->
                val stats = p.fullStats!!
                val rankColor = when (index) {
                    0 -> LolColors.PrimaryLight
                    1 -> Color(0xFFA09B8C)
                    2 -> LolColors.PrimaryDark
                    else -> LolColors.TextPrimary
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (p.isMe) LolColors.Primary.copy(alpha = 0.1f) else LolColors.BgHover, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rankColor, modifier = Modifier.width(20.dp))
                    Text(
                        p.summonerName.ifEmpty { p.riotId.split("#").first() },
                        fontSize = 12.sp,
                        fontWeight = if (p.isMe) FontWeight.Bold else FontWeight.Normal,
                        color = if (p.isMe) LolColors.Primary else LolColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // 솔랭 티어
                    val soloRank = p.riotProfile?.soloRank
                    Text(
                        if (soloRank != null) "${soloRank.tier.take(1)}${soloRank.rank} ${soloRank.lp}LP" else "—",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (soloRank?.tier?.uppercase()) {
                            "CHALLENGER", "GRANDMASTER", "MASTER" -> LolColors.PrimaryLight
                            "DIAMOND", "EMERALD" -> LolColors.Info
                            "PLATINUM", "GOLD" -> LolColors.Primary
                            else -> LolColors.TextSecondary
                        },
                        modifier = Modifier.width(70.dp),
                    )
                    Text(
                        "${stats.elo?.toInt() ?: "—"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            (stats.elo ?: 0.0) >= 1200 -> LolColors.Win
                            (stats.elo ?: 0.0) >= 1000 -> LolColors.Primary
                            else -> LolColors.Loss
                        },
                        modifier = Modifier.width(40.dp),
                    )
                    Text("${stats.wins}W ${stats.losses}L", fontSize = 10.sp, color = LolColors.TextSecondary, modifier = Modifier.width(50.dp))
                    Text("${stats.winRate.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = winRateColor(stats.winRate), modifier = Modifier.width(36.dp))
                    Text(String.format("%.1f", stats.kda), fontSize = 11.sp, color = LolColors.TextSecondary, modifier = Modifier.width(40.dp))
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

// ── Duo Synergy Card ─────────────────────────────────

@Composable
private fun DuoSynergyCard(title: String, color: Color, allDuos: List<DuoSynergy>, teamRiotIds: Set<String>, modifier: Modifier = Modifier) {
    val teamDuos = allDuos.filter { it.player1 in teamRiotIds && it.player2 in teamRiotIds }
        .sortedByDescending { it.winRate }
        .take(5)

    if (teamDuos.isEmpty()) return

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = LolColors.BgSecondary),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.27f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
            Spacer(Modifier.height(4.dp))
            Text("함께 높은 승률을 기록한 조합", fontSize = 10.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            teamDuos.forEach { duo ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(LolColors.BgHover, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(duo.player1.split("#").first(), fontSize = 11.sp, color = LolColors.TextPrimary)
                    Text("+", fontSize = 10.sp, color = LolColors.TextSecondary)
                    Text(duo.player2.split("#").first(), fontSize = 11.sp, color = LolColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text("${duo.winRate.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = winRateColor(duo.winRate))
                    Text("${duo.games}판", fontSize = 10.sp, color = LolColors.TextSecondary)
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

// ── Rival Matchup Card ──────────────────────────────

@Composable
private fun RivalMatchupCard(rivals: List<RivalEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("라이벌 매치업", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Error)
            Spacer(Modifier.height(4.dp))
            Text("블루 vs 레드 개인 대결 기록", fontSize = 11.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
            rivals.sortedByDescending { it.games }.take(8).forEach { rival ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(LolColors.BgHover, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(rival.player1.split("#").first(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Blue)
                    Text("vs", fontSize = 10.sp, color = LolColors.TextSecondary)
                    Text(rival.player2.split("#").first(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Red, modifier = Modifier.weight(1f))
                    Text("${rival.player1Wins}W", fontSize = 11.sp, color = LolColors.Blue)
                    Text("-", fontSize = 10.sp, color = LolColors.TextSecondary)
                    Text("${rival.player2Wins}W", fontSize = 11.sp, color = LolColors.Red)
                    Text("(${rival.games}판)", fontSize = 10.sp, color = LolColors.TextSecondary)
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

// ── Top Champions per Player ─────────────────────────

@Composable
private fun TopChampsCard(blueTeam: List<PlayerData>, redTeam: List<PlayerData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("플레이어별 Top 3 챔피언", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            Spacer(Modifier.height(4.dp))
            Text("내전 데이터 기반 모스트 챔피언 및 승률", fontSize = 11.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(12.dp))

            val allPlayers = blueTeam.map { it to LolColors.Blue } + redTeam.map { it to LolColors.Red }
            allPlayers.forEach { (player, teamColor) ->
                val top3 = player.fullStats?.championStats?.take(3) ?: emptyList()
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(LolColors.BgHover, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        player.summonerName.ifEmpty { player.riotId.split("#").first() },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = teamColor,
                        modifier = Modifier.width(80.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (top3.isEmpty()) {
                        Text("기록 없음", fontSize = 10.sp, color = LolColors.TextDisabled)
                    } else {
                        top3.forEach { stat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(LolColors.BgSecondary, RoundedCornerShape(4.dp))
                                    .border(1.dp, LolColors.Border, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                            ) {
                                ChampionIcon(stat.championId, size = 20.dp)
                                Column {
                                    Text(stat.champion, fontSize = 9.sp, color = LolColors.TextPrimary, maxLines = 1)
                                    Text("${stat.winRate.toInt()}% ${stat.games}판", fontSize = 8.sp, color = winRateColor(stat.winRate))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

@Composable
private fun TeamColumn(title: String, color: Color, players: List<PlayerData>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = LolColors.BgSecondary),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.27f)),
    ) {
        Column {
            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.13f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                Text("${players.size}명", fontSize = 11.sp, color = LolColors.TextSecondary)
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(color))

            // 플레이어 카드
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (players.isEmpty()) {
                    Text("플레이어 없음", fontSize = 12.sp, color = LolColors.TextSecondary)
                } else {
                    players.forEach { player ->
                        PlayerCardCompact(player, color)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerCardCompact(player: PlayerData, accentColor: Color) {
    val borderColor = if (player.isMe) accentColor else LolColors.Border
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LolColors.BgSecondary, RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .then(if (player.isMe) Modifier.border(1.dp, accentColor, RoundedCornerShape(6.dp)) else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (player.isMe) {
                Text(
                    "나",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(accentColor, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Text(
                player.summonerName.ifEmpty { player.riotId },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    try { Desktop.getDesktop().browse(URI("https://gijun.net/player-stats/${java.net.URLEncoder.encode(player.riotId, "UTF-8")}")) } catch (_: Exception) {}
                },
            )
        }
        Spacer(Modifier.height(6.dp))
        if (player.championStats == null) {
            Text("기록 없음", fontSize = 10.sp, color = LolColors.TextSecondary)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                player.championStats.forEach { stat ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ChampionIcon(stat.championId, size = 32.dp)
                        Spacer(Modifier.height(2.dp))
                        Text(stat.champion, fontSize = 9.sp, color = LolColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${stat.winRate.toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = winRateColor(stat.winRate))
                        Text("${stat.games}판", fontSize = 9.sp, color = LolColors.TextSecondary)
                    }
                }
            }
        }
    }
}
