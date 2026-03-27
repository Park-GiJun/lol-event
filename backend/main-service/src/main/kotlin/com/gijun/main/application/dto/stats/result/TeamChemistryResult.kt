package com.gijun.main.application.dto.stats.result

data class TeamChemistryEntry(
    val players: List<String>,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val compositionSize: Int,
)

data class TeamChemistryResult(
    val bestDuos: List<TeamChemistryEntry>,
    val bestTrios: List<TeamChemistryEntry>,
    val bestFullTeams: List<TeamChemistryEntry>,
    val worstDuos: List<TeamChemistryEntry>,
)
