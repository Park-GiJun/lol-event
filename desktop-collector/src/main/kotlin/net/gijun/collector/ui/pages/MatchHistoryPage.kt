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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.gijun.collector.api.*
import net.gijun.collector.ui.components.ChampionIcon
import net.gijun.collector.ui.components.Grid16
import net.gijun.collector.ui.components.colSpan
import net.gijun.collector.ui.theme.LolColors

private val QUEUE_LABEL = mapOf(0 to "커스텀", 3130 to "5v5 내전", 3270 to "칼바람")

private fun fmtDuration(secs: Int): String = "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"

@Composable
fun MatchHistoryPage() {
    var matches by remember { mutableStateOf<MatchListResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val expandedMatches = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        loading = true
        try {
            matches = ApiClient.fetchRecentMatches()
        } catch (_: Exception) {
            error = "경기 데이터를 불러오지 못했습니다"
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("경기 기록", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("최근 내전 경기 결과", fontSize = 13.sp, color = LolColors.TextSecondary)
        Spacer(Modifier.height(24.dp))

        when {
            loading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Text("데이터 로딩 중...", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(24.dp))
                }
            }
            error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Text(error!!, fontSize = 13.sp, color = LolColors.Error, modifier = Modifier.padding(24.dp))
                }
            }
            matches != null -> {
                val matchList = matches!!.matches
                if (matchList.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LolColors.Border),
                    ) {
                        Text("경기 기록이 없습니다", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(24.dp))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        matchList.forEach { match ->
                            MatchCard(
                                match = match,
                                expanded = expandedMatches[match.matchId] == true,
                                onToggle = { expandedMatches[match.matchId] = !(expandedMatches[match.matchId] ?: false) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(match: MatchListEntry, expanded: Boolean, onToggle: () -> Unit) {
    val blue = match.participants.filter { it.team == "blue" }
    val red = match.participants.filter { it.team == "red" }
    val blueWin = blue.firstOrNull()?.win ?: false
    val date = try {
        val instant = java.time.Instant.ofEpochMilli(match.gameCreation)
        val zoned = instant.atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
        "${zoned.year}.${zoned.monthValue.toString().padStart(2, '0')}.${zoned.dayOfMonth.toString().padStart(2, '0')}"
    } catch (_: Exception) { "" }
    val queueLabel = QUEUE_LABEL[match.queueId] ?: "Q${match.queueId}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column {
            // Header row - clickable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Blue team champions (icons)
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp), modifier = Modifier.weight(1f)) {
                    blue.forEach { p ->
                        ChampionIcon(p.championId, 24.dp)
                    }
                }

                // Result badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "블루",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (blueWin) LolColors.Win else LolColors.Loss,
                        )
                        Text("vs", fontSize = 11.sp, color = LolColors.TextDisabled)
                        Text(
                            "레드",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!blueWin) LolColors.Win else LolColors.Loss,
                        )
                    }
                    Text(
                        if (blueWin) "블루 승리" else "레드 승리",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (blueWin) LolColors.Blue else LolColors.Red,
                    )
                }

                // Red team champions (icons)
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp), modifier = Modifier.weight(1f), content = {
                    Box(Modifier.weight(1f)) // push to right
                    red.forEach { p ->
                        ChampionIcon(p.championId, 24.dp)
                    }
                })

                // Meta info
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(80.dp)) {
                    Text(date, fontSize = 10.sp, color = LolColors.TextSecondary)
                    Text(fmtDuration(match.gameDuration), fontSize = 10.sp, color = LolColors.TextDisabled)
                    Text(queueLabel, fontSize = 9.sp, color = LolColors.TextDisabled)
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = LolColors.TextDisabled,
                )
            }

            // Expanded detail
            if (expanded) {
                HorizontalDivider(thickness = 1.dp, color = LolColors.Border)
                MatchDetailExpanded(blue, red, blueWin, match)
            }
        }
    }
}

@Composable
private fun MatchDetailExpanded(
    blue: List<MatchParticipant>,
    red: List<MatchParticipant>,
    blueWin: Boolean,
    match: MatchListEntry,
) {
    // Find MVP and ACE
    val allParticipants = match.participants
    val winners = allParticipants.filter { it.win }
    val losers = allParticipants.filter { !it.win }

    fun mvpScore(p: MatchParticipant): Double {
        val kda = if (p.deaths == 0) (p.kills + p.assists).toDouble() * 1.5 else (p.kills + p.assists).toDouble() / p.deaths
        return kda * 0.4 + p.damage / 1000.0 * 0.3 + p.cs * 0.1 + p.gold / 1000.0 * 0.2
    }

    val mvp = winners.maxByOrNull { mvpScore(it) }?.riotId
    val ace = losers.maxByOrNull { mvpScore(it) }?.riotId

    Column(modifier = Modifier.padding(16.dp)) {
        Grid16(gap = 16.dp) {
            // Blue team
            Column(Modifier.colSpan(8)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(LolColors.Blue, RoundedCornerShape(2.dp)))
                    Text(
                        "블루팀 ${if (blueWin) "승리" else "패배"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (blueWin) LolColors.Win else LolColors.Loss,
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Spacer(Modifier.width(22.dp)) // icon space
                    Text("소환사", fontSize = 9.sp, color = LolColors.TextDisabled, modifier = Modifier.weight(1f))
                    Text("KDA", fontSize = 9.sp, color = LolColors.TextDisabled, modifier = Modifier.width(60.dp))
                    Text("딜량", fontSize = 9.sp, color = LolColors.TextDisabled, modifier = Modifier.width(40.dp))
                    Text("CS", fontSize = 9.sp, color = LolColors.TextDisabled, modifier = Modifier.width(28.dp))
                }
                blue.forEach { p -> PlayerRow(p, isMvp = p.riotId == mvp, isAce = p.riotId == ace) }
            }
            // Red team
            Column(Modifier.colSpan(8)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(LolColors.Red, RoundedCornerShape(2.dp)))
                    Text(
                        "레드팀 ${if (!blueWin) "승리" else "패배"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!blueWin) LolColors.Win else LolColors.Loss,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Spacer(Modifier.width(22.dp))
                    Text("소환사", fontSize = 9.sp, color = LolColors.TextDisabled, modifier = Modifier.weight(1f))
                    Text("KDA", fontSize = 9.sp, color = LolColors.TextDisabled, modifier = Modifier.width(60.dp))
                    Text("딜량", fontSize = 9.sp, color = LolColors.TextDisabled, modifier = Modifier.width(40.dp))
                    Text("CS", fontSize = 9.sp, color = LolColors.TextDisabled, modifier = Modifier.width(28.dp))
                }
                red.forEach { p -> PlayerRow(p, isMvp = p.riotId == mvp, isAce = p.riotId == ace) }
            }
        }
    }
}

@Composable
private fun PlayerRow(p: MatchParticipant, isMvp: Boolean, isAce: Boolean) {
    val kda = if (p.deaths == 0) "Perfect" else String.format("%.2f", (p.kills + p.assists).toDouble() / p.deaths)
    val bgColor = when {
        isMvp -> LolColors.Win.copy(alpha = 0.08f)
        isAce -> LolColors.Warning.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ChampionIcon(p.championId, 22.dp)
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                p.riotId.split("#").first(),
                fontSize = 11.sp,
                color = LolColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isMvp) {
                Text(
                    "MVP",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(LolColors.Win, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            if (isAce) {
                Text(
                    "ACE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(LolColors.Warning, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Text(
            "${p.kills}/${p.deaths}/${p.assists}",
            fontSize = 10.sp,
            color = LolColors.TextSecondary,
            modifier = Modifier.width(60.dp),
        )
        Text(
            "${String.format("%.1f", p.damage / 1000.0)}k",
            fontSize = 10.sp,
            color = LolColors.TextDisabled,
            modifier = Modifier.width(40.dp),
        )
        Text(
            "${p.cs}",
            fontSize = 10.sp,
            color = LolColors.TextDisabled,
            modifier = Modifier.width(28.dp),
        )
    }
}
