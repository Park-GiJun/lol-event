package com.gijun.main.application.dto.stats.result

data class GameLengthBucket(
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgDamage: Double,
    val avgCsPerMin: Double,
)

data class GameLengthTendencyEntry(
    val riotId: String,
    val totalGames: Int,
    val shortGame: GameLengthBucket,
    val midGame: GameLengthBucket,
    val longGame: GameLengthBucket,
    val tendency: String,
)

data class ChampionLengthTendency(
    val champion: String,
    val championId: Int,
    val shortWinRate: Int,
    val midWinRate: Int,
    val longWinRate: Int,
    val bestLength: String,
)

data class GameLengthTendencyResult(
    val players: List<GameLengthTendencyEntry>,
    val championTendencies: List<ChampionLengthTendency>,
)
