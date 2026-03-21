package com.gijun.main.infrastructure.adapter.out.persistence.elo.entity

import com.gijun.main.domain.model.elo.PlayerEloHistory
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "player_elo_history", schema = "lol_event",
    indexes = [Index(name = "idx_elo_history_riot_id", columnList = "riotId, gameCreation DESC")]
)
class PlayerEloHistoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val riotId: String,
    @Column(nullable = false) val matchId: String,
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val eloBefore: Double,
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val eloAfter: Double,
    @Column(nullable = false, columnDefinition = "NUMERIC(6,2)") val delta: Double,
    @Column(nullable = false) val win: Boolean,
    @Column(nullable = false) val gameCreation: Long,
    @Column(nullable = false) val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() = PlayerEloHistory(
        id = id, riotId = riotId, matchId = matchId,
        eloBefore = eloBefore, eloAfter = eloAfter, delta = delta,
        win = win, gameCreation = gameCreation, createdAt = createdAt,
    )

    companion object {
        fun from(d: PlayerEloHistory) = PlayerEloHistoryEntity(
            id = d.id, riotId = d.riotId, matchId = d.matchId,
            eloBefore = d.eloBefore, eloAfter = d.eloAfter, delta = d.delta,
            win = d.win, gameCreation = d.gameCreation, createdAt = d.createdAt,
        )
    }
}
