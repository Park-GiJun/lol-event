package com.gijun.main.shared.infrastructure.config

import com.gijun.main.application.dto.query.SearchRagDocumentsQuery
import com.gijun.main.application.dto.result.EloLeaderboardResult
import com.gijun.main.application.dto.result.RagDocumentResult
import com.gijun.main.application.port.`in`.DescribeChampionUseCase
import com.gijun.main.application.port.`in`.DescribePlayerUseCase
import com.gijun.main.application.port.`in`.GetEloLeaderboardUseCase
import com.gijun.main.application.port.`in`.SearchRagDocumentsUseCase
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
    private val config =
        KoogConfig(
            RagProperties(
                chat =
                    RagProperties.Chat(
                        baseUrl =
                            System.getenv("RAG_SMOKE_CHAT_URL") ?: RagProperties.Chat().baseUrl,
                    ),
            ),
        )
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
                            라인전에서 강했던 상대: 노틸러스(Nautilus) 상대로 8판 승률 62%.
                            라인전에서 약했던 상대(카운터): 레오나(Leona) 상대로 6판 승률 16%, 블리츠크랭크(Blitzcrank) 상대로 4판 승률 25%.
                            """.trimIndent()
                    }

                    override fun describeChampionAllies(name: String): String {
                        calls += "allies:$name"
                        return "징크스(Jinx) — 내전 30판. 같은 팀일 때 성적: 룰루(Lulu)와 7판 승률 71%, 레오나(Leona)와 5판 승률 40%."
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

    private val chat = KoogLlmChatAdapter(executor, config.chatModel(), LlmGate(), tools)

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
    fun `아군 챔피언 질문에 get_champion_allies 를 부른다`() {
        val answer = chat.answer(emptyList(), "우리 원딜이 징크스인데 서폿 뭐 하면 좋아?")
        println("answer → $answer\ncalls → $calls")

        assertTrue(calls.any { it.startsWith("allies:") }) { "get_champion_allies 를 부르지 않았다: $calls" }
        assertTrue(answer.contains("룰루")) { answer }
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

        assertTrue(answer.contains("탑")) { answer }
    }

    @Test
    fun `tool 없는 완성은 준 글만으로 답한다`() {
        val completion = KoogLlmCompletionAdapter(executor, config.chatModel(), LlmGate())

        val answer = completion.complete("아래 사실만으로 한 문장으로 답한다.", "사실: 1팀 평균 Elo 1500, 2팀 평균 Elo 1480.\n질문: 어느 팀이 더 높은가?")
        println("completion → $answer")

        assertTrue(answer.contains("1팀")) { answer }
        assertEquals(0, calls.size)
    }
}
