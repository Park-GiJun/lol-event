package com.gijun.main.infrastructure.adapter.out.persistence.match.entity

import com.gijun.main.domain.model.match.MatchTeam
import jakarta.persistence.*

@Entity
@Table(name = "match_teams", schema = "lol_event")
class MatchTeamEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_db_id", nullable = false)
    val match: MatchEntity,
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
    @Column(nullable = false) val firstDragon: Boolean = false
) {
    fun toDomain() = MatchTeam(
        id = id, teamId = teamId, win = win,
        baronKills = baronKills, dragonKills = dragonKills, towerKills = towerKills,
        inhibitorKills = inhibitorKills, riftHeraldKills = riftHeraldKills, hordeKills = hordeKills,
        firstBlood = firstBlood, firstTower = firstTower, firstBaron = firstBaron,
        firstInhibitor = firstInhibitor, firstDragon = firstDragon
    )

    companion object {
        fun from(domain: MatchTeam, matchEntity: MatchEntity) = MatchTeamEntity(
            match = matchEntity, teamId = domain.teamId, win = domain.win,
            baronKills = domain.baronKills, dragonKills = domain.dragonKills, towerKills = domain.towerKills,
            inhibitorKills = domain.inhibitorKills, riftHeraldKills = domain.riftHeraldKills, hordeKills = domain.hordeKills,
            firstBlood = domain.firstBlood, firstTower = domain.firstTower, firstBaron = domain.firstBaron,
            firstInhibitor = domain.firstInhibitor, firstDragon = domain.firstDragon
        )
    }
}
