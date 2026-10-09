package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.DescribePickQuery
import com.gijun.main.application.dto.query.GetAllyPicksQuery
import com.gijun.main.application.dto.query.GetChampionPageQuery
import com.gijun.main.application.dto.query.GetLaneChampionsQuery
import com.gijun.main.application.port.`in`.DescribeChampionUseCase
import com.gijun.main.application.port.`in`.DescribeLaneChampionsUseCase
import com.gijun.main.application.port.`in`.DescribePickUseCase
import com.gijun.main.application.port.`in`.DescribePlayerUseCase
import com.gijun.main.application.port.`in`.GetAllyPicksUseCase
import com.gijun.main.application.port.`in`.GetChampionPageUseCase
import com.gijun.main.application.port.`in`.GetChampionSynergyUseCase
import com.gijun.main.application.port.`in`.GetDragonChampionsUseCase
import com.gijun.main.application.port.`in`.GetLaneChampionsUseCase
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import com.gijun.main.domain.match.enums.GameMode
import org.springframework.stereotype.Service

/**
 * 에이전트 tool 이 읽을 글을 그 자리에서 쓴다.
 *
 * 저장된 검색 문서와 같은 글이지만, 여기서는 **지금 통계**로 새로 쓴다. 문서는 경기 저장 뒤에
 * 비동기로 갱신되므로 조금 늦을 수 있고, 숫자를 묻는 질문에는 늦은 값을 주면 안 된다.
 *
 * 이름을 너그럽게 받는다. 사람은 "아랑택" 이라고 쓰지 "아랑택#아랑택" 이라고 쓰지 않는다.
 */
@Service
class RagDescribeQueryHandler(
    private val playerProfileComposer: PlayerProfileComposer,
    private val getChampionPageUseCase: GetChampionPageUseCase,
    private val getChampionSynergyUseCase: GetChampionSynergyUseCase,
    private val getDragonChampionsUseCase: GetDragonChampionsUseCase,
    private val getTeamCandidatesUseCase: GetTeamCandidatesUseCase,
    private val getLaneChampionsUseCase: GetLaneChampionsUseCase,
    private val getAllyPicksUseCase: GetAllyPicksUseCase,
) : DescribePlayerUseCase,
    DescribeChampionUseCase,
    DescribeLaneChampionsUseCase,
    DescribePickUseCase {
    /**
     * 이름만으로는 사람인지 챔피언인지 모델이 틀릴 수 있다("도르비이 장단점" 에 챔피언 조회를 부른다).
     * 그래서 한쪽에서 못 찾으면 다른 쪽을 찾아 **그 기록을 바로 준다** — 되묻게 하면 한 바퀴를 더 돈다.
     */
    override fun describePlayer(name: String): String {
        val known = knownPlayers()
        return playerText(name, known)
            ?: championText(name)?.let { "'$name' 은 플레이어가 아니라 챔피언이다. 아래는 그 챔피언의 기록이다.\n$it" }
            ?: (
                "'$name' 이라는 플레이어도 챔피언도 찾지 못했다. 등록된 사람: ${known.take(MAX_SUGGESTIONS).joinToString(", ")} 등 ${known.size}명. " +
                    NO_GUESSING
            )
    }

    override fun describeChampion(name: String): String =
        championText(name)
            ?: playerText(name, knownPlayers())?.let { "'$name' 은 챔피언이 아니라 플레이어다. 아래는 그 플레이어의 기록이다.\n$it" }
            ?: "'$name' 이라는 챔피언도 플레이어도 찾지 못했다. $NO_GUESSING"

    private fun knownPlayers() = getTeamCandidatesUseCase.getTeamCandidates().map { it.riotId }

    /** @return 맞는 사람이 없으면 null. 여럿이면 누구인지 되묻는 글. */
    private fun playerText(
        name: String,
        known: List<String>,
    ): String? {
        val matched = PlayerNames.match(name, known)
        return when {
            matched.isEmpty() -> null
            matched.size > 1 -> "'$name' 에 맞는 사람이 여럿이다: ${matched.joinToString(", ")}. 누구인지 정확히 알려 달라."
            else -> playerProfileComposer.compose(matched.single(), championNames())
        }
    }

    /** @return 그런 챔피언이 없으면 null. */
    private fun championText(name: String): String? {
        val names = championNames()
        val key = names.resolve(name) ?: return null
        val page = getChampionPageUseCase.getChampionPage(GetChampionPageQuery(key, SCOPE))
        if (page.detail.totalGames == 0) return "${names.label(key)}는 내전에서 한 번도 나오지 않았다."
        return RagDocumentWriter.championProfile(page, getChampionSynergyUseCase.getChampionSynergy(key), names)
    }

    override fun describeChampionAllies(name: String): String {
        val names = championNames()
        val key = names.resolve(name) ?: return "'$name' 이라는 챔피언을 찾지 못했다."
        val synergy = getChampionSynergyUseCase.getChampionSynergy(key)
        if (synergy.games == 0) return "${names.label(key)}는 내전에서 한 번도 나오지 않았다."
        return "${names.label(key)} — 내전 ${synergy.games}판. " + RagDocumentWriter.allies(synergy, names)
    }

    override fun describeLaneChampions(position: String): String {
        val lane = LANE_BY_WORD[normalize(position)] ?: return "'$position' 은 모르는 라인이다. 탑, 정글, 미드, 원딜, 서포터 중 하나로 알려 달라."
        val ranked = getLaneChampionsUseCase.getLaneChampions(GetLaneChampionsQuery(lane, SCOPE))
        return RagDocumentWriter.laneChampions(lane, ranked, championNames())
    }

    override fun describePick(query: DescribePickQuery): String {
        val lane = LANE_BY_WORD[normalize(query.position)] ?: return "'${query.position}' 은 모르는 라인이다. 탑, 정글, 미드, 원딜, 서포터 중 하나로 알려 달라."
        val names = championNames()

        // 모델이 "탑 사이온" 처럼 라인을 붙여 보내기도 한다. 라인 말은 떼고 챔피언 이름만 본다.
        val asked = query.allies.map(::withoutLaneWords).filter { it.isNotEmpty() }
        val resolved = asked.associateWith { names.resolve(it) }
        val unknown = resolved.filterValues { it == null }.keys
        val allies = resolved.values.filterNotNull().distinct()
        if (allies.isEmpty()) return "아군 챔피언을 찾지 못했다: ${asked.joinToString(", ")}. 이름을 다시 확인해 달라."

        val picks = getAllyPicksUseCase.getAllyPicks(GetAllyPicksQuery(lane, allies))
        val laneStats = getLaneChampionsUseCase.getLaneChampions(GetLaneChampionsQuery(lane, SCOPE)).associateBy { it.champion }
        val text = RagDocumentWriter.pickRecommendation(lane, allies, picks, laneStats, names)
        return if (unknown.isEmpty()) text else "$text\n찾지 못한 챔피언 이름(빼고 계산했다): ${unknown.joinToString(", ")}."
    }

    private fun withoutLaneWords(name: String): String =
        name
            .split(' ', '-', ':', '(', ')')
            .filter { it.isNotBlank() && normalize(it) !in LANE_BY_WORD }
            .joinToString(" ")

    private fun normalize(text: String) = PlayerNames.normalize(text)

    private fun championNames() = ChampionNames(getDragonChampionsUseCase.getDragonChampions().associate { it.championKey to it.nameKo })

    private companion object {
        val SCOPE = GameMode.ALL
        const val MAX_SUGGESTIONS = 10

        /** 못 찾았을 때 모델이 그럴듯한 이름을 지어내 제안하지 않게 글에 적어 준다. */
        const val NO_GUESSING = "비슷한 이름을 지어내 제안하지 않는다. 이름을 다시 확인해 달라고만 한다."

        /** 사람과 모델이 라인을 부르는 말. 띄어쓰기를 빼고 소문자로 맞춘다. */
        val LANE_BY_WORD =
            mapOf(
                "TOP" to listOf("top", "탑", "탑솔", "탑라인"),
                "JUNGLE" to listOf("jungle", "jg", "jug", "정글"),
                "MID" to listOf("mid", "middle", "미드", "미드라인"),
                "ADC" to listOf("adc", "ad", "bot", "bottom", "원딜", "바텀", "봇"),
                "SUPPORT" to listOf("support", "sup", "supp", "서포터", "서폿", "서포트"),
            ).flatMap { (lane, words) -> words.map { it to lane } }.toMap()
    }
}
