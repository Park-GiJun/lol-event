package com.gijun.main.infrastructure.adapter.`in`.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * **응답 DTO 가 결과 DTO 와 어긋나지 않게 잡는다.**
 *
 * 웹 계층은 application result 를 그대로 내보내지 않고 `*Response` 로 옮겨 담는다. 그 대가로
 * 같은 필드 목록이 두 벌이 됐다. 결과 DTO 에 필드를 더하고 응답 DTO 를 빠뜨리면 **컴파일은 되고
 * 화면에는 그 값이 조용히 안 나온다** — `from()` 이 named argument 라 누락이 오류가 아니기 때문이다.
 *
 * 화면과 수집기는 필드 이름으로 읽으므로 이름·순서가 같으면 와이어 포맷도 같다.
 */
class ResponseDtoContractTest {
    private val results = dataClassesUnder("com/gijun/main/application/dto/result")
    private val responses = dataClassesUnder("com/gijun/main/infrastructure/adapter/in/web").associateBy { it.simpleName }

    @Test
    fun `모든 결과 DTO 에 짝이 되는 응답 DTO 가 있다`() {
        assertTrue(results.size > MIN_EXPECTED_RESULTS, "결과 DTO 를 못 찾았다 — 스캔 경로를 확인한다 (${results.size} 개)")

        val missing =
            results
                .mapNotNull { it.simpleName }
                .filterNot { it in INTERNAL }
                .filterNot { responseNameOf(it) in responses }
        assertTrue(missing.isEmpty(), "응답 DTO 가 없는 결과 DTO: $missing")
    }

    @Test
    fun `응답 DTO 의 필드는 결과 DTO 와 이름과 순서가 같다`() {
        val drifted =
            results.mapNotNull { result ->
                val name = result.simpleName ?: return@mapNotNull null
                val response = responses[responseNameOf(name)] ?: return@mapNotNull null
                val expected = fieldNames(result)
                val actual = fieldNames(response)
                if (expected == actual) null else "$name: 결과 $expected / 응답 $actual"
            }
        assertEquals(emptyList<String>(), drifted)
    }

    private fun fieldNames(type: KClass<*>): List<String?> =
        checkNotNull(type.primaryConstructor) { "주 생성자가 없다: ${type.simpleName}" }.parameters.map { it.name }

    private fun responseNameOf(resultName: String): String = SPECIAL[resultName] ?: (resultName.removeSuffix("Result") + "Response")

    private fun dataClassesUnder(path: String): List<KClass<*>> =
        PathMatchingResourcePatternResolver()
            .getResources("classpath*:$path/**/*.class")
            .map { it.url.path }
            .filterNot { it.contains('$') }
            .map {
                it
                    .substringAfterLast("/classes/kotlin/main/")
                    .substringAfterLast("/classes/kotlin/test/")
                    .removeSuffix(".class")
                    .replace('/', '.')
            }.filter { it.startsWith("com.gijun.main") }
            .map { Class.forName(it).kotlin }
            .filter { it.isData }

    private companion object {
        /** 결과 DTO 는 150 개쯤이다. 스캔이 조용히 0 개를 돌려주면 위 검사가 전부 통과해 버린다. */
        private const val MIN_EXPECTED_RESULTS = 100

        /**
         * 웹으로 나가지 않는 결과. 배치와 에이전트 tool 이 프로세스 안에서만 쓴다.
         * 웹 어댑터가 내보내게 되면 여기서 빼고 응답 DTO 를 만든다.
         */
        private val INTERNAL =
            setOf(
                "IndexRagDocumentResult",
                "RagDocumentResult",
                "RagIndexSummaryResult",
                "ChampionSynergyResult",
                "AllyChampionStat",
                "AllyPickStat",
            )

        /** 규칙대로 지으면 이름이 겹치거나 어색해지는 것들. */
        private val SPECIAL =
            mapOf(
                "SummonerProfile" to "SummonerProfileCardResponse",
                "MapPointDto" to "MapPointResponse",
            )
    }
}
