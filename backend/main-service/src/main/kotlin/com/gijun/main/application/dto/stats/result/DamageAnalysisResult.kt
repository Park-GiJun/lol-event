package com.gijun.main.application.dto.stats.result

data class DamagePlayerEntry(
    val riotId: String,
    val games: Int,
    val avgPhysical: Int,
    val avgMagic: Int,
    val avgTrue: Int,
    val avgTotal: Int,
    val avgMitigated: Int,
    val avgTurretDmg: Int,
    val physicalRate: Double,
    val magicRate: Double,
    val trueRate: Double,
    val damageProfile: String,
)

data class DamageAnalysisResult(
    val players: List<DamagePlayerEntry>,
)
