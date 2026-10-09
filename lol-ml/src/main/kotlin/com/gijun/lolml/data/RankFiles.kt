package com.gijun.lolml.data

import java.nio.file.Files
import java.nio.file.Path

/** 랭크 큐 하나의 티어. 마스터 이상은 단계가 없어 [division] 이 늘 `I` 이다. */
data class QueueRank(
    val tier: String,
    val division: String,
    val lp: Int,
    val wins: Int,
    val losses: Int,
)

/**
 * 한 사람의 랭크 게임 티어. **조회한 날([fetchedAt])의 값이다** — 과거 어느 날의 티어는 Riot API 가 주지 않는다.
 * 그래서 이 값을 그 전 경기에 쓰면 미래 정보다. 알고 쓴다(lol-ml/CLAUDE.md 의 데이터 원칙 예외).
 */
data class PlayerRank(
    val playerId: String,
    val riotId: String,
    /** Riot API 에서 계정을 찾았나. Riot ID 를 바꾼 사람은 못 찾는다. */
    val found: Boolean,
    val solo: QueueRank?,
    val flex: QueueRank?,
    /** epoch millis */
    val fetchedAt: Long,
)

object RankFormat {
    val DEFAULT_PATH: Path = Path.of("data", "ranks.tsv")

    const val SEPARATOR = "\t"

    private val QUEUE_COLUMNS = listOf("Tier", "Division", "Lp", "Wins", "Losses")

    val COLUMNS = listOf("playerId", "riotId", "found") + QUEUE_COLUMNS.map { "solo$it" } + QUEUE_COLUMNS.map { "flex$it" } + "fetchedAt"

    val QUEUE_WIDTH = QUEUE_COLUMNS.size
}

class RankWriter(
    private val path: Path,
) {
    fun write(ranks: List<PlayerRank>) {
        path.parent?.let { Files.createDirectories(it) }
        Files.newBufferedWriter(path).use { out ->
            out.appendLine(RankFormat.COLUMNS.joinToString(RankFormat.SEPARATOR))
            for (rank in ranks) {
                val row = listOf(rank.playerId, rank.riotId, rank.found) + queue(rank.solo) + queue(rank.flex) + rank.fetchedAt
                out.appendLine(row.joinToString(RankFormat.SEPARATOR) { clean(it.toString()) })
            }
        }
    }

    /** 배치를 안 본 큐는 빈칸으로 둔다. */
    private fun queue(rank: QueueRank?): List<Any> =
        if (rank == null) List(RankFormat.QUEUE_WIDTH) { "" } else listOf(rank.tier, rank.division, rank.lp, rank.wins, rank.losses)

    private fun clean(value: String): String = value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
}

class RankReader(
    private val path: Path,
) {
    /** @return 사람 키 → 티어 */
    fun read(): Map<String, PlayerRank> =
        Files.newBufferedReader(path).useLines { lines ->
            lines
                .drop(1)
                .filter { it.isNotBlank() }
                .map { it.split(RankFormat.SEPARATOR) }
                .associate { cols ->
                    require(cols.size == RankFormat.COLUMNS.size) { "컬럼 수가 다른 줄: $cols" }
                    cols[0] to
                        PlayerRank(
                            playerId = cols[0],
                            riotId = cols[1],
                            found = cols[2].toBoolean(),
                            solo = queue(cols, SOLO_AT),
                            flex = queue(cols, SOLO_AT + RankFormat.QUEUE_WIDTH),
                            fetchedAt = cols.last().toLong(),
                        )
                }
        }

    private fun queue(
        cols: List<String>,
        from: Int,
    ): QueueRank? =
        if (cols[from].isEmpty()) {
            null
        } else {
            QueueRank(cols[from], cols[from + 1], cols[from + 2].toInt(), cols[from + 3].toInt(), cols[from + 4].toInt())
        }

    private companion object {
        const val SOLO_AT = 3
    }
}
