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
import net.gijun.collector.api.ApiClient
import net.gijun.collector.api.ChampionStat
import net.gijun.collector.lcu.LcuClient
import net.gijun.collector.lcu.TeamMemberInfo
import net.gijun.collector.ui.components.ChampionIcon
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.winRateColor
import java.awt.Desktop
import java.net.URI
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class PlayerData(
    val summonerName: String,
    val riotId: String,
    val isMe: Boolean,
    val championStats: List<ChampionStat>?,
)

@Composable
fun CustomGamePage() {
    var blueTeam by remember { mutableStateOf<List<PlayerData>>(emptyList()) }
    var redTeam by remember { mutableStateOf<List<PlayerData>>(emptyList()) }
    var phase by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        error = null
        try {
            val result = LcuClient.getCustomMostPicks()
            if (result == null) { error = "LCU 연결 실패"; return }
            phase = result.phase

            suspend fun toPlayerData(m: TeamMemberInfo): PlayerData {
                val stats = try { ApiClient.fetchPlayerStats(m.riotId, "normal")?.championStats?.take(6) } catch (_: Exception) { null }
                return PlayerData(m.summonerName, m.riotId, m.isMe, stats)
            }

            blueTeam = result.blueTeam.map { toPlayerData(it) }
            redTeam = result.redTeam.map { toPlayerData(it) }
            lastUpdated = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TeamColumn("블루팀", LolColors.Blue, blueTeam, Modifier.weight(1f))
                TeamColumn("레드팀", LolColors.Red, redTeam, Modifier.weight(1f))
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
