package com.gijun.main.domain.model.match

data class MatchBan(
    val championId: Int,
    val championName: String,
    val pickTurn: Int = 0,
)
