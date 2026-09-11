package com.gijun.main.infrastructure.adapter.out.persistence.rating.entity

import com.gijun.main.domain.model.rating.PlayerRating
import com.gijun.main.domain.service.RatingMath
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 두 레이팅을 **별도 컬럼으로** 들고 있는다. 하나로 합친 값은 저장하지 않는다 —
 * 합치는 순간 라인 레이팅이 편성 오차로 오염되기 때문이다. 근거는 [RatingMath] 참고.
 *
 * 표시용 수축값도 저장하지 않는다. 저장은 원값, 수축은 API 응답에서만이다.
 */
@Entity
@Table(
    name = "player_rating", schema = "lol_event",
    uniqueConstraints = [UniqueConstraint(name = "uq_player_rating_riot_id", columnNames = ["riot_id"])]
)
class PlayerRatingEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "riot_id", nullable = false, length = 100) val riotId: String = "",
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val laneElo: Double = RatingMath.START,
    @Column(nullable = false) val laneDuels: Int = 0,
    @Column(nullable = false) val laneWins: Int = 0,
    @Column(nullable = false, columnDefinition = "NUMERIC(8,2)") val teamElo: Double = RatingMath.START,
    @Column(nullable = false) val teamGames: Int = 0,
    @Column(nullable = false) val teamWins: Int = 0,
    /** 연승/연패. 표시 전용이며 산식에는 쓰이지 않는다. */
    @Column(nullable = false) val teamWinStreak: Int = 0,
    @Column(nullable = false) val teamLossStreak: Int = 0,
    @Column(nullable = false) val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() = PlayerRating(
        id = id, riotId = riotId,
        laneElo = laneElo, laneDuels = laneDuels, laneWins = laneWins,
        teamElo = teamElo, teamGames = teamGames, teamWins = teamWins,
        teamWinStreak = teamWinStreak, teamLossStreak = teamLossStreak,
        updatedAt = updatedAt,
    )

    companion object {
        fun from(d: PlayerRating, id: Long = d.id) = PlayerRatingEntity(
            id = id, riotId = d.riotId,
            laneElo = d.laneElo, laneDuels = d.laneDuels, laneWins = d.laneWins,
            teamElo = d.teamElo, teamGames = d.teamGames, teamWins = d.teamWins,
            teamWinStreak = d.teamWinStreak, teamLossStreak = d.teamLossStreak,
            updatedAt = d.updatedAt,
        )
    }
}
