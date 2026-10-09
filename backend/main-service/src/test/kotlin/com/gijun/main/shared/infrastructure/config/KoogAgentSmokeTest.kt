package com.gijun.main.shared.infrastructure.config

import com.gijun.main.application.dto.command.BuildTeamsCommand
import com.gijun.main.application.dto.command.BuildTeamsPlayer
import com.gijun.main.application.dto.query.DescribeMatchPredictionQuery
import com.gijun.main.application.dto.query.DescribePickQuery
import com.gijun.main.application.dto.query.SearchRagDocumentsQuery
import com.gijun.main.application.dto.result.EloLeaderboardResult
import com.gijun.main.application.dto.result.RagDocumentResult
import com.gijun.main.application.dto.result.TeamCandidatePositionResult
import com.gijun.main.application.dto.result.TeamCandidateResult
import com.gijun.main.application.handler.TeamBuildCommandHandler
import com.gijun.main.application.port.`in`.DescribeChampionUseCase
import com.gijun.main.application.port.`in`.DescribeLaneChampionsUseCase
import com.gijun.main.application.port.`in`.DescribeMatchPredictionUseCase
import com.gijun.main.application.port.`in`.DescribePickUseCase
import com.gijun.main.application.port.`in`.DescribePlayerUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import com.gijun.main.application.port.`in`.SearchRagDocumentsUseCase
import com.gijun.main.application.port.out.persistence.RagDocumentQueryPersistencePort
import com.gijun.main.domain.rag.enums.ChatRole
import com.gijun.main.domain.rag.model.ChatMessageModel
import com.gijun.main.infrastructure.adapter.`in`.agent.LolAgentTools
import com.gijun.main.infrastructure.adapter.out.external.KoogLlmChatAdapter
import com.gijun.main.infrastructure.adapter.out.external.KoogLlmCompletionAdapter
import com.gijun.main.infrastructure.adapter.out.external.LlmGate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * 에이전트가 **실제 모델로** tool 을 부르고 그 결과로 답하는지 본다. 집 안 망에서만 돈다.
 *
 * ```
 * RAG_SMOKE=1 ./gradlew :main-service:test --tests '*KoogAgentSmokeTest' -i
 * ```
 *
 * 통계는 가짜를 꽂는다(DB 없이 돈다). 보려는 것은 "모델이 맞는 tool 을 맞는 인자로 부르는가" 와
 * "tool 이 준 숫자를 그대로 쓰는가" 다. 프롬프트나 tool 설명을 고쳤을 때 돌려 본다.
 */
@EnabledIfEnvironmentVariable(named = "RAG_SMOKE", matches = "1")
class KoogAgentSmokeTest {
    private val properties =
        RagProperties(
            chat =
                RagProperties.Chat(
                    baseUrl =
                        System.getenv("RAG_SMOKE_CHAT_URL") ?: RagProperties.Chat().baseUrl,
                ),
        )
    private val config = KoogConfig(properties)
    private val client = config.chatClient()
    private val executor = config.promptExecutor(client)
    private val calls = mutableListOf<String>()

    private val tools =
        LolAgentTools(
            describePlayerUseCase =
                object : DescribePlayerUseCase {
                    override fun describePlayer(name: String): String {
                        calls += "player:$name"
                        return """
                            [플레이어] 아랑택#아랑택 (아랑택)
                            전적: 113판 48승 65패, 승률 42%. KDA 2.02 (평균 4.6/5.8/7.0).
                            라인 Elo 1403 (45명 중 43위), 라인전 106번 중 40번 이김. 팀 Elo 1422.
                            포지션: 탑 53판 승률 41%, 미드 28판 승률 46%, 정글 18판 승률 50%, 원딜 14판 승률 28%.
                            주 포지션은 탑.
                            서포터는 한 번도 하지 않았다.
                            강점: 정글에서 18판 승률 50%; 직접 플레이한 사이온(Sion) 28판 승률 57% (KDA 2.91).
                            약점: 라인 Elo 1403 — 45명 중 43위 (라인전 106번 중 40승, 승률 38%); 평균 데스 5.6 — 탑 12명 중 10위; 원딜에서 14판 승률 28%; 직접 플레이한 초가스(Chogath) 8판 승률 25% (KDA 1.80).
                            자주 하는 챔피언: 사이온(Sion) 28판 승률 57%, 초가스(Chogath) 8판 승률 25%.
                            """.trimIndent()
                    }
                },
            describeChampionUseCase =
                object : DescribeChampionUseCase {
                    override fun describeChampion(name: String): String {
                        calls += "champion:$name"
                        return """
                            [챔피언] 세라핀(Seraphine)
                            내전 58판 26승, 승률 44%. 티어 D.
                            가는 라인: 서포터 56판 승률 44%, 미드 1판 승률 100%.
                            많이 한 사람: 도르비이#KR1 31판 승률 51%, 화안시인#KR1 12판 승률 33%.
                            라인전에서 강했던 상대: 노틸러스(Nautilus) 상대로 8판 승률 62%.
                            라인전에서 약했던 상대(카운터): 레오나(Leona) 상대로 6판 승률 16%, 블리츠크랭크(Blitzcrank) 상대로 4판 승률 25%.
                            """.trimIndent()
                    }

                    override fun describeChampionAllies(name: String): String {
                        calls += "allies:$name"
                        return "징크스(Jinx) — 내전 30판. 같은 팀일 때 성적: 룰루(Lulu)와 7판 승률 71%, 레오나(Leona)와 5판 승률 40%."
                    }
                },
            describeLaneChampionsUseCase =
                object : DescribeLaneChampionsUseCase {
                    override fun describeLaneChampions(position: String): String {
                        calls += "lane:$position"
                        return """
                            [라인] 미드 챔피언 순위 — 3판 이상 나온 14종 중 위에서 3종.
                            판수가 적은 승률은 50% 쪽으로 당겨서 줄 세웠다. 그래서 승률이 더 높아도 판수가 적으면 아래에 있을 수 있다.
                            1위 아리(Ahri) — 12판 8승, 승률 66%
                            2위 신드라(Syndra) — 9판 6승, 승률 66%
                            3위 르블랑(Leblanc) — 5판 3승, 승률 60%
                            (3판 미만이라 뺀 챔피언 21종.)
                            """.trimIndent()
                    }
                },
            describePickUseCase =
                object : DescribePickUseCase {
                    override fun describePick(query: DescribePickQuery): String {
                        calls += "pick:${query.position}:${query.allies.joinToString("|") { it.trim() }}"
                        return """
                            [픽 추천] ${query.position} — 아군: ${query.allies.joinToString(", ") { it.trim() }}
                            이 라인에 섰던 챔피언만 골랐다. 판수는 아군마다 따로 세어 더했고, 판수가 적은 승률은 50% 쪽으로 당겨서 줄 세웠다.
                            1위 말자하(Malzahar) — 아군과 합쳐 9판 6승, 승률 66% (사이온(Sion)와 4판 3승, 룰루(Lulu)와 5판 3승). 미드 전체 16판 승률 62%.
                            2위 룰루(Lulu) — 아군과 합쳐 7판 5승, 승률 71% (징크스(Jinx)와 7판 5승). 서포터 전체 20판 승률 60%.
                            3위 빅토르(Viktor) — 아군과 합쳐 6판 3승, 승률 50% (사이온(Sion)와 6판 3승). 미드 전체 30판 승률 63%.
                            """.trimIndent()
                    }
                },
            describeMatchPredictionUseCase =
                object : DescribeMatchPredictionUseCase {
                    override fun describeMatchPrediction(query: DescribeMatchPredictionQuery): String {
                        calls += "predict:${query.blue.joinToString("|") { it.trim() }}:${query.red.joinToString("|") { it.trim() }}"
                        return """
                            [승률 예측] 블루 57% · 레드 43% — 블루가 조금 유리하다
                            픽 전 기준이다. 챔피언, 조합, 상성, 같은 팀끼리의 호흡은 보지 않았다.
                            계산에 들어간 것은 아래 두 가지뿐이다.
                            - 자리 라인 승률(그 자리에서 맞상대보다 더 크게 성장한 비율. 표본이 적으면 평소 승률 쪽으로 당겼다): 블루 평균 52%, 레드 평균 48% → 블루가 3.6%p 높다.
                            - 랭크 게임 티어: 블루 평균 EMERALD III, 레드 평균 PLATINUM I → 블루가 0.2티어 높다.
                            주의: 내전 160경기로 학습한 모델이다. 표본이 적어서 검증에서 동전 던지기보다 낫다고 확정하지 못했다. 참고용이라는 말을 꼭 붙인다.
                            """.trimIndent()
                    }
                },
            getEloLeaderboardUseCase =
                object : GetEloLeaderboardUseCase {
                    override fun getEloLeaderboard(minDuels: Int) = EloLeaderboardResult(emptyList(), minDuels, 0, 0)
                },
            searchRagDocumentsUseCase =
                object : SearchRagDocumentsUseCase {
                    override fun searchRagDocuments(query: SearchRagDocumentsQuery): List<RagDocumentResult> {
                        calls += "search:${query.text}"
                        return emptyList()
                    }
                },
        )

    private val chat = KoogLlmChatAdapter(executor, config.chatModel(), LlmGate(), properties, tools)

    @Test
    fun `플레이어 질문에 get_player 를 부르고 그 내용으로 답한다`() {
        val answer = chat.answer(emptyList(), "아랑택은 서포터 해본 적 있어? 주로 어디 가?")
        println("answer → $answer\ncalls → $calls")

        assertTrue(calls.any { it.startsWith("player:") && it.contains("아랑택") }) { "get_player 를 부르지 않았다: $calls" }
        assertTrue(answer.contains("탑")) { answer }
    }

    @Test
    fun `카운터 질문에 get_champion 을 부르고 tool 이 준 챔피언을 말한다`() {
        val answer = chat.answer(emptyList(), "세라핀 카운터가 뭐야?")
        println("answer → $answer\ncalls → $calls")

        assertTrue(calls.any { it.startsWith("champion:") }) { "get_champion 을 부르지 않았다: $calls" }
        assertTrue(answer.contains("레오나")) { answer }
    }

    @Test
    fun `챔피언을 누가 잘하는지 물으면 get_champion 의 많이 한 사람으로 답하고 검색하지 않는다`() {
        val answer = chat.answer(emptyList(), "세라핀을 잘하는 플레이어가 누구여?")
        println("answer → $answer\ncalls → $calls")

        assertTrue(calls.any { it.startsWith("champion:") }) { "get_champion 을 부르지 않았다: $calls" }
        assertTrue(calls.none { it.startsWith("search:") }) { "search_knowledge 로 찾으면 그 챔피언을 안 한 사람이 섞인다: $calls" }
        assertTrue(answer.contains("도르비이")) { answer }
    }

    @Test
    fun `아군 하나와 고를 라인을 주면 recommend_pick 을 부른다`() {
        val answer = chat.answer(emptyList(), "우리 원딜이 징크스인데 서폿 뭐 하면 좋아?")
        println("answer → $answer\ncalls → $calls")

        assertTrue(calls.any { it.startsWith("pick:") && it.contains("징크스") }) { "recommend_pick 을 부르지 않았다: $calls" }
        assertTrue(answer.contains("룰루")) { answer }
    }

    @Test
    fun `라인만 정해진 챔피언 질문에 get_lane_champions 를 한 번 부르고 한글 이름 그대로 답한다`() {
        val answer = chat.answer(emptyList(), "미드에서 승률 높은 챔피언 뽑아줘")
        println("answer → $answer\ncalls → $calls")

        assertTrue(calls.size == 1 && calls.single().startsWith("lane:")) { "get_lane_champions 만 한 번 불러야 한다: $calls" }
        assertTrue(answer.contains("아리") && answer.contains("신드라")) { answer }
        // 영문 이름을 소리 나는 대로 옮긴 표기가 섞이면 안 된다.
        assertTrue(listOf("아흐리", "아리이", "레블랑").none { answer.contains(it) }) { answer }
    }

    @Test
    fun `아군이 여럿이면 전부 한 번에 넣어 recommend_pick 을 부른다`() {
        val answer = chat.answer(emptyList(), "아군이 탑 - 사이온 정글 - 자르반 원딜 - 유나라 서폿 - 룰루일때 미드 챔피언은 뭘하는게 좋아?")
        println("answer → $answer\ncalls → $calls")

        val pick = calls.singleOrNull { it.startsWith("pick:") } ?: error("recommend_pick 을 한 번만 불러야 한다: $calls")
        listOf("사이온", "자르반", "유나라", "룰루").forEach { ally -> assertTrue(pick.contains(ally)) { "아군 $ally 이 빠졌다: $pick" } }
        assertTrue(answer.contains("말자하")) { answer }
    }

    @Test
    fun `두 팀 열 명이 나오면 predict_match 를 라인 순서대로 부르고 tool 이 준 확률과 판정을 그대로 쓴다`() {
        val answer =
            chat.answer(
                emptyList(),
                "블루 탑 가나 정글 다라 미드 마바 원딜 사아 서폿 자차, 레드 탑 카타 정글 파하 미드 거너 원딜 더러 서폿 머버. 누가 이겨?",
            )
        println("answer → $answer\ncalls → $calls")

        assertEquals(listOf("predict:가나|다라|마바|사아|자차:카타|파하|거너|더러|머버"), calls.filter { it.startsWith("predict:") })
        assertTrue(answer.contains("57")) { answer }
        assertTrue(answer.contains("참고")) { answer }
    }

    @Test
    fun `한 팀만 나온 승패 질문에는 predict_match 를 부르지 않고 되묻는다`() {
        val answer = chat.answer(emptyList(), "탑 가나 정글 다라 미드 마바 원딜 사아 서폿 자차 이 팀 이길 수 있어?")
        println("answer → $answer\ncalls → $calls")

        assertTrue(calls.none { it.startsWith("predict:") }) { "상대 팀을 모르는데 predict_match 를 불렀다: $calls" }
    }

    @Test
    fun `지난 대화를 이어받는다`() {
        val history =
            listOf(
                ChatMessageModel(ChatRole.USER, "아랑택 전적 알려줘"),
                ChatMessageModel(ChatRole.ASSISTANT, "아랑택#아랑택은 113판 48승 65패, 승률 42%입니다."),
            )
        val answer = chat.answer(history, "그 사람 주 포지션은?")
        println("answer → $answer\ncalls → $calls")

        // 지난 대화에 주 포지션은 없다. tool 없이 답하면 지어낸 것이다.
        assertTrue(calls.any { it.startsWith("player:") && it.contains("아랑택") }) { "get_player 를 다시 부르지 않았다: $calls" }
        assertTrue(answer.contains("탑")) { answer }
    }

    @Test
    fun `어떤 사람인지 물으면 장점과 단점을 tool 이 준 수치와 함께 답한다`() {
        val answer = chat.answer(emptyList(), "아랑택 어때? 장단점 알려줘")
        println("answer → $answer\ncalls → $calls")

        assertTrue(answer.contains("장점") && answer.contains("단점")) { answer }
        // 순위와 승률은 tool 이 준 값 그대로여야 한다.
        val compact = answer.filterNot { it.isWhitespace() }
        assertTrue(compact.contains("43위")) { answer }
        // 약점은 넷 중 두세 개를 고른다. 어느 것을 골랐든 승률은 tool 이 준 값이어야 한다.
        assertTrue(compact.contains("28%") || compact.contains("25%")) { answer }
        // 초가스는 본인이 플레이한 챔피언이다. "초가스를 상대로 약하다" 로 뒤집어 읽으면 안 된다.
        assertTrue(listOf("초가스에약", "초가스를상대", "초가스상대", "상대가잘").none { compact.contains(it) }) { answer }
    }

    @Test
    fun `팀 편성 해설은 팀마다 장점과 단점을 수치와 함께 쓴다`() {
        val lanes = listOf("TOP", "JUNGLE", "MID", "ADC", "SUPPORT")
        val known =
            (1..10).map { i ->
                TeamCandidateResult("선수$i#KR1", 1350.0 + i * 35, 40, lanes[i % 5], lanes.map { TeamCandidatePositionResult(it, 8) }, lanes)
            }
        val profiles =
            mock<RagDocumentQueryPersistencePort> {
                on { findContent(any(), any()) } doReturn
                    listOf("전적: 40판 22승 18패, 승률 55%.", "강점: KDA 4.10 — 탑 12명 중 2위.", "약점: 시야 점수 14.0 — 탑 12명 중 11위.").joinToString("\n")
            }
        val handler =
            TeamBuildCommandHandler(
                object : GetTeamCandidatesUseCase {
                    override fun getTeamCandidates() = known
                },
                profiles,
                KoogLlmCompletionAdapter(executor, config.chatModel(), LlmGate(), properties),
            )

        val result = handler.buildTeams(BuildTeamsCommand(known.map { BuildTeamsPlayer(it.riotId) }))
        println("commentary → ${result.commentary}\nerror → ${result.commentaryError}")

        val commentary = requireNotNull(result.commentary) { result.commentaryError ?: "해설 없음" }
        assertTrue(commentary.contains("장점") && commentary.contains("단점")) { commentary }
        assertTrue(commentary.contains("1팀") && commentary.contains("2팀")) { commentary }
    }

    @Test
    fun `tool 없는 완성은 준 글만으로 답한다`() {
        val completion = KoogLlmCompletionAdapter(executor, config.chatModel(), LlmGate(), properties)

        val answer = completion.complete("아래 사실만으로 한 문장으로 답한다.", "사실: 1팀 평균 Elo 1500, 2팀 평균 Elo 1480.\n질문: 어느 팀이 더 높은가?")
        println("completion → $answer")

        assertTrue(answer.contains("1팀")) { answer }
        assertEquals(0, calls.size)
    }
}
