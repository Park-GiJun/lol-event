package com.gijun.main.infrastructure.adapter.out.persistence.elo.entity

import com.gijun.main.domain.model.elo.PlayerElo
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "player_elo", schema = "lol_event",
    uniqueConstraints = [UniqueConstraint(columnNames = ["riot_id"])]
)
class PlayerEloEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val riotId: String,
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val elo: Double = 1000.0,
    @Column(nullable = false) val games: Int = 0,
    @Column(nullable = false) val wins: Int = 0,
    @Column(nullable = false) val losses: Int = 0,
    @Column(nullable = false) val winStreak: Int = 0,
    @Column(nullable = false) val lossStreak: Int = 0,
    @Column(nullable = false) val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() = PlayerElo(
        id = id, riotId = riotId, elo = elo, games = games,
        wins = wins, losses = losses,
        winStreak = winStreak, lossStreak = lossStreak,
        updatedAt = updatedAt,
    )

    companion object {
        fun from(domain: PlayerElo) = PlayerEloEntity(
            id = domain.id, riotId = domain.riotId,
            elo = domain.elo, games = domain.games,
            wins = domain.wins, losses = domain.losses,
            winStreak = domain.winStreak, lossStreak = domain.lossStreak,
            updatedAt = domain.updatedAt,
        )
    }
}
