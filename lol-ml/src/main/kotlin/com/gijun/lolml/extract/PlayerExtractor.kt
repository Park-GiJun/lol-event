package com.gijun.lolml.extract

import java.sql.DriverManager

/**
 * 사람마다 가장 최근 경기에서 쓴 Riot ID. Riot API 에서 그 사람을 찾는 열쇠다.
 * 사람 키는 [MatchExtractor] 와 같은 규칙이다 — puuid, 비어 있으면 riot_id.
 */
class PlayerExtractor(
    private val config: DbConfig,
) {
    /** @return 사람 키 → Riot ID */
    fun extract(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        DriverManager.getConnection(config.jdbcUrl, config.user, config.password).use { connection ->
            connection.isReadOnly = true
            connection.prepareStatement(SQL).use { statement ->
                statement.executeQuery().use { rows ->
                    while (rows.next()) result[rows.getString("player_id")] = rows.getString("riot_id")
                }
            }
        }
        return result
    }

    private companion object {
        val SQL =
            """
            SELECT DISTINCT ON (player_id) player_id, riot_id
            FROM (
                SELECT CASE WHEN TRIM(COALESCE(p.puuid, '')) <> '' THEN p.puuid ELSE p.riot_id END AS player_id,
                       p.riot_id, m.game_creation, m.match_id
                FROM lol_event.match_participants p
                JOIN lol_event.matches m ON m.id = p.match_db_id
            ) t
            ORDER BY player_id, game_creation DESC, match_id DESC
            """.trimIndent()
    }
}
