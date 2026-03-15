package com.gijun.main.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "match_teams", schema = "lol_event")
class MatchTeam(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_db_id", nullable = false)
    val match: Match,
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
)
