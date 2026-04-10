package net.gijun.collector.ui.pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import net.gijun.collector.api.*
import net.gijun.collector.lcu.ChampSelectFull
import net.gijun.collector.lcu.ChampSelectSlot
import net.gijun.collector.lcu.LcuClient
import net.gijun.collector.ui.components.BanRecommendBadge
import net.gijun.collector.ui.components.ChampionIcon
import net.gijun.collector.ui.components.PlayerCard
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.winRateColor

private val positionLabel = mapOf("top" to "탑", "jungle" to "정글", "middle" to "미드", "bottom" to "원딜", "utility" to "서포터", "" to "—")

@Composable
fun ChampSelectPage() {
    var state by remember { mutableStateOf<ChampSelectFull?>(null) }
    var loading by remember { mutableStateOf(false) }
    var lcuError by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("ban") }
    var playerDetails by remember { mutableStateOf<Map<String, PlayerStats>>(emptyMap()) }
    var fetchedRiotIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    suspend fun load() {
        loading = true
        try {
            val data = LcuClient.getChampSelectFull()
            lcuError = false
            state = data
        } catch (_: Exception) {
            lcuError = true
            state = null
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    // 2초 자동 새로고침
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(2_000)
            load()
        }
    }

    // 플레이어 데이터 fetch
    val riotIdKey = state?.let { s ->
        (s.myTeam.map { it.riotId } + s.theirTeam.map { it.riotId }).filter { it.isNotEmpty() }.joinToString(",")
    }
    LaunchedEffect(riotIdKey) {
        if (state == null) return@LaunchedEffect
        val allSlots = state!!.myTeam + state!!.theirTeam
        val riotIds = allSlots.map { it.riotId }.filter { it.isNotEmpty() }
        if (riotIds.isEmpty()) return@LaunchedEffect
        val map = mutableMapOf<String, PlayerStats>()
        riotIds.forEach { riotId ->
            val stats = ApiClient.fetchPlayerStats(riotId)
            if (stats != null) map[riotId] = stats
        }
        playerDetails = map
        fetchedRiotIds = riotIds.toSet()
    }

    val tabs = listOf("ban" to "밴 추천", "pick" to "내 픽 추천", "counter" to "카운터픽")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(scrollState)) {
        Text("챔피언 선택", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("상대 분석 · 밴 추천 · 카운터픽", fontSize = 13.sp, color = LolColors.TextSecondary)
        Spacer(Modifier.height(24.dp))

        // 탭 + 상태바
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LolColors.Border),
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEach { (key, label) ->
                        Button(
                            onClick = { tab = key },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tab == key) LolColors.Primary else LolColors.BgHover,
                                contentColor = if (tab == key) LolColors.TextInverse else LolColors.TextPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            shape = RoundedCornerShape(6.dp),
                        ) { Text(label, fontSize = 11.sp) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state?.let { s ->
                        Text("${s.phase} ${if (s.timer > 0) "${s.timer}s" else ""}", fontSize = 11.sp, color = LolColors.TextSecondary)
                    }
                    IconButton(onClick = { scope.launch { load() } }, enabled = !loading, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = LolColors.TextPrimary)
                    }
                }
            }
            if (lcuError) Text("LoL 클라이언트를 실행해주세요", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp))
            if (!lcuError && state == null && !loading) Text("챔피언 선택 화면이 아닙니다", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp))
        }
        Spacer(Modifier.height(16.dp))

        state?.let { s ->
            // 2열 그리드: 우리팀 / 상대팀
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // 우리팀
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("우리팀", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Info)
                        Spacer(Modifier.height(8.dp))
                        s.myTeam.forEach { slot ->
                            val riotId = slot.riotId.ifEmpty { slot.summonerName.ifEmpty { "Player ${slot.cellId}" } }
                            PlayerCard(
                                riotId = riotId,
                                data = if (slot.riotId.isNotEmpty()) playerDetails[slot.riotId] else null,
                                loading = loading || (slot.riotId.isNotEmpty() && slot.riotId !in fetchedRiotIds),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                // 상대팀
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("상대팀", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Error)
                        Spacer(Modifier.height(8.dp))
                        s.theirTeam.forEach { slot ->
                            val riotId = slot.riotId.ifEmpty { slot.summonerName.ifEmpty { "Player ${slot.cellId}" } }
                            PlayerCard(
                                riotId = riotId,
                                data = if (slot.riotId.isNotEmpty()) playerDetails[slot.riotId] else null,
                                loading = loading || (slot.riotId.isNotEmpty() && slot.riotId !in fetchedRiotIds),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // 분석 탭
            when (tab) {
                "ban" -> BanRecommendSection(s.theirTeam, playerDetails)
                "pick" -> MyPickSection(s.myTeam, s.bans.map { it.championId }.toSet(), playerDetails)
                "counter" -> CounterSection(s.theirTeam)
            }

            // 밴 목록
            if (s.bans.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("밴 목록", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            s.bans.forEach { ban ->
                                val borderColor = if (ban.team == "blue") LolColors.Info.copy(alpha = 0.3f) else LolColors.Error.copy(alpha = 0.3f)
                                val bgColor = if (ban.team == "blue") LolColors.Info.copy(alpha = 0.1f) else LolColors.Error.copy(alpha = 0.1f)
                                Box(
                                    Modifier.background(bgColor, RoundedCornerShape(4.dp))
                                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) { ChampionIcon(ban.championId, 20.dp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BanRecommendSection(enemies: List<ChampSelectSlot>, playerDetails: Map<String, PlayerStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                Text("밴 추천", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))
            val hasRiotIds = enemies.any { it.riotId.isNotEmpty() }
            if (!hasRiotIds) {
                Text("상대팀 소환사 정보를 불러오는 중...", fontSize = 13.sp, color = LolColors.TextSecondary)
            }
            enemies.filter { it.riotId.isNotEmpty() }.forEach { e ->
                val player = playerDetails[e.riotId]
                val top3 = player?.championStats?.take(3) ?: emptyList()
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChampionIcon(e.championId, 24.dp)
                        Text(e.riotId, fontSize = 13.sp, color = LolColors.Error)
                        player?.let { Text("${it.games}판 ${it.winRate.toInt()}%", fontSize = 11.sp, color = LolColors.TextSecondary) }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (top3.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            top3.forEachIndexed { i, c ->
                                BanRecommendBadge(c.champion, c.championId, i == 0, c.winRate, c.games)
                            }
                        }
                    } else {
                        Text("내전 데이터 없음", fontSize = 11.sp, color = LolColors.TextDisabled)
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPickSection(myTeam: List<ChampSelectSlot>, bannedIds: Set<Int>, playerDetails: Map<String, PlayerStats>) {
    val mySlot = myTeam.find { it.isMe }
    val myRiotId = mySlot?.riotId
    val myPosition = mySlot?.assignedPosition?.lowercase() ?: ""
    val myData = myRiotId?.let { playerDetails[it] }
    val topChamps = (myData?.championStats ?: emptyList())
        .filter { it.games >= 2 }
        .sortedByDescending { it.winRate }
        .take(12)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                    Text("내 픽 추천", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                }
                Text("${positionLabel[myPosition] ?: "—"} · 내전 승률순 (2판↑)", fontSize = 11.sp, color = LolColors.TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            when {
                myRiotId == null -> Text("소환사 정보를 불러오는 중...", fontSize = 13.sp, color = LolColors.TextSecondary)
                topChamps.isEmpty() -> Text("내전 데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        topChamps.forEach { c ->
                            val isBanned = c.championId in bannedIds
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.alpha(if (isBanned) 0.3f else 1f),
                            ) {
                                Box {
                                    ChampionIcon(c.championId, 38.dp)
                                    if (isBanned) {
                                        Text("X", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LolColors.Error, modifier = Modifier.align(Alignment.Center))
                                    }
                                }
                                Text("${c.winRate.toInt()}%", fontSize = 10.sp, color = winRateColor(c.winRate))
                                Text("${c.games}판", fontSize = 9.sp, color = LolColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterSection(enemies: List<ChampSelectSlot>) {
    var results by remember { mutableStateOf<Map<Int, MatchupResult>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }

    val champIds = enemies.map { it.championId }.filter { it > 0 }
    LaunchedEffect(champIds.joinToString(",")) {
        if (champIds.isEmpty()) return@LaunchedEffect
        loading = true
        val map = mutableMapOf<Int, MatchupResult>()
        try {
            val httpClient = HttpClient(OkHttp)
            val champText: String = httpClient.get("https://ddragon.leagueoflegends.com/cdn/14.24.1/data/ko_KR/champion.json").body()
            val json = Json { ignoreUnknownKeys = true }
            val rootObj = json.parseToJsonElement(champText).jsonObject
            val dataObj = rootObj["data"]!!.jsonObject
            val idToName = mutableMapOf<Int, String>()
            dataObj.values.forEach { champ ->
                val obj = champ.jsonObject
                idToName[obj["key"]!!.jsonPrimitive.content.toInt()] = obj["id"]!!.jsonPrimitive.content
            }
            httpClient.close()

            enemies.filter { it.championId > 0 }.forEach { e ->
                val champName = idToName[e.championId] ?: return@forEach
                var result = ApiClient.fetchMatchup(champName, "normal", samePosition = true)
                if (result == null || result.matchups.isEmpty()) {
                    result = ApiClient.fetchMatchup(champName, "normal", samePosition = false)
                }
                result?.let { map[e.championId] = it }
            }
        } catch (_: Exception) {}
        results = map
        loading = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("카운터픽 추천", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                Text(if (loading) "분석 중..." else "같은 라인 기준", fontSize = 11.sp, color = LolColors.TextSecondary)
            }
            Spacer(Modifier.height(12.dp))

            val hasData = enemies.any { it.championId > 0 }
            if (!hasData) {
                Text("상대팀 챔피언 선택을 기다리는 중...", fontSize = 13.sp, color = LolColors.TextSecondary)
            }
            enemies.filter { it.championId > 0 }.forEach { e ->
                val result = results[e.championId]
                val top3 = result?.matchups?.take(3) ?: emptyList()
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChampionIcon(e.championId, 24.dp)
                        Text(e.riotId.ifEmpty { e.summonerName.ifEmpty { "상대" } }, fontSize = 13.sp, color = LolColors.Error)
                        Text(positionLabel[e.assignedPosition.lowercase()] ?: "—", fontSize = 11.sp, color = LolColors.TextSecondary)
                        Spacer(Modifier.weight(1f))
                        Text("vs 카운터", fontSize = 11.sp, color = LolColors.TextSecondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    if (top3.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            top3.forEach { m ->
                                Row(
                                    modifier = Modifier
                                        .background(LolColors.BgHover, RoundedCornerShape(4.dp))
                                        .border(1.dp, LolColors.Border, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    ChampionIcon(m.opponentId, 22.dp)
                                    Column {
                                        Text(m.opponent, fontSize = 11.sp, color = LolColors.TextPrimary)
                                        Text("${m.winRate.toInt()}% (${m.games}판)", fontSize = 10.sp, color = winRateColor(m.winRate))
                                    }
                                }
                            }
                        }
                    } else if (!loading) {
                        Text("데이터 없음", fontSize = 11.sp, color = LolColors.TextDisabled)
                    }
                }
            }
        }
    }
}
