package com.gijun.main.application.dto.stats.result

data class WeeklyAwardEntry(
    val riotId: String,
    val displayValue: String,  // 표시할 값 (숫자, 퍼센트, 챔피언명 등)
    val games: Int,
)

data class WeeklyAwardsResult(
    val mostDeaths: WeeklyAwardEntry?,           // 단일 경기 최다 사망
    val worstKda: WeeklyAwardEntry?,             // 평균 KDA 최하위 (최소 5게임)
    val highGoldLowDamage: WeeklyAwardEntry?,    // 먹튀 골드왕
    val mostSurrenders: WeeklyAwardEntry?,       // 항복 유발자
    val pentaKillHero: WeeklyAwardEntry?,        // 펜타킬 영웅
    val loneHero: WeeklyAwardEntry?,             // 그래도 난 했다
    val highestWinRate: WeeklyAwardEntry?,       // 승률 1위 (최소 5게임)
    val mostGamesChampion: WeeklyAwardEntry?,    // 특정 챔피언 최다 플레이
)
