package com.gijun.lolml.data

import com.gijun.lolml.TEAM_A
import com.gijun.lolml.TEAM_B
import com.gijun.lolml.extract.MatchExtractor
import com.gijun.lolml.match
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotAndCleanerTest {
    private val cleaner = MatchCleaner(minDurationSec = 600, queueIds = setOf(0, 3130))

    @Test
    fun `쓴 것을 읽으면 같은 경기가 같은 순서로 나온다`(
        @TempDir dir: Path,
    ) {
        val matches =
            listOf(
                match("m2", TEAM_A, TEAM_B, blueWin = true, gameCreation = 10),
                match("m1", TEAM_B, TEAM_A, blueWin = false, gameCreation = 20),
            )
        val path = dir.resolve("nested").resolve("matches.tsv")

        SnapshotWriter(path).write(matches.asSequence())

        assertEquals(matches, SnapshotReader(path).read())
    }

    @Test
    fun `멀쩡한 경기는 순서대로 남는다`() {
        val matches =
            listOf(
                match("m1", TEAM_A, TEAM_B, blueWin = true, gameCreation = 1),
                match("m2", TEAM_A, TEAM_B, blueWin = false, gameCreation = 2, queueId = 3130),
            )
        assertEquals(listOf("m1", "m2"), cleaner.clean(matches).map { it.matchId })
    }

    @Test
    fun `다른 큐와 짧은 경기와 5대5 가 아닌 경기는 뺀다`() {
        val matches =
            listOf(
                match("aram", TEAM_A, TEAM_B, blueWin = true, gameCreation = 1, queueId = 3270),
                match("remake", TEAM_A, TEAM_B, blueWin = true, gameCreation = 2, durationSec = 200),
                match("4v5", TEAM_A.take(4), TEAM_B, blueWin = true, gameCreation = 3),
                match("same-person-twice", TEAM_A, TEAM_A, blueWin = true, gameCreation = 4),
            )
        assertEquals(emptyList(), cleaner.clean(matches))
    }

    @Test
    fun `두 번 수집된 경기는 한 번만 남긴다`() {
        val matches =
            listOf(
                match("m1", TEAM_A, TEAM_B, blueWin = true, gameCreation = 1),
                match("m1", TEAM_A, TEAM_B, blueWin = true, gameCreation = 1),
                // match_id 는 다르지만 시작 시각과 열 명이 같다.
                match("m1-again", TEAM_A, TEAM_B, blueWin = true, gameCreation = 1),
            )
        assertEquals(listOf("m1"), cleaner.clean(matches).map { it.matchId })
    }

    @Test
    fun `포지션은 표기가 달라도 같은 값으로 읽고 비어 있으면 UNKNOWN 이다`() {
        assertEquals(Position.ADC, MatchExtractor.position("BOTTOM"))
        assertEquals(Position.MID, MatchExtractor.position("middle"))
        assertEquals(Position.UNKNOWN, MatchExtractor.position(""))
        assertEquals(Position.UNKNOWN, MatchExtractor.position(null))
    }
}
