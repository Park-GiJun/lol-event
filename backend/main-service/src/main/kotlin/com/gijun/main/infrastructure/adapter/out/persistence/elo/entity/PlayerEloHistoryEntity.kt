package com.gijun.main.infrastructure.adapter.out.persistence.elo.entity

import com.gijun.main.domain.model.elo.PlayerEloHistory
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "player_elo_history", schema = "lol_event",
    indexes = [
        Index(name = "idx_elo_history_riot_id", columnList = "riotId, gameCreation DESC"),
        Index(name = "idx_elo_history_match_id", columnList = "matchId"),
    ],
    // 같은 매치가 같은 사람에게 두 번 반영되는 것을 DB 차원에서 막는다.
    // Kafka 는 최소 한 번 배달이라 애플리케이션 체크만으로는 경쟁 조건이 남는다.
    uniqueConstraints = [UniqueConstraint(name = "uq_elo_history_match_player", columnNames = ["riot_id", "match_id"])]
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
    @Column(nullable = false, columnDefinition = "NUMERIC(4,3)") val lanePerformance: Double = 0.5,
    @Column(nullable = false) val gameCreation: Long,
    @Column(nullable = false) val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() = PlayerEloHistory(
        id = id, riotId = riotId, matchId = matchId,
        eloBefore = eloBefore, eloAfter = eloAfter, delta = delta,
        win = win, lanePerformance = lanePerformance,
        gameCreation = gameCreation, createdAt = createdAt,
    )

    companion object {
        fun from(d: PlayerEloHistory) = PlayerEloHistoryEntity(
            id = d.id, riotId = d.riotId, matchId = d.matchId,
            eloBefore = d.eloBefore, eloAfter = d.eloAfter, delta = d.delta,
            win = d.win, lanePerformance = d.lanePerformance,
            gameCreation = d.gameCreation, createdAt = d.createdAt,
        )
    }
}
