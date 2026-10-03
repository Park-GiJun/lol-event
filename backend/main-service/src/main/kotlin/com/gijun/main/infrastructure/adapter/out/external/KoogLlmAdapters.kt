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
            - "아군이 X 일 때 뭘 고를까": get_champion_allies 로 X 와 같은 팀일 때 성적이 좋은 챔피언을 본다.
            - "X 를 상대로 뭘 고를까"(카운터 픽): get_champion 의 "약했던 상대" 를 본다.
            - 순위: get_elo_ranking
            - 누구인지 모르거나 넓은 질문("라인전 강한 탑", "최근 경기"): search_knowledge

            ## 답하는 법
            - 한국어로, 짧게. 근거가 된 숫자를 같이 적는다.
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
