package com.gijun.main.application.port.out.external

import com.gijun.main.domain.rag.model.ChatMessageModel

/**
 * 글을 주고 글을 받는다. tool 은 없다 — 넘긴 사실만으로 쓰게 할 때 쓴다(팀 편성 해설).
 */
interface LlmCompletionPort {
    fun complete(
        system: String,
        user: String,
    ): String
}

/**
 * 질문에 답한다. 모델이 필요하다고 판단하면 통계 조회·문서 검색 tool 을 스스로 부른다.
 *
 * 숫자를 모델이 지어내지 않게 하려는 구조다 — 전적·승률·Elo 는 tool 이 준 값만 쓴다.
 */
interface LlmChatPort {
    /**
     * @param history 이번 질문 **이전**의 대화. 오래된 것부터.
     * @param question 이번 질문.
     */
    fun answer(
        history: List<ChatMessageModel>,
        question: String,
    ): String
}
