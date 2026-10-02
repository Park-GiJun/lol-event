package com.gijun.main.shared.domain.vo

/** Riot 계정의 불변 식별자. Riot ID 는 바뀔 수 있어도 PUUID 는 바뀌지 않는다. */
@JvmInline
value class Puuid(
    val value: String,
) : Comparable<Puuid> {
    init {
        require(value.isNotBlank()) { "Puuid must not be blank" }
    }

    override fun compareTo(other: Puuid): Int = value.compareTo(other.value)

    override fun toString(): String = value
}
