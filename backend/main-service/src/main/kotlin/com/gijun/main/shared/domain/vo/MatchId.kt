package com.gijun.main.shared.domain.vo

/** 경기 ID(`KR_7654321098` 형태). 내부 PK 가 아니라 Riot 이 준 자연키다. */
@JvmInline
value class MatchId(
    val value: String,
) : Comparable<MatchId> {
    init {
        require(value.isNotBlank()) { "MatchId must not be blank" }
    }

    override fun compareTo(other: MatchId): Int = value.compareTo(other.value)

    override fun toString(): String = value
}
