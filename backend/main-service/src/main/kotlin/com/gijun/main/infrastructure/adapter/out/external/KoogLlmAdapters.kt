package com.gijun.main.infrastructure.adapter.out.external

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import com.gijun.main.application.port.out.external.LlmChatPort
import com.gijun.main.application.port.out.external.LlmCompletionPort
import com.gijun.main.domain.rag.enums.ChatRole
import com.gijun.main.domain.rag.exception.RagUnavailableException
import com.gijun.main.domain.rag.model.ChatMessageModel
import com.gijun.main.infrastructure.adapter.`in`.agent.LolAgentTools
import com.gijun.main.shared.infrastructure.config.KoogConfig
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/** tool 없이 글만 받는다. */
@Component
class KoogLlmCompletionAdapter(
    private val promptExecutor: PromptExecutor,
    @Qualifier(KoogConfig.CHAT) private val chatModel: LLModel,
    private val gate: LlmGate,
) : LlmCompletionPort {
    override fun complete(
        system: String,
        user: String,
    ): String =
        gate.enter {
            runBlocking {
                reachingLlm {
                    promptExecutor
                        .execute(
                            prompt =
                                prompt("completion") {
                                    system(system)
                                    user(user)
                                },
                            model = chatModel,
                            tools = emptyList(),
                        ).textContent()
                }
            }
        }
}

/**
 * 질문 하나에 에이전트 하나.
 *
 * 에이전트는 "모델 호출 → tool 호출이 오면 실행해 결과를 돌려줌 → 다시 모델 호출" 을 답이 글로 나올
 * 때까지 돈다. 그 고리는 Koog 가 돌리고, 여기서는 지난 대화와 tool 목록을 쥐여 준다.
 *
 * 에이전트는 한 번 쓰고 버린다. 대화를 서버가 기억하지 않으므로 다시 쓸 상태가 없다.
 */
@Component
class KoogLlmChatAdapter(
    private val promptExecutor: PromptExecutor,
    @Qualifier(KoogConfig.CHAT) private val chatModel: LLModel,
    private val gate: LlmGate,
    tools: LolAgentTools,
) : LlmChatPort {
    private val toolRegistry = ToolRegistry { tools(tools) }

    override fun answer(
        history: List<ChatMessageModel>,
        question: String,
    ): String =
        gate.enter {
            val agent =
                AIAgent(
                    promptExecutor = promptExecutor,
                    agentConfig =
                        AIAgentConfig(
                            prompt =
                                prompt("lol-chat") {
                                    system(SYSTEM)
                                    history.forEach { message ->
                                        when (message.role) {
                                            ChatRole.USER -> user(message.content)
                                            ChatRole.ASSISTANT -> assistant(message.content)
                                        }
                                    }
                                },
                            model = chatModel,
                            maxAgentIterations = MAX_ITERATIONS,
                        ),
                    toolRegistry = toolRegistry,
                )
            reachingLlm { runBlocking { agent.run(question) } }
        }

    private companion object {
        /** 모델 호출과 tool 실행을 합친 걸음 수의 상한. tool 을 서너 번 부르는 질문까지 넉넉하다. */
        const val MAX_ITERATIONS = 30

        val SYSTEM =
            """
            너는 리그 오브 레전드 내전(친구들끼리 하는 5:5 사설 경기) 기록을 안내하는 도우미다.

            ## 규칙
            - 전적·승률·Elo·판수 같은 숫자는 반드시 tool 이 준 값만 쓴다. 기억이나 추측으로 숫자를 말하지 않는다.
            - 플레이어나 챔피언에 대한 질문은 먼저 tool 로 내전 기록을 확인한다.
            - 이어지는 질문("그 사람은?", "그럼 주 포지션은?")도 마찬가지다. 지난 대화에 그 값이 글자 그대로 나와 있지 않으면
              누구를 가리키는지 대화에서 찾아 tool 을 다시 부른다. 절대 짐작으로 채우지 않는다.
            - 내전 기록은 표본이 작다. 판수가 적으면(5 판 미만) "표본이 적다" 고 같이 말한다.
            - 내전 기록에 없는 내용은 없다고 말한다. 일반적인 롤 지식으로 보태 답할 때는 "일반적으로는" 이라고 밝혀
              내전 기록과 구분한다.
            - tool 이 "찾지 못했다" 고 하면 이름을 다시 확인해 달라고 한다. 비슷한 이름을 지어내지 않는다.

            ## 어떤 tool 을 쓰나
            - 특정 플레이어: get_player
            - 특정 챔피언(누가 하는지, 승률, 라인 상성·카운터): get_champion
            - "X 잘하는 사람이 누구야": get_champion 의 "많이 한 사람" 을 본다. 거기 나온 사람만 답한다. search_knowledge 로 찾지 않는다.
            - "아군이 X 일 때 뭘 고를까": get_champion_allies 로 X 와 같은 팀일 때 성적이 좋은 챔피언을 본다.
            - "X 를 상대로 뭘 고를까"(카운터 픽): get_champion 의 "약했던 상대" 를 본다.
            - 라인만 정해진 챔피언 질문("미드에서 승률 높은 챔피언", "탑 뭐가 좋아"): get_lane_champions. 한 번만 부르면 된다 —
              챔피언을 하나씩 get_champion 으로 뒤지지 않는다.
            - 플레이어 순위: get_elo_ranking
            - 누구인지 모르거나 넓은 질문("라인전 강한 탑", "최근 경기"): search_knowledge

            ## 답하는 법
            - 챔피언 이름은 tool 이 준 한글 이름을 글자 그대로 쓴다("아리(Ahri)" 면 "아리"). 영문 이름을 소리 나는 대로
              한글로 옮겨 적지 않는다. tool 이 한글 이름을 주지 않았으면 영문 그대로 쓴다.
            - 질문의 조건에 맞는 것만 답에 넣는다. 조건에 맞지 않는 사람이나 챔피언을 "~는 해당 없다" 는 식으로 끼워 넣지 않는다.
            - 물은 것만 답한다. 라인을 물으면 그 라인 순위만 적고, 다른 라인이나 전체 챔피언 목록을 앞에 늘어놓지 않는다.
            - 한국어로 쓴다. 모든 주장에 근거가 된 수치(판수, 승률, 순위, Elo)를 괄호로 붙인다. 수치를 댈 수 없는 말은 쓰지 않는다.
            - 플레이어나 챔피언이 어떤지 물으면 이 형식으로 답한다:
              **한 줄 요약** (전적과 주 포지션)
              **장점** — tool 이 준 "강점" 을 수치와 함께 두세 가지
              **단점** — tool 이 준 "약점" 을 수치와 함께 두세 가지
              필요하면 **추천** 한 줄 (어느 포지션·챔피언이 좋은지, 근거와 함께)
            - 강점·약점은 tool 이 이미 가려 준 것만 쓴다. 다른 숫자를 보고 스스로 "잘한다/못한다" 를 판정하지 않는다.
            - 챔피언 픽을 물으면 후보마다 판수와 승률을 같이 적고, 판수가 많은 것을 앞에 둔다.
            - 단순한 사실 질문("몇 판 했어?")에는 형식 없이 한두 문장으로 답한다.
            - 경기 모드는 일반(소환사의 협곡)만 다룬다. 칼바람 나락 기록은 여기 없다.
            """.trimIndent()
    }
}

/**
 * LLM 호출이 실패하면(장비가 꺼짐, 시간 초과, 모델이 깨진 답을 냄) 원인을 감싸 한 가지 예외로 올린다.
 * 그대로 두면 500 이 나가고 화면은 "서버 오류" 라고만 말한다. 이건 서버가 아니라 LLM 장비의 사정이다.
 */
private inline fun <T> reachingLlm(block: () -> T): T =
    try {
        block()
    } catch (e: Exception) {
        throw RagUnavailableException(e)
    }
