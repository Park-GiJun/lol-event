package com.gijun.main.infrastructure.adapter.out.persistence.match

import com.gijun.main.domain.match.model.MatchTeamModel
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "match_teams", schema = "lol_event")
class MatchTeamJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_db_id", nullable = false)
    val match: MatchJpaEntity,
    @Column(nullable = false) val teamId: Int,
    @Column(nullable = false) val win: Boolean = false,
    @Column(nullable = false) val baronKills: Int = 0,
    @Column(nullable = false) val dragonKills: Int = 0,
    @Column(nullable = false) val towerKills: Int = 0,
    @Column(nullable = false) val inhibitorKills: Int = 0,
    @Column(nullable = false) val riftHeraldKills: Int = 0,
    @Column(nullable = false) val hordeKills: Int = 0,
    @Column(nullable = false) val firstBlood: Boolean = false,
    @Column(nullable = false) val firstTower: Boolean = false,
    @Column(nullable = false) val firstBaron: Boolean = false,
    @Column(nullable = false) val firstInhibitor: Boolean = false,
    @Column(nullable = false) val firstDragon: Boolean = false,
    @OneToMany(mappedBy = "team", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val bans: MutableList<MatchBanJpaEntity> = mutableListOf(),
) {
    fun toModel() =
        MatchTeamModel(
            id = id,
            teamId = teamId,
            win = win,
            baronKills = baronKills,
            dragonKills = dragonKills,
            towerKills = towerKills,
            inhibitorKills = inhibitorKills,
            riftHeraldKills = riftHeraldKills,
            hordeKills = hordeKills,
            firstBlood = firstBlood,
            firstTower = firstTower,
            firstBaron = firstBaron,
            firstInhibitor = firstInhibitor,
            firstDragon = firstDragon,
            bans = bans.map { it.toModel() }.sortedBy { it.pickTurn },
        )

    companion object {
        fun from(
            domain: MatchTeamModel,
            matchEntity: MatchJpaEntity,
        ): MatchTeamJpaEntity {
            val entity =
                MatchTeamJpaEntity(
                    match = matchEntity,
                    teamId = domain.teamId,
                    win = domain.win,
                    baronKills = domain.baronKills,
                    dragonKills = domain.dragonKills,
                    towerKills = domain.towerKills,
                    inhibitorKills = domain.inhibitorKills,
                    riftHeraldKills = domain.riftHeraldKills,
                    hordeKills = domain.hordeKills,
                    firstBlood = domain.firstBlood,
                    firstTower = domain.firstTower,
                    firstBaron = domain.firstBaron,
                    firstInhibitor = domain.firstInhibitor,
                    firstDragon = domain.firstDragon,
                )
            domain.bans.forEach { ban -> entity.bans.add(MatchBanJpaEntity.from(ban, entity)) }
            return entity
        }
    }
}
