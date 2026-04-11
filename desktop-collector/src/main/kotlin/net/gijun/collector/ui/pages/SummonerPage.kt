package net.gijun.collector.ui.pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.gijun.collector.api.*
import net.gijun.collector.ui.components.ChampionIcon
import net.gijun.collector.ui.components.Grid16
import net.gijun.collector.ui.components.colSpan
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.winRateColor

private val QUEUE_LABEL = mapOf(0 to "커스텀", 3130 to "5v5 내전", 3270 to "칼바람")

private fun fmt(secs: Int): String = "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"

@Composable
fun SummonerPage() {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<PlayerStats?>(null) }
    var loadingDetail by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var eloHistory by remember { mutableStateOf<EloHistoryResult?>(null) }
    var streak by remember { mutableStateOf<PlayerStreakResult?>(null) }
    var dnaEntry by remember { mutableStateOf<DnaEntry?>(null) }
    var positionPool by remember { mutableStateOf<PositionPoolEntry?>(null) }

    suspend fun selectPlayer(riotId: String) {
        selected = riotId
        loadingDetail = true
        detail = null
        eloHistory = null
        streak = null
        dnaEntry = null
        positionPool = null
        try {
            detail = ApiClient.fetchPlayerStats(riotId) ?: run { searchError = "전적 로드 실패"; null }
            // Fetch additional data in parallel
            scope.launch { eloHistory = ApiClient.fetchEloHistory(riotId) }
            scope.launch { streak = ApiClient.fetchPlayerStreak(riotId) }
            scope.launch {
                val dnaResult = ApiClient.fetchPlaystyleDna()
                dnaEntry = dnaResult?.players?.find { it.riotId == riotId }
            }
            scope.launch {
                val poolResult = ApiClient.fetchPositionPool()
                positionPool = poolResult?.players?.find { it.riotId == riotId }
            }
        } catch (_: Exception) {
            searchError = "전적 로드 실패"
        } finally {
            loadingDetail = false
        }
    }

    suspend fun doSearch() {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return
        searching = true; searchError = null; candidates = emptyList(); selected = null; detail = null
        try {
            val result = ApiClient.fetchStatsList()
            val matched = result.stats.map { it.riotId }.filter { it.split("#")[0].lowercase().contains(q) }
            when {
                matched.isEmpty() -> searchError = "검색 결과 없음"
                matched.size == 1 -> { candidates = matched; selectPlayer(matched[0]) }
                else -> candidates = matched
            }
        } catch (_: Exception) {
            searchError = "검색 실패"
        } finally {
            searching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("소환사 검색", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("닉네임으로 내전 전적 조회", fontSize = 13.sp, color = LolColors.TextSecondary)
        Spacer(Modifier.height(24.dp))

        // 검색
        Card(
            colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LolColors.Border),
        ) {
            Grid16(modifier = Modifier.padding(16.dp), gap = 8.dp) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("닉네임 입력 (태그 제외)", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.colSpan(12).onKeyEvent {
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) { scope.launch { doSearch() }; true } else false
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LolColors.Primary,
                        unfocusedBorderColor = LolColors.Border,
                        cursorColor = LolColors.Primary,
                        focusedTextColor = LolColors.TextPrimary,
                        unfocusedTextColor = LolColors.TextPrimary,
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                )
                Button(
                    onClick = { scope.launch { doSearch() } },
                    enabled = !searching && query.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = LolColors.Primary, contentColor = LolColors.TextInverse),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.colSpan(4),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (searching) "검색 중..." else "검색", fontSize = 11.sp)
                }
            }
            searchError?.let { Text(it, fontSize = 11.sp, color = LolColors.Error, modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)) }
        }

        Spacer(Modifier.height(12.dp))

        // 후보 목록
        if (candidates.size > 1) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("${candidates.size}명 검색됨 — 선택하세요", fontSize = 11.sp, color = LolColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        candidates.forEach { id ->
                            val isSel = selected == id
                            OutlinedButton(
                                onClick = { scope.launch { selectPlayer(id) } },
                                border = BorderStroke(1.dp, if (isSel) LolColors.Primary else LolColors.Border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) LolColors.Primary else LolColors.BgHover,
                                    contentColor = if (isSel) LolColors.TextInverse else LolColors.TextPrimary,
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(id, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 결과
        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
            if (loadingDetail) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Text("전적 조회 중...", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(24.dp))
                }
            }

            detail?.let { d ->
                val winColor = winRateColor(d.winRate)

                // 요약 배너
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Grid16(modifier = Modifier.padding(24.dp), gap = 16.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.colSpan(3)) {
                            Text("${d.winRate.toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = winColor)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${d.wins}W", fontSize = 10.sp, color = LolColors.Win)
                                Text("${d.losses}L", fontSize = 10.sp, color = LolColors.Loss)
                            }
                        }
                        Column(modifier = Modifier.colSpan(5)) {
                            Text("${String.format("%.2f", d.kda)} KDA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
                            Row {
                                Text(String.format("%.1f", d.avgKills), fontSize = 11.sp, color = LolColors.TextSecondary)
                                Text(" / ", fontSize = 11.sp, color = LolColors.TextSecondary)
                                Text(String.format("%.1f", d.avgDeaths), fontSize = 11.sp, color = LolColors.Error)
                                Text(" / ", fontSize = 11.sp, color = LolColors.TextSecondary)
                                Text(String.format("%.1f", d.avgAssists), fontSize = 11.sp, color = LolColors.TextSecondary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.colSpan(8)) {
                            Text(d.riotId.split("#").first(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
                            Text("#${d.riotId.split("#").getOrElse(1) { "" }} · ${d.games}판", fontSize = 11.sp, color = LolColors.TextDisabled)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 스트릭 + 포지션 분포
                Grid16(modifier = Modifier.fillMaxWidth(), gap = 12.dp) {
                    Box(Modifier.colSpan(8)) { StreakSection(streak) }
                    Box(Modifier.colSpan(8)) { PositionPoolSection(positionPool) }
                }
                Spacer(Modifier.height(12.dp))

                // Elo 히스토리
                EloHistorySection(eloHistory)
                Spacer(Modifier.height(12.dp))

                // 플레이스타일 DNA
                PlaystyleDnaSection(dnaEntry)
                Spacer(Modifier.height(12.dp))

                // 챔피언 통계
                if (d.championStats.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LolColors.Border),
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text("챔피언별 통계", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
                            Spacer(Modifier.height(10.dp))
                            d.championStats.forEach { c ->
                                val wrC = winRateColor(c.winRate)
                                val kdaC = when { c.kda >= 5 -> LolColors.Win; c.kda >= 3 -> LolColors.Primary; else -> LolColors.TextPrimary }
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(LolColors.BgHover, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ChampionIcon(c.championId, 26.dp)
                                    Text(c.champion, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("${c.games}판", fontSize = 11.sp, color = LolColors.TextSecondary, modifier = Modifier.width(36.dp))
                                    // 승률 바
                                    Box(Modifier.width(60.dp).height(4.dp).background(LolColors.Border, RoundedCornerShape(2.dp))) {
                                        Box(Modifier.fillMaxHeight().fillMaxWidth(c.winRate.toFloat() / 100f).background(wrC, RoundedCornerShape(2.dp)))
                                    }
                                    Text("${c.winRate.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = wrC, modifier = Modifier.width(28.dp))
                                    Text(String.format("%.2f", c.kda), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = kdaC, modifier = Modifier.width(38.dp))
                                    Text(
                                        "${String.format("%.1f", c.avgKills)}/${String.format("%.1f", c.avgDeaths)}/${String.format("%.1f", c.avgAssists)}",
                                        fontSize = 10.sp, color = LolColors.TextSecondary, modifier = Modifier.width(68.dp),
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // 최근 경기
                if (d.recentMatches.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LolColors.Border),
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text("최근 경기 (${d.recentMatches.size}게임)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
                            Spacer(Modifier.height(10.dp))
                            d.recentMatches.forEach { m ->
                                MatchRow(m, d.riotId)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MatchRow(m: RecentMatch, searchedRiotId: String) {
    var expanded by remember { mutableStateOf(false) }
    val kda = if (m.deaths == 0) "완벽" else String.format("%.2f", (m.kills + m.assists).toDouble() / m.deaths)
    val date = java.time.Instant.ofEpochMilli(m.gameCreation).atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
    val dateStr = "${date.monthValue}/${date.dayOfMonth}"
    val borderColor = if (m.win) LolColors.Win.copy(alpha = 0.25f) else LolColors.Loss.copy(alpha = 0.2f)
    val bgColor = if (m.win) LolColors.Win.copy(alpha = 0.04f) else LolColors.Loss.copy(alpha = 0.04f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (m.win) "승" else "패", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (m.win) LolColors.Win else LolColors.Loss, modifier = Modifier.width(18.dp))
            ChampionIcon(m.championId, 28.dp)
            Column(Modifier.weight(1f)) {
                Row {
                    Text("${m.kills} / ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LolColors.TextPrimary)
                    Text("${m.deaths}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Error)
                    Text(" / ${m.assists}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LolColors.TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("$kda KDA", fontSize = 10.sp, color = LolColors.TextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("CS ${m.cs}", fontSize = 10.sp, color = LolColors.TextSecondary)
                Text("${String.format("%.1f", m.damage / 1000.0)}k딜", fontSize = 10.sp, color = LolColors.TextDisabled)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(52.dp)) {
                Text(dateStr, fontSize = 10.sp, color = LolColors.TextSecondary)
                Text(fmt(m.gameDuration), fontSize = 10.sp, color = LolColors.TextDisabled)
            }
            Text(QUEUE_LABEL[m.queueId] ?: "Q${m.queueId}", fontSize = 10.sp, color = LolColors.TextDisabled, modifier = Modifier.width(36.dp))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = LolColors.TextDisabled,
            )
        }

        if (expanded) {
            MatchExpandedView(m.matchId, searchedRiotId)
        }
    }
}

@Composable
private fun MatchExpandedView(matchId: String, searchedRiotId: String) {
    var detail by remember { mutableStateOf<MatchDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(matchId) {
        try {
            detail = ApiClient.fetchMatchDetail(matchId)
            if (detail == null) error = "매치 정보 로드 실패"
        } catch (_: Exception) {
            error = "매치 정보 로드 실패"
        } finally {
            loading = false
        }
    }

    Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
        when {
            loading -> Text("로딩 중...", fontSize = 11.sp, color = LolColors.TextSecondary)
            error != null -> Text(error!!, fontSize = 11.sp, color = LolColors.Error)
            detail != null -> {
                val d = detail!!
                val blue = d.participants.filter { it.team == "blue" }
                val red = d.participants.filter { it.team == "red" }
                val blueWin = blue.firstOrNull()?.win ?: false

                Column {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(LolColors.Border))
                    Spacer(Modifier.height(8.dp))
                    Grid16(gap = 12.dp) {
                        // 블루팀
                        Column(Modifier.colSpan(8)) {
                            Text(
                                "블루팀 ${if (blueWin) "승" else "패"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (blueWin) LolColors.Win else LolColors.Loss,
                            )
                            Spacer(Modifier.height(4.dp))
                            blue.forEach { p -> TeamRow(p, searchedRiotId) }
                        }
                        // 레드팀
                        Column(Modifier.colSpan(8)) {
                            Text(
                                "레드팀 ${if (!blueWin) "승" else "패"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!blueWin) LolColors.Win else LolColors.Loss,
                            )
                            Spacer(Modifier.height(4.dp))
                            red.forEach { p -> TeamRow(p, searchedRiotId) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamRow(p: MatchParticipant, searchedRiotId: String) {
    val isMe = p.riotId == searchedRiotId
    val bgColor = if (isMe) LolColors.Primary.copy(alpha = 0.08f) else Color.Transparent
    val nameColor = if (isMe) LolColors.Primary else LolColors.TextPrimary
    val kda = if (p.deaths == 0) "완벽" else String.format("%.2f", (p.kills + p.assists).toDouble() / p.deaths)

    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(3.dp)).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ChampionIcon(p.championId, 22.dp)
        Text(
            p.riotId.split("#").first(),
            fontSize = 11.sp,
            color = nameColor,
            fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("${p.kills}/${p.deaths}/${p.assists}", fontSize = 10.sp, color = LolColors.TextSecondary)
        Text(kda, fontSize = 10.sp, color = LolColors.TextDisabled)
        Text("${p.cs}CS", fontSize = 10.sp, color = LolColors.TextDisabled)
        Text("${String.format("%.1f", p.damage / 1000.0)}k딜", fontSize = 10.sp, color = LolColors.TextDisabled)
    }
}

// ── 스트릭 표시 ──────────────────────────────────

@Composable
private fun StreakSection(data: PlayerStreakResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("스트릭", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            if (data == null) {
                Text("데이터 없음", fontSize = 11.sp, color = LolColors.TextSecondary)
            } else {
                // Current streak
                val streakColor = if (data.currentStreakType == "win") LolColors.Win else LolColors.Loss
                val streakLabel = if (data.currentStreakType == "win") "연승" else "연패"
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("현재", fontSize = 11.sp, color = LolColors.TextSecondary)
                    Text("${data.currentStreak}${streakLabel}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = streakColor)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("최장 연승: ${data.longestWinStreak}", fontSize = 10.sp, color = LolColors.Win)
                    Text("최장 연패: ${data.longestLossStreak}", fontSize = 10.sp, color = LolColors.Loss)
                }
                // Recent form dots
                if (data.recentForm.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("최근 전적", fontSize = 10.sp, color = LolColors.TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        data.recentForm.takeLast(10).forEach { win ->
                            Box(
                                Modifier.size(14.dp)
                                    .background(
                                        if (win) LolColors.Win else LolColors.Loss,
                                        RoundedCornerShape(2.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (win) "W" else "L", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 포지션 분포 ──────────────────────────────────

private val POSITION_LABEL = mapOf("TOP" to "탑", "JUNGLE" to "정글", "MIDDLE" to "미드", "BOTTOM" to "원딜", "UTILITY" to "서포터")

@Composable
private fun PositionPoolSection(data: PositionPoolEntry?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("포지션 분포", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            if (data == null || data.positions.isEmpty()) {
                Text("데이터 없음", fontSize = 11.sp, color = LolColors.TextSecondary)
            } else {
                val total = data.positions.values.sum().coerceAtLeast(1)
                val mainLabel = POSITION_LABEL[data.mainPosition.uppercase()] ?: data.mainPosition
                Text("주 포지션: $mainLabel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                Spacer(Modifier.height(8.dp))
                data.positions.entries.sortedByDescending { it.value }.forEach { (pos, count) ->
                    val label = POSITION_LABEL[pos.uppercase()] ?: pos
                    val pct = (count.toDouble() / total * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(label, fontSize = 10.sp, color = LolColors.TextSecondary, modifier = Modifier.width(40.dp))
                        Box(Modifier.weight(1f).height(6.dp).background(LolColors.Border, RoundedCornerShape(3.dp))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).background(LolColors.Primary, RoundedCornerShape(3.dp)))
                        }
                        Text("$pct%", fontSize = 10.sp, color = LolColors.TextPrimary, modifier = Modifier.width(28.dp))
                        Text("${count}판", fontSize = 9.sp, color = LolColors.TextDisabled, modifier = Modifier.width(28.dp))
                    }
                }
            }
        }
    }
}

// ── Elo 히스토리 ─────────────────────────────────

@Composable
private fun EloHistorySection(data: EloHistoryResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Elo 히스토리", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            if (data == null || data.history.isEmpty()) {
                Text("Elo 기록 없음", fontSize = 11.sp, color = LolColors.TextSecondary)
            } else {
                data.history.take(10).forEach { entry ->
                    val deltaColor = if (entry.delta >= 0) LolColors.Win else LolColors.Loss
                    val arrow = if (entry.delta >= 0) "+" else ""
                    val date = try {
                        val instant = java.time.Instant.ofEpochMilli(entry.gameCreation)
                        val zoned = instant.atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
                        "${zoned.monthValue}/${zoned.dayOfMonth}"
                    } catch (_: Exception) { "" }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (entry.win) LolColors.Win.copy(alpha = 0.04f) else LolColors.Loss.copy(alpha = 0.04f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(if (entry.win) "W" else "L", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (entry.win) LolColors.Win else LolColors.Loss, modifier = Modifier.width(14.dp))
                        Text(entry.champion, fontSize = 11.sp, color = LolColors.TextPrimary, modifier = Modifier.weight(1f))
                        Text("${entry.elo.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = LolColors.TextPrimary)
                        Text("$arrow${String.format("%.0f", entry.delta)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = deltaColor)
                        Text(date, fontSize = 9.sp, color = LolColors.TextDisabled)
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }
        }
    }
}

// ── 플레이스타일 DNA ──────────────────────────────

@Composable
private fun PlaystyleDnaSection(data: DnaEntry?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("플레이스타일 DNA", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            if (data == null) {
                Text("DNA 데이터 없음", fontSize = 11.sp, color = LolColors.TextSecondary)
            } else {
                if (data.archetype.isNotEmpty()) {
                    Text("유형: ${data.archetype}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                    Spacer(Modifier.height(8.dp))
                }
                val stats = listOf(
                    "공격성" to data.aggression,
                    "파밍" to data.farming,
                    "시야" to data.vision,
                    "팀파이트" to data.teamfight,
                    "오브젝트" to data.objective,
                )
                stats.forEach { (label, value) ->
                    val barColor = when {
                        value >= 80 -> LolColors.Win
                        value >= 50 -> LolColors.Primary
                        else -> LolColors.TextSecondary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(label, fontSize = 10.sp, color = LolColors.TextSecondary, modifier = Modifier.width(50.dp))
                        Box(Modifier.weight(1f).height(8.dp).background(LolColors.Border, RoundedCornerShape(4.dp))) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth((value / 100.0).toFloat().coerceIn(0f, 1f))
                                    .background(barColor, RoundedCornerShape(4.dp))
                            )
                        }
                        Text("${value.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = barColor, modifier = Modifier.width(24.dp))
                    }
                }
            }
        }
    }
}
