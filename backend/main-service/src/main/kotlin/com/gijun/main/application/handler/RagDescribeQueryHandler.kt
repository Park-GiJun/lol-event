package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetChampionPageQuery
import com.gijun.main.application.dto.query.GetSummonerProfileQuery
import com.gijun.main.application.port.`in`.DescribeChampionUseCase
import com.gijun.main.application.port.`in`.DescribePlayerUseCase
import com.gijun.main.application.port.`in`.GetChampionPageUseCase
import com.gijun.main.application.port.`in`.GetChampionSynergyUseCase
import com.gijun.main.application.port.`in`.GetDragonChampionsUseCase
import com.gijun.main.application.port.`in`.GetSummonerProfileUseCase
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.domain.vo.RiotId
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
    private val getSummonerProfileUseCase: GetSummonerProfileUseCase,
    private val getChampionPageUseCase: GetChampionPageUseCase,
    private val getChampionSynergyUseCase: GetChampionSynergyUseCase,
    private val getDragonChampionsUseCase: GetDragonChampionsUseCase,
    private val getTeamCandidatesUseCase: GetTeamCandidatesUseCase,
) : DescribePlayerUseCase,
    DescribeChampionUseCase {
    override fun describePlayer(name: String): String {
        val known = getTeamCandidatesUseCase.getTeamCandidates().map { it.riotId }
        val matched = matchPlayers(name, known)
        return when {
            matched.isEmpty() -> "'$name' 이라는 플레이어를 찾지 못했다. 등록된 사람: ${known.take(MAX_SUGGESTIONS).joinToString(", ")} 등 ${known.size}명."
            matched.size > 1 -> "'$name' 에 맞는 사람이 여럿이다: ${matched.joinToString(", ")}. 누구인지 정확히 알려 달라."
            else ->
                RagDocumentWriter.playerProfile(
                    getSummonerProfileUseCase.getSummonerProfile(GetSummonerProfileQuery(RiotId(matched.single()), SCOPE)),
                    championNames(),
                )
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
    }
}
