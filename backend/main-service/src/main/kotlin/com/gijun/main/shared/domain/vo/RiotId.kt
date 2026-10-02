package com.gijun.main.shared.domain.vo

/**
 * Riot ID(`게임이름#태그`).
 *
 * 맨 `String` 으로 두면 챔피언 이름·모드·경기 ID 자리에 바꿔 넣어도 컴파일된다. 유즈케이스 입력과
 * 포트 조회에서는 이 타입으로 받고, JPA 엔티티·결과 DTO 같은 경계에서 `.value` 로 푼다.
 *
 * `#` 를 강제하지 않는다 — 태그 없이 저장된 과거 참가자 행이 있어 조회 자체를 막으면 안 된다.
 */
@JvmInline
value class RiotId(
    val value: String,
) : Comparable<RiotId> {
    init {
        require(value.isNotBlank()) { "RiotId must not be blank" }
    }

    override fun compareTo(other: RiotId): Int = value.compareTo(other.value)

    override fun toString(): String = value
}
