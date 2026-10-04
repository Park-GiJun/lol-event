package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetChampionPageQuery
import com.gijun.main.application.dto.query.GetLaneChampionsQuery
import com.gijun.main.application.port.`in`.DescribeChampionUseCase
import com.gijun.main.application.port.`in`.DescribeLaneChampionsUseCase
import com.gijun.main.application.port.`in`.DescribePlayerUseCase
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
) : DescribePlayerUseCase,
    DescribeChampionUseCase,
    DescribeLaneChampionsUseCase {
    override fun describePlayer(name: String): String {
        val known = getTeamCandidatesUseCase.getTeamCandidates().map { it.riotId }
        val matched = matchPlayers(name, known)
        return when {
            matched.isEmpty() -> "'$name' 이라는 플레이어를 찾지 못했다. 등록된 사람: ${known.take(MAX_SUGGESTIONS).joinToString(", ")} 등 ${known.size}명."
            matched.size > 1 -> "'$name' 에 맞는 사람이 여럿이다: ${matched.joinToString(", ")}. 누구인지 정확히 알려 달라."
            else -> playerProfileComposer.compose(matched.single(), championNames())
        }
    }

    override fun describeChampion(name: String): String {
        val names = championNames()
        val key = names.resolve(name) ?: return "'$name' 이라는 챔피언을 찾지 못했다."
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

    /** 전체 일치 → '#' 앞부분 일치 → 포함 순으로 좁힌다. 띄어쓰기와 대소문자는 보지 않는다. */
    private fun matchPlayers(
        name: String,
        known: List<String>,
    ): List<String> {
        val wanted = normalize(name)
        if (wanted.isEmpty()) return emptyList()
        return known.filter { normalize(it) == wanted }.ifEmpty {
            known.filter { normalize(it.substringBefore('#')) == wanted }.ifEmpty {
                known.filter { normalize(it).contains(wanted) }
            }
        }
    }

    private fun normalize(text: String) = text.filterNot { it.isWhitespace() }.lowercase()

    private fun championNames() = ChampionNames(getDragonChampionsUseCase.getDragonChampions().associate { it.championKey to it.nameKo })

    private companion object {
        val SCOPE = GameMode.ALL
        const val MAX_SUGGESTIONS = 10

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
