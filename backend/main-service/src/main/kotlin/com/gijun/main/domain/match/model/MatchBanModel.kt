package com.gijun.main.domain.match.model

data class MatchBanModel(
    val championId: Int,
    val championName: String,
    val pickTurn: Int = 0,
)
