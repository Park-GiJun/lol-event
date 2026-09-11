package com.gijun.main.domain.service

/**
 * riotId 정규화. 같은 사람이 여러 이름으로 잡히는 것을 한 줄로 합친다.
 *
 * 내전 참가자는 계정을 바꾸거나 이름을 바꾼다. 그때마다 리더보드에 새 사람이 하나 생기고,
 * 양쪽 다 표본이 쪼개져 배치 구간에 머문다. 그래서 **레이팅을 키로 잡는 모든 지점**에서
 * 이 레이어를 통과시킨다.
 *
 * 별명 표는 코드가 아니라 설정(`riot-id.aliases`)에 둔다. 참가자 명단은 운영 중에 계속 바뀌는
 * 값이라, 이름 하나 합치자고 배포를 다시 하는 구조는 오래 못 간다.
 *
 * 표는 `별명 -> 정규 이름` 방향이다. 체인(A->B, B->C)은 따라가되 순환은 원본을 그대로 돌려준다 —
 * 설정 실수로 서버가 멈추는 것보다 낫다.
 */
class RiotIdNormalizer(aliases: Map<String, String>) {

    /** 조회는 대소문자·앞뒤 공백을 무시한다. 표 자체는 사람이 읽을 원형으로 둔다. */
    private val table: Map<String, String> =
        aliases.entries.associate { (from, to) -> key(from) to to.trim() }

    fun canonical(riotId: String): String {
        val trimmed = riotId.trim()
        if (table.isEmpty()) return trimmed

        var current = trimmed
        // 체인 깊이 상한. 순환이면 상한에서 멈추고 마지막 값을 쓴다.
        repeat(MAX_CHAIN) {
            val next = table[key(current)] ?: return current
            if (next == current) return current
            current = next
        }
        return current
    }

    /** 목록 전체를 정규화하면서 같은 사람이 두 번 나오면 하나로 접는다. */
    fun canonicalDistinct(riotIds: Collection<String>): List<String> =
        riotIds.map(::canonical).distinct()

    private fun key(s: String) = s.trim().lowercase()

    private companion object {
        const val MAX_CHAIN = 8
    }
}
