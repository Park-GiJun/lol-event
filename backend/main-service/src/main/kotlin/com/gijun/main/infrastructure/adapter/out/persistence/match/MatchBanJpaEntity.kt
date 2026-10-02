package com.gijun.main.infrastructure.adapter.out.persistence.match

import com.gijun.main.domain.match.model.MatchBanModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "match_team_bans", schema = "lol_event")
class MatchBanJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_db_id", nullable = false)
    val team: MatchTeamJpaEntity,
    @Column(nullable = false) val championId: Int,
    @Column(nullable = false) val championName: String,
    @Column(nullable = false) val pickTurn: Int = 0,
) {
    fun toModel() = MatchBanModel(championId = championId, championName = championName, pickTurn = pickTurn)

    companion object {
        fun from(
            domain: MatchBanModel,
            teamEntity: MatchTeamJpaEntity,
        ) = MatchBanJpaEntity(
            team = teamEntity,
            championId = domain.championId,
            championName = domain.championName,
            pickTurn = domain.pickTurn,
        )
    }
}
