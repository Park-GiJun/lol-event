package com.gijun.main.infrastructure.adapter.`in`.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.gijun.main.application.dto.query.DescribeMatchPredictionQuery
import com.gijun.main.application.dto.query.DescribePickQuery
import com.gijun.main.application.dto.query.SearchRagDocumentsQuery
import com.gijun.main.application.port.`in`.DescribeChampionUseCase
import com.gijun.main.application.port.`in`.DescribeLaneChampionsUseCase
import com.gijun.main.application.port.`in`.DescribeMatchPredictionUseCase
import com.gijun.main.application.port.`in`.DescribePickUseCase
import com.gijun.main.application.port.`in`.DescribePlayerUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.SearchRagDocumentsUseCase
import com.gijun.main.domain.rag.enums.RagDocumentType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

/**
 * 모델이 부를 수 있는 tool. **모델이 이 서비스를 쓰는 입구**라서 웹 어댑터와 같은 인바운드 어댑터다.
 *
 * - 설명(`@LLMDescription`)은 모델이 읽는다. 무엇을 넣어야 하는지 한국어 예시까지 적는다 — 설명이
 *   빈약하면 모델이 인자를 엉뚱하게 채운다.
 * - 답은 전부 글이다. 실패도 예외가 아니라 글로 돌려준다. 예외를 던지면 에이전트가 통째로 죽지만,
 *   "찾지 못했다" 는 글을 받으면 모델이 다른 이름으로 다시 시도하거나 사용자에게 되묻는다.
 */
@Component
@LLMDescription("리그 오브 레전드 내전 기록을 조회하는 도구 모음")
class LolAgentTools(
    private val describePlayerUseCase: DescribePlayerUseCase,
    private val describeChampionUseCase: DescribeChampionUseCase,
    private val describeLaneChampionsUseCase: DescribeLaneChampionsUseCase,
    private val describePickUseCase: DescribePickUseCase,
    private val describeMatchPredictionUseCase: DescribeMatchPredictionUseCase,
    private val getEloLeaderboardUseCase: GetEloLeaderboardUseCase,
    private val searchRagDocumentsUseCase: SearchRagDocumentsUseCase,
) : ToolSet {
    private val log = LoggerFactory.getLogger(javaClass)

    @Tool("get_player")
    @LLMDescription("플레이어 한 명의 내전 기록: 전적, 승률, Elo 와 순위, 포지션별 판수, 자주 하는 챔피언, 자주 같이 한 사람.")
    fun getPlayer(
        @LLMDescription("플레이어 이름. Riot ID 전체('아랑택#아랑택')나 '#' 앞부분('아랑택')만 넣어도 된다.")
        name: String,
    ): String = guarded("get_player", name) { describePlayerUseCase.describePlayer(name) }

    @Tool("get_champion")
    @LLMDescription(
        "챔피언 하나의 내전 기록: 판수와 승률, 가는 라인, 많이 한 사람(사람별 판수와 승률), 같은 라인에서 강했던 상대와 약했던 상대(카운터), " +
            "같은 팀일 때 성적이 좋은 챔피언. '피즈 잘하는 사람이 누구야', '세라핀 누가 해' 처럼 그 챔피언을 누가 하는지 묻는 질문에도 이걸 쓴다.",
    )
    fun getChampion(
        @LLMDescription("챔피언 이름. 한글('세라핀')이나 영문('Seraphine') 모두 된다.")
        name: String,
    ): String = guarded("get_champion", name) { describeChampionUseCase.describeChampion(name) }

    @Tool("get_champion_allies")
    @LLMDescription("이 챔피언과 같은 팀이었던 챔피언별 판수와 승률(라인 구분 없음). 고를 라인이 정해져 있으면 이것 말고 recommend_pick 을 쓴다.")
    fun getChampionAllies(
        @LLMDescription("아군 챔피언 이름. 한글이나 영문 모두 된다.")
        name: String,
    ): String = guarded("get_champion_allies", name) { describeChampionUseCase.describeChampionAllies(name) }

    @Tool("recommend_pick")
    @LLMDescription(
        "아군 챔피언이 정해졌을 때 한 라인에서 고를 챔피언 추천: 그 라인 챔피언별로, 아군들과 같은 팀이었던 판수와 승률. " +
            "'아군이 탑 사이온, 정글 자르반, 서폿 룰루일 때 미드 뭐 해', '우리 원딜이 징크스인데 서폿 뭐 하면 좋아' 처럼 " +
            "아군이 한 명 이상이고 고를 라인이 정해진 질문에 쓴다. 아군은 전부 한 번에 넣는다.",
    )
    fun recommendPick(
        @LLMDescription("고를 라인. '탑', '정글', '미드', '원딜', '서포터' 중 하나.")
        position: String,
        @LLMDescription("아군 챔피언 이름을 쉼표로 이어 쓴다. 예: '사이온, 자르반, 유나라, 룰루'.")
        allies: String,
    ): String =
        guarded("recommend_pick", "$position ← $allies") {
            describePickUseCase.describePick(DescribePickQuery(position, allies.split(',', '，', '、', '/')))
        }

    @Tool("get_lane_champions")
    @LLMDescription(
        "한 라인에서 성적이 좋은 챔피언 순위: 챔피언별 판수와 승률, 승률 높은 순. " +
            "'미드에서 승률 높은 챔피언', '탑 뭐가 좋아', '정글 1티어 챔피언' 처럼 라인만 정해지고 챔피언은 정해지지 않은 질문에 쓴다.",
    )
    fun getLaneChampions(
        @LLMDescription("라인. '탑', '정글', '미드', '원딜', '서포터' 중 하나.")
        position: String,
    ): String = guarded("get_lane_champions", position) { describeLaneChampionsUseCase.describeLaneChampions(position) }

    @Tool("get_elo_ranking")
    @LLMDescription("라인 Elo 순위표. 위에서부터 count 명의 순위, Elo, 주 포지션, 전적.")
    fun getEloRanking(
        @LLMDescription("몇 명까지 볼지. 1~30.")
        count: Int,
    ): String =
        guarded("get_elo_ranking", count.toString()) {
            val board = getEloLeaderboardUseCase.getEloLeaderboard(MIN_DUELS)
            board.players.take(count.coerceIn(1, MAX_RANKING)).joinToString("\n") { entry ->
                val position = entry.mainPosition?.let { POSITION_LABEL[it] ?: it } ?: "포지션 미정"
                "${entry.rank}위 ${entry.riotId} — Elo ${entry.laneElo.roundToInt()}, $position, ${entry.games}판 ${entry.wins}승 ${entry.losses}패"
            } + "\n(순위에 든 사람 ${board.rankedCount}명. 라인 맞대결 ${board.minDuels}번 미만은 배치 중이라 빠졌다.)"
        }

    @Tool("predict_match")
    @LLMDescription(
        "두 팀 열 명이 정해졌을 때 어느 팀이 이길지 예측한다: 블루와 레드의 승리 확률과 그 근거(자리별 라인 승률, 랭크 티어). " +
            "'이 팀으로 하면 누가 이겨', '블루 탑 아랑택 정글 도르비이 ... 레드 탑 화안시인 ... 승률 어때' 처럼 두 팀의 사람이 전부 나온 질문에 쓴다. " +
            "열 명과 각자의 라인을 다 알아야 한다. 빠진 사람이 있으면 부르지 말고 사용자에게 물어본다. 챔피언이 아니라 플레이어 이름을 넣는다.",
    )
    fun predictMatch(
        @LLMDescription("블루팀 탑 플레이어 이름. Riot ID 전체('아랑택#아랑택')나 '#' 앞부분('아랑택')만 넣어도 된다.")
        blueTop: String,
        @LLMDescription("블루팀 정글 플레이어 이름.")
        blueJungle: String,
        @LLMDescription("블루팀 미드 플레이어 이름.")
        blueMid: String,
        @LLMDescription("블루팀 원딜 플레이어 이름.")
        blueAdc: String,
        @LLMDescription("블루팀 서포터 플레이어 이름.")
        blueSupport: String,
        @LLMDescription("레드팀 탑 플레이어 이름.")
        redTop: String,
        @LLMDescription("레드팀 정글 플레이어 이름.")
        redJungle: String,
        @LLMDescription("레드팀 미드 플레이어 이름.")
        redMid: String,
        @LLMDescription("레드팀 원딜 플레이어 이름.")
        redAdc: String,
        @LLMDescription("레드팀 서포터 플레이어 이름.")
        redSupport: String,
    ): String {
        val blue = listOf(blueTop, blueJungle, blueMid, blueAdc, blueSupport)
        val red = listOf(redTop, redJungle, redMid, redAdc, redSupport)
        return guarded("predict_match", "${blue.joinToString(", ")} vs ${red.joinToString(", ")}") {
            describeMatchPredictionUseCase.describeMatchPrediction(DescribeMatchPredictionQuery(blue, red))
        }
    }

    @Tool("search_knowledge")
    @LLMDescription(
        "내전 기록에서 뜻이 가까운 글을 찾는다. 누구를 물어보는지 정해지지 않은 질문에 쓴다. " +
            "예: '라인전이 강한 탑', '요즘 연승 중인 사람', '펜타킬이 나온 경기'. " +
            "챔피언이나 플레이어 이름이 질문에 있으면 이것 말고 get_champion / get_player 를 쓴다.",
    )
    fun searchKnowledge(
        @LLMDescription("찾을 내용을 풀어 쓴 한국어 문장.")
        query: String,
        @LLMDescription("찾을 글의 종류. player = 플레이어 요약, champion = 챔피언 요약, match = 경기 리뷰, all = 전부.")
        kind: String,
    ): String =
        guarded("search_knowledge", "$kind: $query") {
            val docType =
                when (kind.trim().lowercase()) {
                    "player" -> RagDocumentType.PLAYER_PROFILE
                    "champion" -> RagDocumentType.CHAMPION_PROFILE
                    "match" -> RagDocumentType.MATCH_REVIEW
                    else -> null
                }
            val hits = searchRagDocumentsUseCase.searchRagDocuments(SearchRagDocumentsQuery(query, SEARCH_LIMIT, docType))
            if (hits.isEmpty()) "찾은 글이 없다." else hits.joinToString("\n\n") { it.content }
        }

    private fun guarded(
        tool: String,
        argument: String,
        block: () -> String,
    ): String =
        try {
            log.info("agent tool — {}({})", tool, argument)
            block()
        } catch (e: Exception) {
            log.warn("agent tool 실패 — {}({}): {}", tool, argument, e.message)
            "조회하지 못했다: ${e.message ?: e.javaClass.simpleName}"
        }

    private companion object {
        const val MIN_DUELS = 10
        const val MAX_RANKING = 30
        const val SEARCH_LIMIT = 4
        val POSITION_LABEL = mapOf("TOP" to "탑", "JUNGLE" to "정글", "MID" to "미드", "ADC" to "원딜", "SUPPORT" to "서포터")
    }
}
