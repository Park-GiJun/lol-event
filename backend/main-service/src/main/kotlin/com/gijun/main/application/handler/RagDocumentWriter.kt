package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.ChampionPageResult
import com.gijun.main.application.dto.result.ChampionSynergyResult
import com.gijun.main.application.dto.result.SummonerProfileResult
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.match.model.MatchParticipantModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 집계한 사실을 한국어 문장으로 옮긴다. 검색 문서와 에이전트 tool 의 답이 같은 글을 쓴다.
 *
 * **LLM 을 쓰지 않는다.** 숫자를 모델이 지어내지 않게 하는 것이 이 구조의 목적이라, 글에 들어가는
 * 값은 전부 통계 유즈케이스가 낸 것이다. 문장은 틀에 맞춰 찍어낸다.
 *
 * 글은 검색에 걸리라고 쓴다. 그래서 사람이 물어볼 말("서포터는 안 한다", "노틸러스에게 강하다")을
 * 그대로 적고, 챔피언은 한글 이름과 영문 키를 같이 적는다.
 */
internal object RagDocumentWriter {
    private val POSITION_LABEL =
        mapOf("TOP" to "탑", "JUNGLE" to "정글", "MID" to "미드", "ADC" to "원딜", "BOTTOM" to "원딜", "SUPPORT" to "서포터")
    private val LANE_ORDER = listOf("TOP", "JUNGLE", "MID", "ADC", "SUPPORT")
    private val KST = ZoneId.of("Asia/Seoul")
    private val MATCH_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private const val TOP_CHAMPIONS = 6
    private const val TOP_PEOPLE = 5
    private const val TOP_MATCHUPS = 6
    private const val BLUE_TEAM_ID = 100

    fun positionLabel(position: String): String = POSITION_LABEL[position] ?: position

    fun playerProfile(
        result: SummonerProfileResult,
        names: ChampionNames,
        insights: PlayerInsights,
    ): String {
        val p = result.profile
        val lines = mutableListOf<String>()
        lines += "[플레이어] ${p.riotId} (${p.riotId.substringBefore('#')})"
        lines +=
            "전적: ${p.games}판 ${p.wins}승 ${p.losses}패, 승률 ${p.winRate}%. KDA ${p.kda} (평균 ${p.avgKills}/${p.avgDeaths}/${p.avgAssists})."

        p.rating?.let { rating ->
            val rank = p.eloRank?.let { " (${p.eloRankedTotal}명 중 ${it}위)" } ?: " (배치 중이라 순위 없음)"
            lines +=
                "라인 Elo ${rating.laneElo.roundToInt()}$rank, 라인전 ${rating.laneDuels}번 중 ${rating.laneWins}번 이김. 팀 Elo ${rating.teamElo.roundToInt()}."
        }

        val positions = result.positionStats.filter { it.games > 0 }.sortedByDescending { it.games }
        if (positions.isNotEmpty()) {
            lines += "포지션: " + positions.joinToString(", ") { "${positionLabel(it.position)} ${it.games}판 승률 ${it.winRate}%" } + "."
            lines += "주 포지션은 ${positionLabel(positions.first().position)}."
            val played = positions.map { if (it.position == "BOTTOM") "ADC" else it.position }.toSet()
            val never = LANE_ORDER.filterNot { it in played }
            if (never.isNotEmpty()) lines += "${never.joinToString(", ") { positionLabel(it) }}는 한 번도 하지 않았다."
        }

        // 판단까지 코드가 끝낸 것이다. 모델은 이 줄을 근거와 함께 옮기기만 한다.
        lines += "강점: " + insights.strengths.ifEmpty { listOf(NOTHING_STANDS_OUT) }.joinToString("; ") + "."
        lines += "약점: " + insights.weaknesses.ifEmpty { listOf(NOTHING_STANDS_OUT) }.joinToString("; ") + "."

        val champions = result.championStats.sortedByDescending { it.games }.take(TOP_CHAMPIONS)
        if (champions.isNotEmpty()) {
            lines += "자주 하는 챔피언: " + champions.joinToString(", ") { "${names.label(it.champion)} ${it.games}판 승률 ${it.winRate}%" } + "."
        }

        when {
            result.streak.current >= 2 -> lines += "지금 ${result.streak.current}연승 중."
            result.streak.current <= -2 -> lines += "지금 ${-result.streak.current}연패 중."
        }

        val teammates = result.teammates.take(TOP_PEOPLE)
        if (teammates.isNotEmpty()) {
            lines += "같은 팀으로 많이 한 사람: " + teammates.joinToString(", ") { "${it.riotId} ${it.games}판 승률 ${it.winRate}%" } + "."
        }
        val opponents = result.opponents.take(TOP_PEOPLE)
        if (opponents.isNotEmpty()) {
            lines += "상대로 많이 만난 사람: " + opponents.joinToString(", ") { "${it.riotId} ${it.games}판 중 ${it.wins}승" } + "."
        }
        return lines.joinToString("\n")
    }

    fun championProfile(
        page: ChampionPageResult,
        synergy: ChampionSynergyResult?,
        names: ChampionNames,
    ): String {
        val d = page.detail
        val lines = mutableListOf<String>()
        lines += "[챔피언] ${names.label(d.champion)}"
        lines += "내전 ${d.totalGames}판 ${d.totalWins}승, 승률 ${d.winRate}%." + (page.tier?.let { " 티어 ${it.tier}." } ?: "")

        val lanes = page.laneStrength.filter { it.games > 0 }.sortedByDescending { it.games }
        if (lanes.isNotEmpty()) {
            lines += "가는 라인: " + lanes.joinToString(", ") { "${positionLabel(it.position)} ${it.games}판 승률 ${it.winRate}%" } + "."
        }

        val players = d.players.sortedByDescending { it.games }.take(TOP_PEOPLE)
        if (players.isNotEmpty()) {
            lines += "많이 한 사람: " + players.joinToString(", ") { "${it.riotId} ${it.games}판 승률 ${it.winRate}%" } + "."
        }

        // 같은 라인에서 맞붙은 상대. 표본이 작으니 판수를 꼭 같이 적는다.
        val strong =
            page.matchups
                .filter { it.winRate >= WIN_LINE }
                .sortedByDescending { it.games }
                .take(TOP_MATCHUPS)
        val weak =
            page.matchups
                .filter { it.winRate < WIN_LINE }
                .sortedByDescending { it.games }
                .take(TOP_MATCHUPS)
        if (strong.isNotEmpty()) {
            lines += "라인전에서 강했던 상대: " + strong.joinToString(", ") { "${names.label(it.opponent)} 상대로 ${it.games}판 승률 ${it.winRate}%" } + "."
        }
        if (weak.isNotEmpty()) {
            lines += "라인전에서 약했던 상대(카운터): " +
                weak.joinToString(", ") { "${names.label(it.opponent)} 상대로 ${it.games}판 승률 ${it.winRate}%" } + "."
        }
        if (page.matchups.isEmpty()) lines += "같은 상대와 ${page.matchupMinGames}판 이상 맞붙은 기록이 없어 상성은 말할 수 없다."

        synergy?.let { lines += allies(it, names) }
        return lines.joinToString("\n")
    }

    /** 같은 팀이었던 챔피언별 전적 한 줄. */
    fun allies(
        synergy: ChampionSynergyResult,
        names: ChampionNames,
    ): String {
        if (synergy.allies.isEmpty()) return "${names.label(synergy.champion)}와 같은 팀으로 2판 이상 나온 챔피언이 없다."
        val best =
            synergy.allies.sortedWith(
                compareByDescending<com.gijun.main.application.dto.result.AllyChampionStat> {
                    it.winRate
                }.thenByDescending { it.games },
            )
        return "같은 팀일 때 성적: " + best.take(TOP_ALLIES).joinToString(", ") { "${names.label(it.champion)}와 ${it.games}판 승률 ${it.winRate}%" } +
            "."
    }

    fun matchReview(
        match: MatchModel,
        names: ChampionNames,
    ): String {
        val time = MATCH_TIME.format(Instant.ofEpochMilli(match.gameCreation).atZone(KST))
        val minutes = match.gameDuration / SECONDS_PER_MINUTE
        val blue = match.participants.filter { it.teamId == BLUE_TEAM_ID }
        val red = match.participants.filter { it.teamId != BLUE_TEAM_ID }
        val blueWon = blue.firstOrNull()?.win ?: false

        val lines = mutableListOf<String>()
        lines += "[경기] ${match.matchId} · $time · ${minutes}분 · ${if (blueWon) "블루팀" else "레드팀"} 승"
        lines += "블루팀(${if (blueWon) "승" else "패"}): " + teamLine(blue, names)
        lines += "레드팀(${if (blueWon) "패" else "승"}): " + teamLine(red, names)

        match.participants.maxByOrNull { it.kills }?.takeIf { it.kills > 0 }?.let {
            lines += "최다 킬: ${it.riotId} (${names.label(it.champion)}) ${it.kills}킬."
        }
        match.participants.maxByOrNull { it.damage }?.takeIf { it.damage > 0 }?.let {
            lines += "최다 딜: ${it.riotId} (${names.label(it.champion)}) ${it.damage}."
        }
        return lines.joinToString("\n")
    }

    private fun teamLine(
        team: List<MatchParticipantModel>,
        names: ChampionNames,
    ): String =
        team
            .sortedBy { LANE_ORDER.indexOf(it.assignedPosition).let { index -> if (index < 0) LANE_ORDER.size else index } }
            .joinToString(", ") {
                val lane = POSITION_LABEL[it.assignedPosition]?.let { label -> "$label " } ?: ""
                "$lane${it.riotId} ${names.label(it.champion)} ${it.kills}/${it.deaths}/${it.assists}"
            }

    private const val WIN_LINE = 50
    private const val NOTHING_STANDS_OUT = "같은 포지션 사람들과 견줘 두드러지는 수치가 없다(표본이 적을 수도 있다)"
    private const val TOP_ALLIES = 8
    private const val SECONDS_PER_MINUTE = 60
}

/** 챔피언 영문 키 → "세라핀(Seraphine)". Data Dragon 이 아직 없으면 키만 쓴다. */
internal class ChampionNames(
    private val koreanByKey: Map<String, String>,
) {
    fun label(key: String): String = koreanByKey[key]?.let { "$it($key)" } ?: key

    /** 한글 이름이나 영문 키(대소문자·띄어쓰기 무시)로 영문 키를 찾는다. */
    fun resolve(input: String): String? {
        val wanted = normalize(input)
        if (wanted.isEmpty()) return null
        return koreanByKey.entries.firstOrNull { normalize(it.key) == wanted || normalize(it.value) == wanted }?.key
            ?: koreanByKey.entries.firstOrNull { normalize(it.value).contains(wanted) || normalize(it.key).contains(wanted) }?.key
    }

    private fun normalize(text: String) = text.filterNot { it.isWhitespace() || it == '\'' || it == '.' }.lowercase()
}
