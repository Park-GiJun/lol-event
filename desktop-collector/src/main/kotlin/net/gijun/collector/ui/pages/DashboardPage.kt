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
