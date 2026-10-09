package com.gijun.lolml.data

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class RankFilesTest {
    @Test
    fun `쓴 것을 그대로 읽는다 - 배치를 안 본 큐와 못 찾은 계정까지`() {
        val path = Files.createTempFile("ranks", ".tsv")
        val ranks =
            listOf(
                PlayerRank("p1", "가 나#KR1", true, QueueRank("EMERALD", "II", 41, 120, 111), null, 1_000L),
                PlayerRank("p2", "b#2", true, null, QueueRank("GOLD", "IV", 0, 3, 2), 1_000L),
                PlayerRank("p3", "c#3", true, null, null, 1_000L),
                PlayerRank("p4", "바뀐이름#KR1", false, null, null, 1_000L),
            )

        RankWriter(path).write(ranks)

        assertEquals(ranks.associateBy { it.playerId }, RankReader(path).read())
    }
}
