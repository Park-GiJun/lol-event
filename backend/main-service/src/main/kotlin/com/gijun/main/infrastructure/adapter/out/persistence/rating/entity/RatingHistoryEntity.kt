package com.gijun.main.infrastructure.adapter.out.persistence.rating.entity

import com.gijun.main.domain.model.rating.LaneResult
import com.gijun.main.domain.model.rating.RatingHistory
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "rating_history", schema = "lol_event",
    indexes = [
        Index(name = "idx_rating_history_riot_id", columnList = "riot_id, game_creation DESC"),
        Index(name = "idx_rating_history_match_id", columnList = "match_id"),
    ],
    // 같은 매치가 같은 사람에게 두 번 반영되는 것을 DB 차원에서 막는다.
    // Kafka 는 최소 한 번 배달이라 애플리케이션 체크만으로는 경쟁 조건이 남는다.
    uniqueConstraints = [UniqueConstraint(name = "uq_rating_history_match_player", columnNames = ["riot_id", "match_id"])]
)
class RatingHistoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "riot_id", nullable = false, length = 100) val riotId: String = "",
    @Column(name = "match_id", nullable = false, length = 100) val matchId: String = "",
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val laneBefore: Double = 0.0,
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val laneAfter: Double = 0.0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8) val laneResult: LaneResult = LaneResult.NONE,
    @Column(length = 100) val laneOpponent: String? = null,
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val teamBefore: Double = 0.0,
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val teamAfter: Double = 0.0,
    @Column(nullable = false) val win: Boolean = false,
    @Column(name = "game_creation", nullable = false) val gameCreation: Long = 0,
    @Column(nullable = false) val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() = RatingHistory(
        id = id, riotId = riotId, matchId = matchId,
        laneBefore = laneBefore, laneAfter = laneAfter,
        laneResult = laneResult, laneOpponent = laneOpponent,
        teamBefore = teamBefore, teamAfter = teamAfter,
        win = win, gameCreation = gameCreation, createdAt = createdAt,
    )

    companion object {
        fun from(d: RatingHistory) = RatingHistoryEntity(
            id = d.id, riotId = d.riotId, matchId = d.matchId,
            laneBefore = d.laneBefore, laneAfter = d.laneAfter,
            laneResult = d.laneResult, laneOpponent = d.laneOpponent,
            teamBefore = d.teamBefore, teamAfter = d.teamAfter,
            win = d.win, gameCreation = d.gameCreation, createdAt = d.createdAt,
        )
    }
}
