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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.gijun.collector.api.*
import net.gijun.collector.ui.components.ChampionIcon
import net.gijun.collector.ui.components.Grid16
import net.gijun.collector.ui.components.colSpan
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.winRateColor

@Composable
fun DashboardPage() {
    var overview by remember { mutableStateOf<OverviewResult?>(null) }
    var eloLeaderboard by remember { mutableStateOf<EloLeaderboardResult?>(null) }
    var awards by remember { mutableStateOf<AwardsResult?>(null) }
    var multikill by remember { mutableStateOf<MultikillHighlightsResult?>(null) }
    var mvpRanking by remember { mutableStateOf<MvpRankingResult?>(null) }
    var banAnalysis by remember { mutableStateOf<BanAnalysisResult?>(null) }
    var championTier by remember { mutableStateOf<ChampionTierResult?>(null) }
    var duoSynergy by remember { mutableStateOf<DuoSynergyResult?>(null) }
    var statsList by remember { mutableStateOf<StatsListResult?>(null) }
    var playerStreaks by remember { mutableStateOf<Map<String, PlayerStreakResult>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        loading = true
        // Fetch all in parallel via multiple coroutines
        val jobs = listOf(
            scope.launch { overview = ApiClient.fetchOverview() },
            scope.launch { eloLeaderboard = ApiClient.fetchEloLeaderboard() },
            scope.launch { awards = ApiClient.fetchAwards() },
            scope.launch { multikill = ApiClient.fetchMultikillHighlights() },
            scope.launch { mvpRanking = ApiClient.fetchMvpRanking() },
            scope.launch { banAnalysis = ApiClient.fetchBanAnalysis() },
            scope.launch { championTier = ApiClient.fetchChampionTier() },
            scope.launch { duoSynergy = ApiClient.fetchDuoSynergy() },
            scope.launch {
                val stats = ApiClient.fetchStatsList()
                statsList = stats
                // Fetch streaks for top players (by games)
                val topPlayers = stats.stats.sortedByDescending { it.games }.take(10)
                val streakMap = mutableMapOf<String, PlayerStreakResult>()
                topPlayers.forEach { entry ->
                    try {
                        val s = ApiClient.fetchPlayerStreak(entry.riotId)
                        if (s != null) streakMap[entry.riotId] = s
                    } catch (_: Exception) {}
                }
                playerStreaks = streakMap
            },
        )
        jobs.forEach { it.join() }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(scrollState)) {
        Text("내전 대시보드", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("내전 통계 한눈에 보기", fontSize = 13.sp, color = LolColors.TextSecondary)
        Spacer(Modifier.height(24.dp))

        if (loading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Text("데이터 로딩 중...", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(24.dp))
            }
        } else {
            // ── Overview Stats ──
            OverviewSection(overview)
            Spacer(Modifier.height(16.dp))

            Grid16(modifier = Modifier.fillMaxWidth(), gap = 16.dp) {
                // ── Elo Leaderboard (Left) ──
                Box(Modifier.colSpan(8)) {
                    EloLeaderboardSection(eloLeaderboard)
                }
                // ── Awards (Right) ──
                Box(Modifier.colSpan(8)) {
                    AwardsSection(awards)
                }
            }
            Spacer(Modifier.height(16.dp))

            Grid16(modifier = Modifier.fillMaxWidth(), gap = 16.dp) {
                // ── Multikill Highlights (Left) ──
                Box(Modifier.colSpan(8)) {
                    MultikillSection(multikill)
                }
                // ── MVP Ranking (Right) ──
                Box(Modifier.colSpan(8)) {
                    MvpSection(mvpRanking)
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Ban Trends + Champion Tier ──
            Grid16(modifier = Modifier.fillMaxWidth(), gap = 16.dp) {
                Box(Modifier.colSpan(8)) {
                    BanTrendSection(banAnalysis)
                }
                Box(Modifier.colSpan(8)) {
                    ChampionTierSection(championTier)
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Duo Synergy + Player Streaks ──
            Grid16(modifier = Modifier.fillMaxWidth(), gap = 16.dp) {
                Box(Modifier.colSpan(8)) {
                    DuoSynergySection(duoSynergy)
                }
                Box(Modifier.colSpan(8)) {
                    PlayerStreakSection(playerStreaks)
                }
            }
        }
    }
}

@Composable
private fun OverviewSection(overview: OverviewResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                Text("전체 개요", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(16.dp))
            if (overview == null) {
                Text("데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                Grid16(modifier = Modifier.fillMaxWidth(), gap = 12.dp) {
                    OverviewStatBox(Modifier.colSpan(3), "총 경기", "${overview.matchCount}", LolColors.Primary)
                    OverviewStatBox(Modifier.colSpan(3), "평균 시간", "${String.format("%.1f", overview.avgGameMinutes)}분", LolColors.Info)
                    OverviewStatBox(Modifier.colSpan(3), "드래곤", "${overview.totalDragonKills}", LolColors.Warning)
                    OverviewStatBox(Modifier.colSpan(3), "바론", "${overview.totalBaronKills}", Color(0xFFAA6FDB))
                    OverviewStatBox(Modifier.colSpan(4), "포탑", "${overview.totalTowerKills}", LolColors.Error)
                    OverviewStatBox(Modifier.colSpan(4), "총 CS", "${overview.totalCs}", LolColors.Win)
                }
            }
        }
    }
}

@Composable
private fun OverviewStatBox(modifier: Modifier, label: String, value: String, color: Color) {
    Column(
        modifier = modifier
            .background(LolColors.BgTertiary, RoundedCornerShape(8.dp))
            .border(1.dp, LolColors.Border, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = LolColors.TextSecondary)
    }
}

@Composable
private fun EloLeaderboardSection(data: EloLeaderboardResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                Text("Elo 리더보드", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))
            if (data == null || data.players.isEmpty()) {
                Text("데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                data.players.take(10).forEachIndexed { index, entry ->
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD700)
                        1 -> Color(0xFFC0C0C0)
                        2 -> Color(0xFFCD7F32)
                        else -> LolColors.TextSecondary
                    }
                    val eloColor = when {
                        entry.elo >= 1200 -> LolColors.Win
                        entry.elo >= 1000 -> LolColors.Primary
                        else -> LolColors.Loss
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (index % 2 == 0) LolColors.BgHover else Color.Transparent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("#${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rankColor, modifier = Modifier.width(28.dp))
                        Text(
                            entry.riotId.split("#").first(),
                            fontSize = 12.sp,
                            fontWeight = if (index < 3) FontWeight.Bold else FontWeight.Normal,
                            color = LolColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${entry.elo.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = eloColor,
                        )
                        Text("${entry.games}판", fontSize = 10.sp, color = LolColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AwardsSection(data: AwardsResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                Text("어워즈", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))
            if (data == null) {
                Text("데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                val awards = listOf(
                    "펜타킬 영웅" to data.pentaKillHero,
                    "최고 승률" to data.highestWinRate,
                    "KDA 킹" to data.kdaKing,
                    "최다 데스" to data.mostDeaths,
                )
                awards.forEach { (title, entry) ->
                    if (entry != null) {
                        AwardRow(title, entry)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (awards.all { it.second == null }) {
                    Text("어워즈 데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AwardRow(title: String, entry: AwardEntry) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(LolColors.BgTertiary, RoundedCornerShape(6.dp))
            .border(1.dp, LolColors.Border, RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 10.sp, color = LolColors.Warning, fontWeight = FontWeight.Bold)
            Text(entry.riotId.split("#").first(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LolColors.TextPrimary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(entry.displayValue, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LolColors.Primary)
            if (entry.games > 0) Text("${entry.games}판", fontSize = 10.sp, color = LolColors.TextSecondary)
        }
    }
}

@Composable
private fun MultikillSection(data: MultikillHighlightsResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Error)
                Text("멀티킬 하이라이트", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))
            if (data == null || (data.pentaKillEvents.isEmpty() && data.quadraKillEvents.isEmpty())) {
                Text("멀티킬 기록 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                if (data.pentaKillEvents.isNotEmpty()) {
                    Text("펜타킬", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                    Spacer(Modifier.height(4.dp))
                    data.pentaKillEvents.take(5).forEach { event ->
                        MultikillRow(event, "PENTA")
                        Spacer(Modifier.height(4.dp))
                    }
                }
                if (data.quadraKillEvents.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("쿼드라킬", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LolColors.Warning)
                    Spacer(Modifier.height(4.dp))
                    data.quadraKillEvents.take(5).forEach { event ->
                        MultikillRow(event, "QUADRA")
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MultikillRow(event: MultikillEvent, type: String) {
    val date = try {
        val instant = java.time.Instant.ofEpochMilli(event.gameCreation)
        val zoned = instant.atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
        "${zoned.monthValue}/${zoned.dayOfMonth}"
    } catch (_: Exception) { "" }

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(LolColors.BgHover, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(event.riotId.split("#").first(), fontSize = 11.sp, color = LolColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(event.champion, fontSize = 10.sp, color = LolColors.TextSecondary)
        Text(date, fontSize = 10.sp, color = LolColors.TextDisabled)
    }
}

// ── 밴 트렌드 ────────────────────────────────────

@Composable
private fun BanTrendSection(data: BanAnalysisResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.DoNotDisturb, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Error)
                Text("밴 트렌드", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))
            if (data == null || data.bans.isEmpty()) {
                Text("밴 데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                data.bans.take(8).forEachIndexed { index, ban ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (index % 2 == 0) LolColors.BgHover else Color.Transparent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ChampionIcon(ban.championId, 24.dp)
                        Text(ban.champion, fontSize = 11.sp, color = LolColors.TextPrimary, modifier = Modifier.width(60.dp))
                        // Ban rate bar
                        Box(Modifier.weight(1f).height(6.dp).background(LolColors.Border, RoundedCornerShape(3.dp))) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth((ban.banRate / 100.0).toFloat().coerceIn(0f, 1f))
                                    .background(LolColors.Error, RoundedCornerShape(3.dp))
                            )
                        }
                        Text(
                            "${String.format("%.1f", ban.banRate)}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = LolColors.Error,
                            modifier = Modifier.width(38.dp),
                        )
                        Text("${ban.banCount}회", fontSize = 9.sp, color = LolColors.TextSecondary)
                    }
                }
            }
        }
    }
}

// ── 챔피언 티어 ──────────────────────────────────

@Composable
private fun ChampionTierSection(data: ChampionTierResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Warning)
                Text("챔피언 티어", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))
            if (data == null || data.tiers.isEmpty()) {
                Text("챔피언 티어 데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                val tierGroups = data.tiers.groupBy { it.tier }
                listOf("S", "A", "B", "C").forEach { tier ->
                    val champions = tierGroups[tier] ?: return@forEach
                    val tierColor = when (tier) {
                        "S" -> Color(0xFFFFD700)
                        "A" -> LolColors.Win
                        "B" -> LolColors.Info
                        else -> LolColors.TextSecondary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            tier,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tierColor,
                            modifier = Modifier.width(20.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            champions.take(5).forEach { c ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    ChampionIcon(c.championId, 20.dp)
                                    Text(c.champion, fontSize = 10.sp, color = LolColors.TextPrimary, modifier = Modifier.weight(1f))
                                    Text("${c.winRate.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = winRateColor(c.winRate))
                                    Text("${c.games}판", fontSize = 9.sp, color = LolColors.TextDisabled)
                                }
                            }
                        }
                    }
                    if (tier != "C") {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

// ── 듀오 시너지 ──────────────────────────────────

@Composable
private fun DuoSynergySection(data: DuoSynergyResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Info)
                Text("듀오 시너지 Top 5", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))
            if (data == null || data.duos.isEmpty()) {
                Text("듀오 데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                data.duos.take(5).forEachIndexed { index, duo ->
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD700)
                        1 -> Color(0xFFC0C0C0)
                        2 -> Color(0xFFCD7F32)
                        else -> LolColors.TextSecondary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (index % 2 == 0) LolColors.BgHover else Color.Transparent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("#${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rankColor, modifier = Modifier.width(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${duo.player1.split("#").first()} + ${duo.player2.split("#").first()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LolColors.TextPrimary,
                            )
                            Text("${duo.games}판", fontSize = 9.sp, color = LolColors.TextDisabled)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${duo.winRate.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = winRateColor(duo.winRate))
                            Text("KDA ${String.format("%.1f", duo.combinedKda)}", fontSize = 9.sp, color = LolColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

// ── 플레이어 스트릭 ──────────────────────────────

@Composable
private fun PlayerStreakSection(streaks: Map<String, PlayerStreakResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF6B35))
                Text("플레이어 스트릭", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))
            if (streaks.isEmpty()) {
                Text("스트릭 데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                // Hot streaks (winning)
                val hot = streaks.filter { it.value.currentStreakType == "win" && it.value.currentStreak >= 2 }
                    .entries.sortedByDescending { it.value.currentStreak }
                val cold = streaks.filter { it.value.currentStreakType == "loss" && it.value.currentStreak >= 2 }
                    .entries.sortedByDescending { it.value.currentStreak }

                if (hot.isNotEmpty()) {
                    Text("연승 중", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LolColors.Win)
                    Spacer(Modifier.height(4.dp))
                    hot.take(5).forEach { (riotId, s) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(LolColors.Win.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(riotId.split("#").first(), fontSize = 11.sp, color = LolColors.TextPrimary, modifier = Modifier.weight(1f))
                            Text("${s.currentStreak}연승", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LolColors.Win)
                            // Recent form dots
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                s.recentForm.takeLast(5).forEach { win ->
                                    Box(
                                        Modifier.size(10.dp)
                                            .background(if (win) LolColors.Win else LolColors.Loss, RoundedCornerShape(2.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(if (win) "W" else "L", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                    }
                }

                if (hot.isNotEmpty() && cold.isNotEmpty()) Spacer(Modifier.height(8.dp))

                if (cold.isNotEmpty()) {
                    Text("연패 중", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LolColors.Loss)
                    Spacer(Modifier.height(4.dp))
                    cold.take(5).forEach { (riotId, s) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(LolColors.Loss.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(riotId.split("#").first(), fontSize = 11.sp, color = LolColors.TextPrimary, modifier = Modifier.weight(1f))
                            Text("${s.currentStreak}연패", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LolColors.Loss)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                s.recentForm.takeLast(5).forEach { win ->
                                    Box(
                                        Modifier.size(10.dp)
                                            .background(if (win) LolColors.Win else LolColors.Loss, RoundedCornerShape(2.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(if (win) "W" else "L", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                    }
                }

                if (hot.isEmpty() && cold.isEmpty()) {
                    Text("현재 2연승/2연패 이상인 플레이어 없음", fontSize = 11.sp, color = LolColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun MvpSection(data: MvpRankingResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Warning)
                Text("MVP 랭킹", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))
            if (data == null || data.rankings.isEmpty()) {
                Text("MVP 데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                data.rankings.take(5).forEachIndexed { index, entry ->
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD700)
                        1 -> Color(0xFFC0C0C0)
                        2 -> Color(0xFFCD7F32)
                        else -> LolColors.TextSecondary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (index % 2 == 0) LolColors.BgHover else Color.Transparent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("#${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rankColor, modifier = Modifier.width(28.dp))
                        Text(entry.riotId.split("#").first(), fontSize = 12.sp, color = LolColors.TextPrimary, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${String.format("%.1f", entry.mvpScore)}점", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LolColors.Primary)
                            Text("MVP ${entry.mvpCount}회 / ${entry.games}판", fontSize = 9.sp, color = LolColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
