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
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val elo: Double = 1500.0,
    @Column(nullable = false) val games: Int = 0,
    @Column(nullable = false) val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() = PlayerElo(id = id, riotId = riotId, elo = elo, games = games, updatedAt = updatedAt)

    companion object {
        fun from(domain: PlayerElo) = PlayerEloEntity(
            id = domain.id, riotId = domain.riotId,
            elo = domain.elo, games = domain.games, updatedAt = domain.updatedAt,
        )
    }
}
