package com.gijun.main.application.dto.stats.result

data class ChampionPlayerStat(
    val riotId: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
    val avgVisionScore: Double,
)

data class ChampionItemStat(
    val itemId: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
)

/**
 * 챔피언별 룬 조합 성적.
 *
 * 아이템은 칸 단위라 한 경기에서 여섯 번 세지만, 룬은 조합이 경기당 하나라
 * picks 가 곧 그 조합을 쓴 경기 수다.
 */
data class ChampionRuneStat(
    /** 핵심 룬(키스톤) id. */
    val keystone: Int,
    /** 주 계열 id (정밀 8000 / 지배 8100 / 마법 8200 / 영감 8300 / 결의 8400). */
    val primaryStyle: Int,
    /** 보조 계열 id. */
    val subStyle: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
)

data class ChampionLaneStat(
    val position: String,
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val kda: Double,
    val avgDamage: Int,
    val avgCs: Double,
    val avgGold: Int,
)

data class ChampionDetailStats(
    val champion: String,
    val championId: Int,
    val totalGames: Int,
    val totalWins: Int,
    val winRate: Int,
    val players: List<ChampionPlayerStat>,
    val itemStats: List<ChampionItemStat> = emptyList(),
    /** 룬 정보가 실려 오지 않은 경기만 있으면 빈 목록이다. */
    val runeStats: List<ChampionRuneStat> = emptyList(),
    val laneStats: List<ChampionLaneStat> = emptyList(),
)
