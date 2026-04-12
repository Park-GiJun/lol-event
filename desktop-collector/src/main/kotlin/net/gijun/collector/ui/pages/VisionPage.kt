package net.gijun.collector.ui.pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.gijun.collector.api.ApiClient
import net.gijun.collector.api.VisionPlayerEntry
import net.gijun.collector.ui.theme.LolColors

@Composable
fun VisionPage() {
    var entries by remember { mutableStateOf<List<VisionPlayerEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    suspend fun load() {
        loading = true
        error = null
        try {
            val result = ApiClient.fetchVisionDominance()
            entries = result?.players?.sortedByDescending { it.avgVisionScore } ?: emptyList()
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
            Icon(Icons.Default.Visibility, contentDescription = null, tint = LolColors.Info, modifier = Modifier.size(24.dp))
            Column {
                Text("시야 분석", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
                Text("시야 점수 랭킹 · 와드 설치/제거 · 제어 와드", fontSize = 13.sp, color = LolColors.TextSecondary)
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

        if (loading && entries.isEmpty()) {
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

        if (entries.isEmpty()) {
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

        // 랭킹 테이블 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LolColors.Border),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("시야 점수 랭킹", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Info)
                Spacer(Modifier.height(4.dp))
                Text("내전 기록 기반 평균 시야 점수", fontSize = 11.sp, color = LolColors.TextSecondary)
                Spacer(Modifier.height(12.dp))

                // 헤더 행
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(22.dp))
                    Text("소환사", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.weight(1f))
                    Text("시야 점수", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(60.dp))
                    Text("와드 설치", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(60.dp))
                    Text("와드 제거", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(60.dp))
                    Text("제어 와드", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(60.dp))
                    Text("판수", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(36.dp))
                }

                entries.forEachIndexed { index, entry ->
                    val rankColor = when (index) {
                        0 -> LolColors.PrimaryLight
                        1 -> Color(0xFFA09B8C)
                        2 -> LolColors.PrimaryDark
                        else -> LolColors.TextPrimary
                    }
                    val visionColor = when {
                        entry.avgVisionScore >= 30 -> LolColors.Win
                        entry.avgVisionScore >= 20 -> LolColors.Primary
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
                        Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rankColor, modifier = Modifier.width(22.dp))
                        Text(
                            entry.riotId.split("#").first(),
                            fontSize = 12.sp,
                            color = LolColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            String.format("%.1f", entry.avgVisionScore),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = visionColor,
                            modifier = Modifier.width(60.dp),
                        )
                        Text(
                            String.format("%.1f", entry.avgWardsPlaced),
                            fontSize = 11.sp,
                            color = LolColors.TextSecondary,
                            modifier = Modifier.width(60.dp),
                        )
                        Text(
                            String.format("%.1f", entry.avgWardsKilled),
                            fontSize = 11.sp,
                            color = LolColors.TextSecondary,
                            modifier = Modifier.width(60.dp),
                        )
                        Text(
                            String.format("%.1f", entry.avgControlWards),
                            fontSize = 11.sp,
                            color = LolColors.Info,
                            modifier = Modifier.width(60.dp),
                        )
                        Text("${entry.games}판", fontSize = 10.sp, color = LolColors.TextSecondary, modifier = Modifier.width(36.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }
        }
    }
}
