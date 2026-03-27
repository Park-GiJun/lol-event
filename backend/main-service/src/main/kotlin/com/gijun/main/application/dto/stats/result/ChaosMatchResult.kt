package com.gijun.main.application.dto.stats.result

data class ChaosMatchEntry(
    val matchId: String,
    val gameCreation: Long,
    val gameDurationMin: Double,
    val chaosIndex: Double,
    val totalKills: Int,
    val killDensity: Double,
    val multiKillScore: Int,
    val gameTypeTag: String,        // "혈전", "학살", "운영 접전", "일반"
    val participants: List<String>, // riotId 목록
)

data class ChaosMatchResult(
    val topChaosMatches: List<ChaosMatchEntry>,      // 혼돈 지수 상위 10
    val topBloodBathMatches: List<ChaosMatchEntry>,  // 학살 태그 상위 5
    val topStrategicMatches: List<ChaosMatchEntry>,  // 운영 접전 상위 5
    val avgChaosIndex: Double,
)
