package com.gijun.main.domain.model.match

data class MatchTeam(
    val id: Long = 0,
    val teamId: Int,
    val win: Boolean = false,
    val baronKills: Int = 0,
    val dragonKills: Int = 0,
    val towerKills: Int = 0,
    val inhibitorKills: Int = 0,
    val riftHeraldKills: Int = 0,
    val hordeKills: Int = 0,
    val firstBlood: Boolean = false,
    val firstTower: Boolean = false,
    val firstBaron: Boolean = false,
    val firstInhibitor: Boolean = false,
    val firstDragon: Boolean = false
)
