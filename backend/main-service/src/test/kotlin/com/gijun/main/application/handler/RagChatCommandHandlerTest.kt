package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.AskChatCommand
import com.gijun.main.application.port.out.external.LlmChatPort
import com.gijun.main.domain.rag.enums.ChatRole
import com.gijun.main.domain.rag.exception.InvalidChatRequestException
import com.gijun.main.domain.rag.model.ChatMessageModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RagChatCommandHandlerTest {
    private val llm = RecordingLlm()
    private val handler = RagChatCommandHandler(llm)

    @Test
    fun `마지막 질문과 그 앞의 대화를 나눠 넘긴다`() {
        val result =
            handler.askChat(
                AskChatCommand(listOf(user("아랑택 전적"), assistant("113판입니다"), user("  주 포지션은?  "))),
            )

        assertEquals("답", result.reply)
        assertEquals("주 포지션은?", llm.question)
        assertEquals(listOf("아랑택 전적", "113판입니다"), llm.history.map { it.content })
    }

    @Test
    fun `오래된 대화는 잘라낸다`() {
        // 모델의 컨텍스트는 tool 결과가 대부분 쓴다. 대화가 길어져도 최근 것만 본다.
        val long = (1..30).flatMap { listOf(user("질문$it"), assistant("답$it")) } + user("마지막")

        handler.askChat(AskChatCommand(long))

        assertEquals(8, llm.history.size)
        assertEquals("답30", llm.history.last().content)
    }

    @Test
    fun `지난 메시지가 길면 앞부분만 넘긴다`() {
        handler.askChat(AskChatCommand(listOf(user("x".repeat(5_000)), assistant("네"), user("질문"))))

        assertTrue(
            llm.history
                .first()
                .content.length <= 1_500,
        )
    }

    @Test
    fun `질문이 아닌 것은 LLM 에 보내지 않고 거절한다`() {
        assertInvalid(AskChatCommand(emptyList()))
        assertInvalid(AskChatCommand(listOf(user("   "))))
        assertInvalid(AskChatCommand(listOf(user("질문"), assistant("답"))))
        assertInvalid(AskChatCommand(listOf(user("x".repeat(1_001)))))
        assertEquals(null, llm.question)
    }

    private fun assertInvalid(command: AskChatCommand) {
        assertThrows(InvalidChatRequestException::class.java) { handler.askChat(command) }
    }

    private fun user(content: String) = ChatMessageModel(ChatRole.USER, content)

    private fun assistant(content: String) = ChatMessageModel(ChatRole.ASSISTANT, content)

    private class RecordingLlm : LlmChatPort {
        var history: List<ChatMessageModel> = emptyList()
        var question: String? = null

        override fun answer(
            history: List<ChatMessageModel>,
            question: String,
        ): String {
            this.history = history
            this.question = question
            return " 답 "
        }
    }
}
