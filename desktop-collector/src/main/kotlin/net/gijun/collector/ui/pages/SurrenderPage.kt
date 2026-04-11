package net.gijun.collector.ui.pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.gijun.collector.api.ApiClient
import net.gijun.collector.api.SurrenderAnalysisResult
import net.gijun.collector.api.SurrenderPlayerEntry
import net.gijun.collector.ui.theme.LolColors

@Composable
fun SurrenderPage() {
    var result by remember { mutableStateOf<SurrenderAnalysisResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    suspend fun load() {
        loading = true
        error = null
        try {
            result = ApiClient.fetchSurrenderAnalysis()
        } catch (_: Exception) {
            error = "데이터 로드 실패"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(scrollState)) {
        // 헤더
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = LolColors.Error, modifier = Modifier.size(24.dp))
            Column {
                Text("서렌더 분석", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
                Text("서렌더율 · 조기 서렌더 · 유발왕 / 안포기왕", fontSize = 13.sp, color = LolColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { scope.launch { load() } },
                enabled = !loading,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = LolColors.TextPrimary)
                Spacer(Modifier.width(4.dp))
                Text(if (loading) "로딩 중..." else "새로고침", fontSize = 11.sp, color = LolColors.TextPrimary)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (loading && result == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LolColors.Primary, modifier = Modifier.size(28.dp))
                }
            }
            return@Column
        }

        error?.let {
            Text(it, color = LolColors.Error, fontSize = 13.sp)
            return@Column
        }

        val data = result
        if (data == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LolColors.Border),
            ) {
                Text("데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(24.dp))
            }
            return@Column
        }

        // 전체 서렌더율 요약 카드
        SurrenderSummaryCard(data)
        Spacer(Modifier.height(16.dp))

        // 플레이어별 서렌더 투표 현황
        if (data.players.isNotEmpty()) {
            SurrenderPlayerListCard(data.players)
        }
    }
}

@Composable
private fun SurrenderSummaryCard(data: SurrenderAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("전체 서렌더 통계", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Error)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                SurrenderStatBox(
                    label = "전체 서렌더율",
                    value = "${data.surrenderRate.toInt()}%",
                    color = if (data.surrenderRate >= 50) LolColors.Error else LolColors.Win,
                    modifier = Modifier.weight(1f),
                )
                SurrenderStatBox(
                    label = "조기 서렌더율",
                    value = "${data.earlySurrenderRate.toInt()}%",
                    color = if (data.earlySurrenderRate >= 30) LolColors.Warning else LolColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(14.dp))

            // 전체 서렌더율 바
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("서렌더", fontSize = 10.sp, color = LolColors.Error)
                    Text("끝까지 싸움", fontSize = 10.sp, color = LolColors.Win)
                }
                val surrenderFrac = (data.surrenderRate / 100.0).toFloat().coerceIn(0f, 1f)
                Row(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                    if (surrenderFrac > 0f) {
                        Box(
                            Modifier
                                .weight(surrenderFrac)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
                                .background(LolColors.Error),
                        )
                    }
                    if (surrenderFrac < 1f) {
                        Box(
                            Modifier
                                .weight(1f - surrenderFrac)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
                                .background(LolColors.Win),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurrenderStatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(LolColors.BgTertiary, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 10.sp, color = LolColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun SurrenderPlayerListCard(players: List<SurrenderPlayerEntry>) {
    val sorted = players.sortedByDescending { it.surrenderRate }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("플레이어별 서렌더 투표 현황", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            Spacer(Modifier.height(4.dp))
            Text("유발왕 = 가장 많이 서렌더에 투표  ·  안포기왕 = 가장 적게 투표", fontSize = 11.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(12.dp))

            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("소환사", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.weight(1f))
                Text("투표율", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(50.dp))
                Text("투표/기회", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(60.dp))
                Text("태그", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(60.dp))
            }

            sorted.forEach { entry ->
                val surrenderColor = when {
                    entry.surrenderRate >= 70 -> LolColors.Error
                    entry.surrenderRate <= 20 -> LolColors.Win
                    else -> LolColors.TextSecondary
                }
                val labelColor = when (entry.label.lowercase()) {
                    "유발왕" -> LolColors.Error
                    "안포기왕" -> LolColors.Win
                    else -> LolColors.TextSecondary
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LolColors.BgHover, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        entry.riotId.split("#").first(),
                        fontSize = 12.sp,
                        color = LolColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${entry.surrenderRate.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = surrenderColor,
                        modifier = Modifier.width(50.dp),
                    )
                    Text(
                        "${entry.surrenderVotes}/${entry.totalVotes}",
                        fontSize = 11.sp,
                        color = LolColors.TextSecondary,
                        modifier = Modifier.width(60.dp),
                    )
                    if (entry.label.isNotEmpty()) {
                        Text(
                            entry.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = labelColor,
                            modifier = Modifier
                                .background(labelColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .width(60.dp),
                            maxLines = 1,
                        )
                    } else {
                        Spacer(Modifier.width(60.dp))
                    }
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}
