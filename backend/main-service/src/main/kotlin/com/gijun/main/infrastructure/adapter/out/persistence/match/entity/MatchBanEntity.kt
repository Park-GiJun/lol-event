package com.gijun.main.infrastructure.adapter.out.persistence.match.entity

import com.gijun.main.domain.model.match.MatchBan
import jakarta.persistence.*

@Entity
@Table(name = "match_team_bans", schema = "lol_event")
class MatchBanEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_db_id", nullable = false)
    val team: MatchTeamEntity,
    @Column(nullable = false) val championId: Int,
    @Column(nullable = false) val championName: String,
    @Column(nullable = false) val pickTurn: Int = 0,
) {
    fun toDomain() = MatchBan(championId = championId, championName = championName, pickTurn = pickTurn)

    companion object {
        fun from(domain: MatchBan, teamEntity: MatchTeamEntity) = MatchBanEntity(
            team = teamEntity,
            championId = domain.championId,
            championName = domain.championName,
            pickTurn = domain.pickTurn,
        )
    }
}
