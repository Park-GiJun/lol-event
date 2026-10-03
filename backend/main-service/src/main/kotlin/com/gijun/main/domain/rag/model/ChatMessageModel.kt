package com.gijun.main.domain.rag.model

import com.gijun.main.domain.rag.enums.ChatRole

data class ChatMessageModel(
    val role: ChatRole,
    val content: String,
)
