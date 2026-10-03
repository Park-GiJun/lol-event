package com.gijun.main.infrastructure.adapter.out.persistence.rag

/**
 * pgvector 의 글자 표기. JDBC 드라이버는 `vector` 타입을 모르므로 글자로 넘기고 SQL 에서
 * `?::public.vector` 로 바꾼다.
 */
internal object PgVector {
    fun literal(values: List<Float>): String {
        require(values.isNotEmpty()) { "빈 벡터는 저장할 수 없다" }
        require(values.all { it.isFinite() }) { "벡터에 NaN 이나 무한대가 있다" }
        return values.joinToString(separator = ",", prefix = "[", postfix = "]")
    }
}
