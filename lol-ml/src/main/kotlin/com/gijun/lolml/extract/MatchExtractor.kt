package com.gijun.lolml.extract

import com.gijun.lolml.data.Match
import com.gijun.lolml.data.Participant
import com.gijun.lolml.data.Position
import com.gijun.lolml.data.Team
import java.sql.DriverManager
import java.sql.ResultSet

/**
 * `lol_event.matches` ⨝ `lol_event.match_participants` 를 시간순으로 읽는다.
 * 가공 테이블(`player_elo`, `*_stats_snapshot`)은 읽지 않는다.
 */
class MatchExtractor(
    private val config: DbConfig,
) {
    /**
     * 정렬은 `ORDER BY game_creation, match_id` 로 SQL 에서 동점까지 고정한다.
     * PostgreSQL 은 `autoCommit = false` + `fetchSize` 여야 스트리밍된다.
     * 참가자 행 10개를 경기 하나로 묶어 [onMatch] 에 넘긴다.
     */
    fun extract(onMatch: (Match) -> Unit) {
        DriverManager.getConnection(config.jdbcUrl, config.user, config.password).use { connection ->
            connection.isReadOnly = true
            connection.autoCommit = false
            connection.prepareStatement(SQL).use { statement ->
                statement.fetchSize = FETCH_SIZE
                statement.executeQuery().use { rows ->
                    var current: MatchHeader? = null
                    var participants = ArrayList<Participant>()
                    while (rows.next()) {
                        val header = header(rows)
                        if (current != null && current.matchId != header.matchId) {
                            onMatch(current.toMatch(participants))
                            participants = ArrayList()
                        }
                        current = header
                        participants.add(participant(rows))
                    }
                    current?.let { onMatch(it.toMatch(participants)) }
                }
            }
        }
    }

    private fun header(rows: ResultSet) =
        MatchHeader(
            matchId = rows.getString("match_id"),
            queueId = rows.getInt("queue_id"),
            gameCreation = rows.getLong("game_creation"),
            gameDurationSec = rows.getInt("game_duration"),
        )

    private fun participant(rows: ResultSet) =
        Participant(
            // puuid 는 Riot ID 를 바꿔도 그대로다. 비어 있는 옛 행만 riot_id 로 대신한다.
            playerId = rows.getString("puuid")?.takeIf { it.isNotBlank() } ?: rows.getString("riot_id"),
            team = if (rows.getInt("team_id") == BLUE_TEAM_ID) Team.BLUE else Team.RED,
            position = position(rows.getString("assigned_position")),
            champion = rows.getString("champion"),
            win = rows.getBoolean("win"),
            kills = rows.getInt("kills"),
            deaths = rows.getInt("deaths"),
            assists = rows.getInt("assists"),
            gold = rows.getInt("gold"),
            damageSelfMitigated = rows.getInt("damage_self_mitigated"),
        )

    private class MatchHeader(
        val matchId: String,
        val queueId: Int,
        val gameCreation: Long,
        val gameDurationSec: Int,
    ) {
        fun toMatch(participants: List<Participant>) = Match(matchId, queueId, gameCreation, gameDurationSec, participants)
    }

    companion object {
        private const val FETCH_SIZE = 1000
        private const val BLUE_TEAM_ID = 100

        private val SQL =
            """
            SELECT m.match_id, m.queue_id, m.game_creation, m.game_duration,
                   p.puuid, p.riot_id, p.team_id, p.assigned_position, p.champion,
                   p.win, p.kills, p.deaths, p.assists, p.gold, p.damage_self_mitigated
            FROM lol_event.matches m
            JOIN lol_event.match_participants p ON p.match_db_id = m.id
            ORDER BY m.game_creation, m.match_id, p.id
            """.trimIndent()

        /** `assigned_position` 은 비어 있을 수 있고, 원딜·미드는 표기가 둘이다. */
        fun position(raw: String?): Position =
            when (raw?.trim()?.uppercase()) {
                "TOP" -> Position.TOP
                "JUNGLE" -> Position.JUNGLE
                "MID", "MIDDLE" -> Position.MID
                "ADC", "BOTTOM" -> Position.ADC
                "SUPPORT", "UTILITY" -> Position.SUPPORT
                else -> Position.UNKNOWN
            }
    }
}
