package net.gijun.collector.ui.pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Whatshot
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
import net.gijun.collector.api.DamagePlayerEntry
import net.gijun.collector.ui.theme.LolColors

@Composable
fun DamageAnalysisPage() {
    var entries by remember { mutableStateOf<List<DamagePlayerEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    suspend fun load() {
        loading = true
        error = null
        try {
            val result = ApiClient.fetchDamageAnalysis()
            entries = result?.players?.sortedByDescending { it.avgPhysicalDmg + it.avgMagicDmg + it.avgTrueDmg } ?: emptyList()
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
            Icon(Icons.Default.Whatshot, contentDescription = null, tint = LolColors.Error, modifier = Modifier.size(24.dp))
            Column {
                Text("데미지 분석", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
                Text("물리 / 마법 / 트루 딜 비율 · 탱킹량 · 포탑 딜", fontSize = 13.sp, color = LolColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))

        // 새로고침 버튼
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

        // 플레이어 카드 목록
        entries.forEach { entry ->
            DamagePlayerCard(entry)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DamagePlayerCard(entry: DamagePlayerEntry) {
    val totalDmg = entry.avgPhysicalDmg + entry.avgMagicDmg + entry.avgTrueDmg
    val physFrac = if (totalDmg > 0) (entry.avgPhysicalDmg / totalDmg).toFloat() else 0f
    val magicFrac = if (totalDmg > 0) (entry.avgMagicDmg / totalDmg).toFloat() else 0f
    val trueFrac = if (totalDmg > 0) (entry.avgTrueDmg / totalDmg).toFloat() else 0f

    val profileColor = when (entry.damageProfile.uppercase()) {
        "AD" -> LolColors.Primary
        "AP" -> LolColors.Info
        "HYBRID" -> Color(0xFF9B6FDB)
        "TANK" -> LolColors.Win
        else -> LolColors.TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            // 상단: 이름 + 프로필 배지 + 판수
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        entry.riotId.split("#").first(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LolColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entry.damageProfile.isNotEmpty()) {
                        Text(
                            entry.damageProfile,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = profileColor,
                            modifier = Modifier
                                .background(profileColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text("${entry.games}판", fontSize = 11.sp, color = LolColors.TextSecondary)
            }

            Spacer(Modifier.height(10.dp))

            // 딜 비율 바
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DamageBarRow("물리 딜", physFrac, entry.physicalPct, entry.avgPhysicalDmg, LolColors.Primary)
                DamageBarRow("마법 딜", magicFrac, entry.magicPct, entry.avgMagicDmg, LolColors.Info)
                DamageBarRow("트루 딜", trueFrac, entry.truePct, entry.avgTrueDmg, LolColors.PrimaryLight)
            }

            Spacer(Modifier.height(10.dp))

            // 탱킹량 + 포탑 딜
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StatItem("탱킹량", formatK(entry.avgMitigated), LolColors.Win)
                StatItem("포탑 딜", formatK(entry.avgTurretDmg), LolColors.Primary)
                StatItem("총 딜", formatK(totalDmg), LolColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun DamageBarRow(label: String, fraction: Float, pct: Double, avg: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 10.sp, color = LolColors.TextSecondary, modifier = Modifier.width(44.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(LolColors.BgTertiary),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(6.dp))
                    .background(color),
            )
        }
        Text(
            "${pct.toInt()}%",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.width(30.dp),
        )
        Text(formatK(avg), fontSize = 10.sp, color = LolColors.TextSecondary, modifier = Modifier.width(46.dp))
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 9.sp, color = LolColors.TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun formatK(value: Double): String = when {
    value >= 1000 -> String.format("%.1fK", value / 1000)
    else -> value.toInt().toString()
}
