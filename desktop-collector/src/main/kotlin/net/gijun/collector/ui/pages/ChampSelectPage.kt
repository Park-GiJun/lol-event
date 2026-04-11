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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
import net.gijun.collector.lcu.LobbyCache
import net.gijun.collector.ui.components.BanRecommendBadge
import net.gijun.collector.ui.components.ChampionIcon
import net.gijun.collector.ui.components.Grid16
import net.gijun.collector.ui.components.PlayerCard
import net.gijun.collector.ui.components.colSpan
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.winRateColor
import kotlin.math.pow
import kotlin.math.roundToInt

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

    // Lobby cache state
    var lobbyCacheActive by remember { mutableStateOf(false) }
    var cachedEnemyRiotIds by remember { mutableStateOf<List<String>>(emptyList()) }

    // 플레이어 데이터 fetch (with lobby cache fallback)
    val riotIdKey = state?.let { s ->
        (s.myTeam.map { it.riotId } + s.theirTeam.map { it.riotId }).filter { it.isNotEmpty() }.joinToString(",")
    }
    LaunchedEffect(riotIdKey, state) {
        if (state == null) return@LaunchedEffect
        val allSlots = state!!.myTeam + state!!.theirTeam
        val riotIds = allSlots.map { it.riotId }.filter { it.isNotEmpty() }.toMutableSet()
        val map = mutableMapOf<String, PlayerStats>()

        // Check if enemy team has empty riotIds and lobby cache is valid
        val enemiesEmpty = state!!.theirTeam.all { it.riotId.isEmpty() } || state!!.theirTeam.count { it.riotId.isNotEmpty() } == 0
        if (enemiesEmpty && LobbyCache.isValid()) {
            lobbyCacheActive = true
            val iAmBlue = LobbyCache.iAmBlue()
            val cachedEnemies = LobbyCache.getEnemyTeam(iAmBlue)
            cachedEnemyRiotIds = cachedEnemies.map { it.riotId }
            // Use cached PlayerStats for enemies
            cachedEnemies.forEach { cached ->
                val cachedStats = LobbyCache.playerStats[cached.riotId]
                if (cachedStats != null) {
                    map[cached.riotId] = cachedStats
                } else if (cached.riotId.isNotEmpty()) {
                    riotIds.add(cached.riotId)
                }
            }
        } else {
            lobbyCacheActive = false
            cachedEnemyRiotIds = emptyList()
        }

        // Fetch stats for all riotIds not already in map
        riotIds.filter { it !in map }.forEach { riotId ->
            val stats = ApiClient.fetchPlayerStats(riotId)
            if (stats != null) map[riotId] = stats
        }
        playerDetails = map
        fetchedRiotIds = riotIds
    }

    // 내전 데이터 fetch
    var duoSynergy by remember { mutableStateOf<DuoSynergyResult?>(null) }
    var rivalMatchup by remember { mutableStateOf<RivalMatchupResult?>(null) }
    var banAnalysis by remember { mutableStateOf<BanAnalysisResult?>(null) }
    var championTier by remember { mutableStateOf<ChampionTierResult?>(null) }

    LaunchedEffect(Unit) {
        launch { duoSynergy = ApiClient.fetchDuoSynergy() }
        launch { rivalMatchup = ApiClient.fetchRivalMatchup() }
        launch { banAnalysis = ApiClient.fetchBanAnalysis() }
        launch { championTier = ApiClient.fetchChampionTier() }
    }

    val tabs = listOf("ban" to "밴 추천", "pick" to "내 픽 추천", "counter" to "카운터픽", "tier" to "챔피언 티어")

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
            if (lobbyCacheActive) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(8.dp).background(LolColors.Win, RoundedCornerShape(50)))
                    Text("로비 캐시 활성", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Win)
                    Text("— 상대팀 ${cachedEnemyRiotIds.size}명 캐시됨", fontSize = 11.sp, color = LolColors.TextSecondary)
                }
            }
            if (lcuError) Text("LoL 클라이언트를 실행해주세요", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp))
            if (!lcuError && state == null && !loading) Text("챔피언 선택 화면이 아닙니다", fontSize = 13.sp, color = LolColors.TextSecondary, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp))
        }
        Spacer(Modifier.height(16.dp))

        state?.let { s ->
            // 2열 그리드: 우리팀 / 상대팀
            Grid16(modifier = Modifier.fillMaxWidth(), gap = 16.dp) {
                // 우리팀
                Card(
                    modifier = Modifier.colSpan(8),
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
                    modifier = Modifier.colSpan(8),
                    colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LolColors.Border),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("상대팀", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Error)
                            if (lobbyCacheActive) {
                                Text(
                                    "캐시",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(LolColors.Win, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (lobbyCacheActive && cachedEnemyRiotIds.isNotEmpty()) {
                            // Show cached enemy players
                            cachedEnemyRiotIds.forEach { cachedRiotId ->
                                val stats = playerDetails[cachedRiotId]
                                PlayerCard(
                                    riotId = cachedRiotId,
                                    data = stats,
                                    loading = loading || cachedRiotId !in playerDetails,
                                )
                                // Show champion warning from cached stats
                                stats?.championStats?.firstOrNull()?.let { topChamp ->
                                    if (topChamp.games >= 3) {
                                        Text(
                                            "이 플레이어는 ${topChamp.champion}을 ${topChamp.games}판 ${topChamp.winRate.toInt()}%승률로 플레이합니다",
                                            fontSize = 10.sp,
                                            color = LolColors.Warning,
                                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        } else {
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
            }
            Spacer(Modifier.height(16.dp))

            // 분석 탭
            when (tab) {
                "ban" -> {
                    BanRecommendSection(s.theirTeam, playerDetails, lobbyCacheActive, cachedEnemyRiotIds)
                    // 내전 밴 분석 강화
                    banAnalysis?.let { ba ->
                        if (ba.bans.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            InternalBanAnalysisSection(ba)
                        }
                    }
                }
                "pick" -> MyPickSection(s.myTeam, s.bans.map { it.championId }.toSet(), playerDetails)
                "counter" -> CounterSection(s.theirTeam)
                "tier" -> ChampionTierSection(championTier)
            }

            // 듀오 시너지 (우리팀 기준)
            duoSynergy?.let { duo ->
                val myRiotIds = s.myTeam.mapNotNull { it.riotId.ifEmpty { null } }.toSet()
                val relevantDuos = duo.duos.filter { it.player1 in myRiotIds && it.player2 in myRiotIds }
                if (relevantDuos.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    DuoSynergySection(relevantDuos)
                }
            }

            // 라이벌 경고 (우리팀 vs 상대팀) - with cache fallback
            val rivalSource = rivalMatchup
            val myRiotIdsForRival = s.myTeam.mapNotNull { it.riotId.ifEmpty { null } }.toSet()
            val theirRiotIdsForRival = if (lobbyCacheActive) cachedEnemyRiotIds.toSet()
                else s.theirTeam.mapNotNull { it.riotId.ifEmpty { null } }.toSet()

            // Try API data first, then cache
            val relevantRivals = if (rivalSource != null) {
                rivalSource.rivals.filter {
                    (it.player1 in myRiotIdsForRival && it.player2 in theirRiotIdsForRival) ||
                    (it.player2 in myRiotIdsForRival && it.player1 in theirRiotIdsForRival)
                }
            } else if (lobbyCacheActive) {
                LobbyCache.rivalMatchups.filter {
                    (it.player1 in myRiotIdsForRival && it.player2 in theirRiotIdsForRival) ||
                    (it.player2 in myRiotIdsForRival && it.player1 in theirRiotIdsForRival)
                }
            } else emptyList()

            if (relevantRivals.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                RivalMatchupSection(relevantRivals)
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

            // 팀 전력 분석
            Spacer(Modifier.height(16.dp))
            TeamStrengthSection(s.myTeam, s.theirTeam, playerDetails, lobbyCacheActive, cachedEnemyRiotIds)

            // 룬 추천 & 자동 적용
            val mySlot = s.myTeam.find { it.isMe }
            if (mySlot != null && mySlot.championId > 0) {
                Spacer(Modifier.height(16.dp))
                RuneRecommendSection(mySlot, playerDetails[mySlot.riotId])
            }
        }
    }
}

@Composable
private fun BanRecommendSection(
    enemies: List<ChampSelectSlot>,
    playerDetails: Map<String, PlayerStats>,
    lobbyCacheActive: Boolean = false,
    cachedEnemyRiotIds: List<String> = emptyList(),
) {
    // Determine which riotIds to show ban recommendations for
    val banTargets: List<Pair<String, Int>> = if (lobbyCacheActive && cachedEnemyRiotIds.isNotEmpty()) {
        // Use cached enemy riotIds with championId=0 (no champ pick info from cache)
        cachedEnemyRiotIds.map { it to 0 }
    } else {
        enemies.filter { it.riotId.isNotEmpty() }.map { it.riotId to it.championId }
    }

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
                if (lobbyCacheActive) {
                    Text(
                        "캐시 기반",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(LolColors.Win, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (banTargets.isEmpty()) {
                Text("상대팀 소환사 정보를 불러오는 중...", fontSize = 13.sp, color = LolColors.TextSecondary)
            }
            banTargets.forEach { (riotId, champId) ->
                val player = playerDetails[riotId]
                val top3 = player?.championStats?.filter { it.winRate >= 50.0 || it.games >= 3 }?.take(3) ?: player?.championStats?.take(3) ?: emptyList()
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (champId > 0) ChampionIcon(champId, 24.dp)
                        Text(riotId, fontSize = 13.sp, color = LolColors.Error)
                        player?.let { Text("${it.games}판 ${it.winRate.toInt()}%", fontSize = 11.sp, color = LolColors.TextSecondary) }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (top3.isNotEmpty()) {
                        Grid16(gap = 8.dp) {
                            top3.forEachIndexed { i, c ->
                                Box(Modifier.colSpan(4)) {
                                    BanRecommendBadge(c.champion, c.championId, i == 0, c.winRate, c.games)
                                }
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

// ── Team Strength Analysis ─────────────────────────

private fun calculateWinProbability(myElo: Double, theirElo: Double): Double {
    val eloDiff = myElo - theirElo
    return 1.0 / (1.0 + 10.0.pow(-eloDiff / 400.0))
}

@Composable
private fun TeamStrengthSection(
    myTeam: List<ChampSelectSlot>,
    theirTeam: List<ChampSelectSlot>,
    playerDetails: Map<String, PlayerStats>,
    lobbyCacheActive: Boolean = false,
    cachedEnemyRiotIds: List<String> = emptyList(),
) {
    val myElos = myTeam.mapNotNull { slot ->
        if (slot.riotId.isEmpty()) return@mapNotNull null
        playerDetails[slot.riotId]?.elo?.takeIf { it.isFinite() }
    }
    // Use cached enemy riotIds for elo when cache is active
    val theirElos = if (lobbyCacheActive && cachedEnemyRiotIds.isNotEmpty()) {
        cachedEnemyRiotIds.mapNotNull { riotId ->
            playerDetails[riotId]?.elo?.takeIf { it.isFinite() }
        }
    } else {
        theirTeam.mapNotNull { slot ->
            if (slot.riotId.isEmpty()) return@mapNotNull null
            playerDetails[slot.riotId]?.elo?.takeIf { it.isFinite() }
        }
    }

    val hasData = myElos.isNotEmpty() || theirElos.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                Text("팀 전력 분석", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))

            if (!hasData) {
                Text("Elo 데이터를 불러오는 중...", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                val myAvgElo = if (myElos.isNotEmpty()) myElos.average() else 1000.0
                val theirAvgElo = if (theirElos.isNotEmpty()) theirElos.average() else 1000.0
                val winProb = calculateWinProbability(myAvgElo, theirAvgElo)
                val winPercent = (winProb * 100).roundToInt()
                val eloDiff = (myAvgElo - theirAvgElo).roundToInt()

                // Elo comparison row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // My team
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("우리팀", fontSize = 11.sp, color = LolColors.Info)
                        Text("${myAvgElo.roundToInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = LolColors.Info)
                        Text("평균 Elo (${myElos.size}명)", fontSize = 10.sp, color = LolColors.TextSecondary)
                    }

                    // Win probability
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val probColor = when {
                            winPercent >= 60 -> LolColors.Win
                            winPercent >= 45 -> LolColors.Primary
                            else -> LolColors.Loss
                        }
                        Text("승률 예측", fontSize = 10.sp, color = LolColors.TextSecondary)
                        Text("$winPercent%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = probColor)
                        val diffLabel = if (eloDiff >= 0) "+$eloDiff" else "$eloDiff"
                        Text("Elo 차이: $diffLabel", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = LolColors.TextSecondary)
                    }

                    // Their team
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("상대팀", fontSize = 11.sp, color = LolColors.Error)
                        Text("${theirAvgElo.roundToInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = LolColors.Error)
                        Text("평균 Elo (${theirElos.size}명)", fontSize = 10.sp, color = LolColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Visual bar
                val myFraction = (winProb).toFloat().coerceIn(0.05f, 0.95f)
                Row(
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                ) {
                    Box(
                        Modifier.weight(myFraction)
                            .fillMaxHeight()
                            .background(LolColors.Info, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    )
                    Box(
                        Modifier.weight(1f - myFraction)
                            .fillMaxHeight()
                            .background(LolColors.Error, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    )
                }

                // Per-player Elo breakdown
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        myTeam.forEach { slot ->
                            if (slot.riotId.isEmpty()) return@forEach
                            val elo = playerDetails[slot.riotId]?.elo?.takeIf { it.isFinite() }?.roundToInt()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    slot.riotId.split("#").first(),
                                    fontSize = 10.sp,
                                    color = if (slot.isMe) LolColors.Primary else LolColors.TextSecondary,
                                    fontWeight = if (slot.isMe) FontWeight.Bold else FontWeight.Normal,
                                )
                                Text(
                                    if (elo != null) "$elo" else "—",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        elo == null -> LolColors.TextDisabled
                                        elo >= 1200 -> LolColors.Win
                                        elo >= 1000 -> LolColors.Primary
                                        else -> LolColors.Loss
                                    },
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        val theirRiotIds = if (lobbyCacheActive && cachedEnemyRiotIds.isNotEmpty()) {
                            cachedEnemyRiotIds
                        } else {
                            theirTeam.map { it.riotId }.filter { it.isNotEmpty() }
                        }
                        theirRiotIds.forEach { riotId ->
                            val elo = playerDetails[riotId]?.elo?.takeIf { it.isFinite() }?.roundToInt()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    riotId.split("#").first(),
                                    fontSize = 10.sp,
                                    color = LolColors.TextSecondary,
                                )
                                Text(
                                    if (elo != null) "$elo" else "—",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        elo == null -> LolColors.TextDisabled
                                        elo >= 1200 -> LolColors.Win
                                        elo >= 1000 -> LolColors.Primary
                                        else -> LolColors.Loss
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Rune Recommendation & Auto-Apply ───────────────

private val RUNE_STYLE_NAMES = mapOf(
    8000 to "정밀", 8100 to "지배", 8200 to "마법", 8300 to "영감", 8400 to "결의",
)

@Composable
private fun RuneRecommendSection(mySlot: ChampSelectSlot, myStats: PlayerStats?) {
    val scope = rememberCoroutineScope()
    var runeApplyStatus by remember { mutableStateOf("") }
    var applying by remember { mutableStateOf(false) }

    // Find the most-played champion stat matching current locked champion
    val champStat = myStats?.championStats?.find { it.championId == mySlot.championId }

    // Fetch rune data from match history for this champion
    var runeData by remember { mutableStateOf<RuneDataForChamp?>(null) }
    var loadingRunes by remember { mutableStateOf(false) }

    LaunchedEffect(mySlot.championId) {
        if (mySlot.championId <= 0) return@LaunchedEffect
        loadingRunes = true
        try {
            runeData = fetchRuneDataForChampion(mySlot.championId)
        } catch (_: Exception) {}
        loadingRunes = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                Text("룬 추천", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(12.dp))

            if (loadingRunes) {
                Text("룬 데이터 분석 중...", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else if (runeData == null || runeData!!.primaryStyleId == 0) {
                Text("이 챔피언의 룬 데이터가 없습니다", fontSize = 13.sp, color = LolColors.TextSecondary)
                if (champStat != null) {
                    Text("${champStat.champion} ${champStat.games}판 플레이 기록 있음", fontSize = 11.sp, color = LolColors.TextDisabled)
                }
            } else {
                val rd = runeData!!
                // Display rune info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                RUNE_STYLE_NAMES[rd.primaryStyleId] ?: "주 ${rd.primaryStyleId}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LolColors.Primary,
                            )
                            Text("+", fontSize = 13.sp, color = LolColors.TextSecondary)
                            Text(
                                RUNE_STYLE_NAMES[rd.subStyleId] ?: "부 ${rd.subStyleId}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LolColors.TextSecondary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "룬 ID: ${rd.perkIds.joinToString(", ")}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LolColors.TextDisabled,
                        )
                        Text(
                            "${rd.sampleSize}판 기반 (승률 ${rd.winRate.roundToInt()}%)",
                            fontSize = 11.sp,
                            color = winRateColor(rd.winRate),
                        )
                    }

                    // Auto-apply button
                    Button(
                        onClick = {
                            applying = true
                            runeApplyStatus = ""
                            scope.launch {
                                try {
                                    val success = LcuClient.applyRunePage(
                                        name = "Auto: ${champStat?.champion ?: "Champ"}",
                                        primaryStyleId = rd.primaryStyleId,
                                        subStyleId = rd.subStyleId,
                                        perkIds = rd.perkIds,
                                    )
                                    runeApplyStatus = if (success) "룬 적용 완료" else "룬 적용 실패"
                                } catch (e: Exception) {
                                    runeApplyStatus = "오류: ${e.message}"
                                } finally {
                                    applying = false
                                }
                            }
                        },
                        enabled = !applying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LolColors.Primary,
                            contentColor = LolColors.TextInverse,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (applying) "적용 중..." else "룬 자동 적용", fontSize = 11.sp)
                    }
                }

                if (runeApplyStatus.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    val statusColor = if (runeApplyStatus.contains("완료")) LolColors.Win else LolColors.Error
                    Text(runeApplyStatus, fontSize = 11.sp, color = statusColor)
                }
            }
        }
    }
}

private data class RuneDataForChamp(
    val primaryStyleId: Int,
    val subStyleId: Int,
    val perkIds: List<Int>,
    val sampleSize: Int,
    val winRate: Double,
)

/**
 * Fetch rune data for a given champion from match history via LCU.
 * Looks at recent matches where this champion was played and extracts the most common rune setup.
 */
private suspend fun fetchRuneDataForChampion(championId: Int): RuneDataForChamp? {
    val lockfilePath = LcuClient.findLockfile() ?: return null
    val creds = LcuClient.parseLockfile(lockfilePath)

    data class RuneSetup(val primary: Int, val sub: Int, val perks: List<Int>, val win: Boolean)
    val setups = mutableListOf<RuneSetup>()

    try {
        // Scan first 100 matches for this champion
        for (begIndex in 0 until 100 step 20) {
            val endpoint = "/lol-match-history/v1/products/lol/current-summoner/matches?begIndex=$begIndex&endIndex=${begIndex + 19}"
            val data = LcuClient.lcuGet(creds.port, creds.password, endpoint).jsonObject
            val games = data["games"]?.jsonObject?.get("games")?.jsonArray ?: break

            for (game in games) {
                val g = game.jsonObject
                val participants = g["participants"]?.jsonArray ?: continue
                for (p in participants) {
                    val po = p.jsonObject
                    if (po["championId"]?.jsonPrimitive?.intOrNull != championId) continue
                    val stats = po["stats"]?.jsonObject ?: continue
                    val primary = stats["perkPrimaryStyle"]?.jsonPrimitive?.intOrNull ?: continue
                    val sub = stats["perkSubStyle"]?.jsonPrimitive?.intOrNull ?: continue
                    val perks = (0..5).mapNotNull { stats["perk$it"]?.jsonPrimitive?.intOrNull }
                    val win = stats["win"]?.jsonPrimitive?.booleanOrNull ?: false
                    if (perks.size >= 6 && primary > 0) {
                        setups.add(RuneSetup(primary, sub, perks, win))
                    }
                }
            }
            if (games.size < 20) break
        }
    } catch (_: Exception) {}

    if (setups.isEmpty()) return null

    // Find most common setup by (primary, sub, keystone=perk0)
    val grouped = setups.groupBy { Triple(it.primary, it.sub, it.perks.firstOrNull() ?: 0) }
    val best = grouped.maxByOrNull { it.value.size } ?: return null
    val representative = best.value.first()
    val wins = best.value.count { it.win }

    return RuneDataForChamp(
        primaryStyleId = representative.primary,
        subStyleId = representative.sub,
        perkIds = representative.perks,
        sampleSize = best.value.size,
        winRate = if (best.value.isNotEmpty()) wins.toDouble() / best.value.size * 100 else 0.0,
    )
}

// ── Duo Synergy Section ──────────────────────────

@Composable
private fun DuoSynergySection(duos: List<DuoSynergy>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Win)
                Text("듀오 시너지 (우리팀)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Win)
            }
            Spacer(Modifier.height(8.dp))
            Text("내전 기록에서 함께 승률이 높은 조합", fontSize = 11.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
            duos.sortedByDescending { it.winRate }.take(5).forEach { duo ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(LolColors.BgHover, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(duo.player1.split("#").first(), fontSize = 11.sp, color = LolColors.TextPrimary)
                    Text("+", fontSize = 11.sp, color = LolColors.TextSecondary)
                    Text(duo.player2.split("#").first(), fontSize = 11.sp, color = LolColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text("${duo.winRate.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = winRateColor(duo.winRate))
                    Text("${duo.games}판", fontSize = 10.sp, color = LolColors.TextSecondary)
                    Text("KDA ${String.format("%.1f", duo.combinedKda)}", fontSize = 10.sp, color = LolColors.TextDisabled)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── Rival Matchup Section ────────────────────────

@Composable
private fun RivalMatchupSection(rivals: List<RivalEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Error)
                Text("라이벌 경고", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Error)
            }
            Spacer(Modifier.height(8.dp))
            Text("내전에서 자주 맞붙은 상대", fontSize = 11.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
            rivals.sortedByDescending { it.games }.take(5).forEach { rival ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(LolColors.BgHover, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(rival.player1.split("#").first(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Info)
                    Text("vs", fontSize = 10.sp, color = LolColors.TextSecondary)
                    Text(rival.player2.split("#").first(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Error, modifier = Modifier.weight(1f))
                    Text("${rival.player1Wins}W", fontSize = 11.sp, color = LolColors.Info)
                    Text("-", fontSize = 10.sp, color = LolColors.TextSecondary)
                    Text("${rival.player2Wins}W", fontSize = 11.sp, color = LolColors.Error)
                    Text("(${rival.games}판)", fontSize = 10.sp, color = LolColors.TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── Internal Ban Analysis Section ────────────────

@Composable
private fun InternalBanAnalysisSection(data: BanAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Warning)
                Text("내전 밴 분석", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Warning)
            }
            Spacer(Modifier.height(8.dp))
            Text("내전에서 자주 밴되는 챔피언", fontSize = 11.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                data.bans.take(8).forEach { ban ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(LolColors.BgHover, RoundedCornerShape(6.dp))
                            .border(1.dp, LolColors.Border, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                    ) {
                        ChampionIcon(ban.championId, 32.dp)
                        Spacer(Modifier.height(4.dp))
                        Text("${ban.banRate.toInt()}%밴", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.Error)
                        Text("${ban.banCount}회", fontSize = 9.sp, color = LolColors.TextSecondary)
                        if (ban.winRateWhenNotBanned > 0) {
                            Text("비밴시 ${ban.winRateWhenNotBanned.toInt()}%", fontSize = 9.sp, color = winRateColor(ban.winRateWhenNotBanned))
                        }
                    }
                }
            }
        }
    }
}

// ── Champion Tier Section ────────────────────────

@Composable
private fun ChampionTierSection(data: ChampionTierResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LolColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = LolColors.Primary)
                Text("내전 챔피언 티어", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
            }
            Spacer(Modifier.height(4.dp))
            Text("내전 데이터 기반 챔피언 메타 (3판 이상)", fontSize = 11.sp, color = LolColors.TextSecondary)
            Spacer(Modifier.height(12.dp))

            if (data == null || data.tiers.isEmpty()) {
                Text("티어 데이터 없음", fontSize = 13.sp, color = LolColors.TextSecondary)
            } else {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(30.dp)) // icon space
                    Text("챔피언", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.weight(1f))
                    Text("티어", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(32.dp))
                    Text("판수", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(32.dp))
                    Text("승률", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(36.dp))
                    Text("KDA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LolColors.TextSecondary, modifier = Modifier.width(36.dp))
                }
                data.tiers.take(20).forEach { entry ->
                    val tierColor = when (entry.tier.uppercase()) {
                        "S" -> Color(0xFFFFD700)
                        "A" -> Color(0xFF4CAF50)
                        "B" -> LolColors.Info
                        "C" -> LolColors.TextSecondary
                        else -> LolColors.TextDisabled
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(LolColors.BgHover, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChampionIcon(entry.championId, 24.dp)
                        Text(entry.champion, fontSize = 12.sp, color = LolColors.TextPrimary, modifier = Modifier.weight(1f))
                        Text(entry.tier, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tierColor, modifier = Modifier.width(32.dp))
                        Text("${entry.games}", fontSize = 11.sp, color = LolColors.TextSecondary, modifier = Modifier.width(32.dp))
                        Text("${entry.winRate.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = winRateColor(entry.winRate), modifier = Modifier.width(36.dp))
                        Text(String.format("%.1f", entry.kda), fontSize = 11.sp, color = LolColors.TextSecondary, modifier = Modifier.width(36.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }
        }
    }
}
