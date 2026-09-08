package com.gijun.main.infrastructure.adapter.out.persistence.match.adapter

import com.gijun.main.infrastructure.adapter.out.persistence.match.repository.MatchJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.match.repository.MatchParticipantJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

/**
 * 포지션 백필은 매치 전체를 훑기 때문에 갱신 대상이 참가자 수천 명까지 간다.
 * 예전에는 한 명당 UPDATE 한 방이라 한 트랜잭션에서 왕복이 그 수만큼 났다.
 * 배정 결과는 포지션 다섯 종뿐이므로 포지션별로 묶여 나가야 한다.
 */
class MatchPersistenceAdapterTest {

    /** updateAssignedPositionIn 호출만 받아 적는 기록기. */
    private class RecordedUpdate(val ids: List<Long>, val pos: String)

    private val recorded = mutableListOf<RecordedUpdate>()

    /**
     * JpaRepository 는 메서드가 많아 손으로 구현하기 번거롭고, Mockito 의 captor 는
     * Kotlin 의 non-null 파라미터에 null 을 물려 터진다. 프록시로 필요한 한 메서드만 받는다.
     */
    private val participantRepo: MatchParticipantJpaRepository = Proxy.newProxyInstance(
        MatchParticipantJpaRepository::class.java.classLoader,
        arrayOf(MatchParticipantJpaRepository::class.java),
    ) { _, method, args ->
        when (method.name) {
            "updateAssignedPositionIn" -> {
                @Suppress("UNCHECKED_CAST")
                val ids = (args[0] as Collection<Long>).toList()
                recorded += RecordedUpdate(ids, args[1] as String)
                ids.size
            }
            "toString" -> "FakeMatchParticipantJpaRepository"
            "hashCode" -> System.identityHashCode(this)
            "equals" -> false
            else -> error("테스트가 예상하지 못한 호출: ${method.name}")
        }
    } as MatchParticipantJpaRepository

    private val matchRepo: MatchJpaRepository = Proxy.newProxyInstance(
        MatchJpaRepository::class.java.classLoader,
        arrayOf(MatchJpaRepository::class.java),
    ) { _, method, _ -> error("테스트가 예상하지 못한 호출: ${method.name}") } as MatchJpaRepository

    private val adapter = MatchPersistenceAdapter(matchRepo, participantRepo)

    @Test
    fun `포지션별로 묶어서 포지션 종류만큼만 UPDATE 한다`() {
        val updates = buildMap {
            (1L..300L).forEach { put(it, "TOP") }
            (301L..600L).forEach { put(it, "JUNGLE") }
            (601L..900L).forEach { put(it, "MID") }
            (901L..1200L).forEach { put(it, "ADC") }
            (1201L..1500L).forEach { put(it, "SUPPORT") }
        }

        adapter.updateAssignedPositions(updates)

        // 참가자 1,500명 → UPDATE 5방.
        assertEquals(5, recorded.size)
        assertEquals(1_500, recorded.sumOf { it.ids.size })
        assertEquals(
            setOf("TOP", "JUNGLE", "MID", "ADC", "SUPPORT"),
            recorded.map { it.pos }.toSet(),
        )
    }

    @Test
    fun `묶인 id 목록이 실제로 그 포지션인 참가자들이다`() {
        val updates = mapOf(1L to "TOP", 2L to "MID", 3L to "TOP", 4L to "MID", 5L to "ADC")

        adapter.updateAssignedPositions(updates)

        assertEquals(3, recorded.size)
        val byPos = recorded.associate { it.pos to it.ids.toSet() }
        assertEquals(setOf(1L, 3L), byPos["TOP"])
        assertEquals(setOf(2L, 4L), byPos["MID"])
        assertEquals(setOf(5L), byPos["ADC"])
    }

    @Test
    fun `IN 목록이 길면 청크로 쪼개서 보낸다`() {
        // 파라미터 개수 한도가 있는 DB 에서 IN 목록이 통째로 나가면 터진다.
        val updates = (1L..2_500L).associateWith { "TOP" }

        adapter.updateAssignedPositions(updates)

        assertEquals(3, recorded.size)
        assertTrue(recorded.all { it.ids.size <= 1_000 }, "청크 크기가 상한을 넘었다")
        assertEquals(2_500, recorded.sumOf { it.ids.size })
        // 쪼개도 대상은 빠짐없이 다 나가야 한다.
        assertEquals((1L..2_500L).toSet(), recorded.flatMap { it.ids }.toSet())
    }

    @Test
    fun `갱신할 게 없으면 아무것도 안 부른다`() {
        adapter.updateAssignedPositions(emptyMap())
        assertTrue(recorded.isEmpty())
    }
}
