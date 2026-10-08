package com.gijun.lolml.data

import java.nio.file.Files
import java.nio.file.Path

/**
 * 스냅샷 파일의 형식을 아는 곳은 이 파일뿐이다. 쓰는 쪽과 읽는 쪽이 같은 컬럼 순서를 본다.
 *
 * 참가자 한 명이 한 줄이고 경기 컬럼은 줄마다 반복된다. 탭으로 가른다 — 챔피언 이름이나
 * Riot ID 에 쉼표가 들어가도 깨지지 않는다.
 */
object SnapshotFormat {
    val DEFAULT_PATH: Path = Path.of("data", "matches.tsv")

    const val SEPARATOR = "\t"

    val COLUMNS =
        listOf(
            "matchId",
            "queueId",
            "gameCreation",
            "gameDurationSec",
            "playerId",
            "team",
            "position",
            "champion",
            "win",
            "kills",
            "deaths",
            "assists",
            "gold",
            "damageSelfMitigated",
        )
}

class SnapshotWriter(
    private val path: Path,
) {
    /** 받은 순서 그대로 쓴다. 정렬은 SQL 이 이미 했다. */
    fun write(matches: Sequence<Match>) {
        path.parent?.let { Files.createDirectories(it) }
        Files.newBufferedWriter(path).use { out ->
            out.appendLine(SnapshotFormat.COLUMNS.joinToString(SnapshotFormat.SEPARATOR))
            for (match in matches) {
                for (p in match.participants) {
                    val row =
                        listOf(
                            match.matchId,
                            match.queueId,
                            match.gameCreation,
                            match.gameDurationSec,
                            p.playerId,
                            p.team,
                            p.position,
                            p.champion,
                            p.win,
                            p.kills,
                            p.deaths,
                            p.assists,
                            p.gold,
                            p.damageSelfMitigated,
                        )
                    out.appendLine(row.joinToString(SnapshotFormat.SEPARATOR) { clean(it.toString()) })
                }
            }
        }
    }

    private fun clean(value: String): String = value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
}

class SnapshotReader(
    private val path: Path,
) {
    /** 파일에 적힌 순서(= 시간순)대로 돌려준다. */
    fun read(): List<Match> {
        // LinkedHashMap 이라 경기가 처음 나온 순서가 그대로 남는다.
        val rowsByMatch = LinkedHashMap<String, MutableList<List<String>>>()
        Files.newBufferedReader(path).useLines { lines ->
            lines
                .drop(1)
                .filter { it.isNotBlank() }
                .map { it.split(SnapshotFormat.SEPARATOR) }
                .forEach { cols ->
                    require(cols.size == SnapshotFormat.COLUMNS.size) { "컬럼 수가 다른 줄: $cols" }
                    rowsByMatch.getOrPut(cols[0]) { ArrayList() }.add(cols)
                }
        }
        return rowsByMatch.values.map { rows ->
            val head = rows.first()
            Match(
                matchId = head[0],
                queueId = head[1].toInt(),
                gameCreation = head[2].toLong(),
                gameDurationSec = head[3].toInt(),
                participants =
                    rows.map { c ->
                        Participant(
                            playerId = c[4],
                            team = Team.valueOf(c[5]),
                            position = Position.valueOf(c[6]),
                            champion = c[7],
                            win = c[8].toBoolean(),
                            kills = c[9].toInt(),
                            deaths = c[10].toInt(),
                            assists = c[11].toInt(),
                            gold = c[12].toInt(),
                            damageSelfMitigated = c[13].toInt(),
                        )
                    },
            )
        }
    }
}
