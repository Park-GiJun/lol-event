package com.gijun.main.domain.match.enums

/**
 * 통계를 낼 경기 범위.
 *
 * 예전에는 `mode: String` 이었고 모르는 값은 조용히 [NORMAL] 로 떨어졌다. 오타 난 `mode=arm` 이
 * 협곡 통계를 돌려주면 호출자는 틀린 줄 모른다. 값이 유한하므로 enum 으로 닫고, 경계에서
 * 파싱에 실패하면 400 으로 돌려준다.
 */
enum class GameMode(
    /** 와이어 포맷이자 캐시 키·스냅샷 테이블의 `mode` 컬럼 값. */
    val key: String,
    val queueIds: List<Int>,
) {
    /** 5v5 내전(소환사의 협곡). */
    NORMAL("normal", listOf(0, 3130)),

    /** 칼바람 나락. */
    ARAM("aram", listOf(3270)),

    /**
     * **이름과 달리 칼바람을 포함하지 않는다** — [NORMAL] 과 같은 범위다. 칼바람은 통계 집계에서
     * 항상 뺀다. 이미 배포된 화면과 수집기가 `all` 을 보내고 있어 값은 남겨 둔다.
     */
    ALL("all", listOf(0, 3130)),
    ;

    companion object {
        /** 대소문자와 앞뒤 공백을 가리지 않는다. 모르는 값이면 null. */
        fun parse(value: String): GameMode? = entries.firstOrNull { it.key.equals(value.trim(), ignoreCase = true) }
    }
}
