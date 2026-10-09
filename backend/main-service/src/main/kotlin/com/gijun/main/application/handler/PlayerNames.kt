package com.gijun.main.application.handler

/**
 * 사람이 쓴 이름으로 Riot ID 를 찾는다. 사람은 "아랑택" 이라고 쓰지 "아랑택#아랑택" 이라고 쓰지 않는다.
 */
internal object PlayerNames {
    /** 전체 일치 → '#' 앞부분 일치 → 포함 순으로 좁힌다. 띄어쓰기와 대소문자는 보지 않는다. */
    fun match(
        name: String,
        known: List<String>,
    ): List<String> {
        val wanted = normalize(name)
        if (wanted.isEmpty()) return emptyList()
        return known.filter { normalize(it) == wanted }.ifEmpty {
            known.filter { normalize(it.substringBefore('#')) == wanted }.ifEmpty {
                known.filter { normalize(it).contains(wanted) }
            }
        }
    }

    fun normalize(text: String) = text.filterNot { it.isWhitespace() }.lowercase()
}
