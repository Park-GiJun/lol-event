package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.command.AskChatCommand
import com.gijun.main.application.dto.result.ChatAnswerResult

interface AskChatUseCase {
    /** 대화의 마지막 질문에 답한다. 답 하나가 수십 초 걸릴 수 있다. */
    fun askChat(command: AskChatCommand): ChatAnswerResult
}
