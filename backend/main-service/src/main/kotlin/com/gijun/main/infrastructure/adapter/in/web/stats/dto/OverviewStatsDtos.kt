package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.gijun.main.application.dto.result.ChampionPickStat
import com.gijun.main.application.dto.result.OverviewStats
import com.gijun.main.application.dto.result.PlayerLeaderStat
import com.gijun.main.application.dto.result.WeeklyAwardEntry
import com.gijun.main.application.dto.result.WeeklyAwardsResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "ChampionPickStat")
data class ChampionPickStatResponse(
    val champion: String,
    val championId: Int,
    val picks: Int,
    val wins: Int,
    val winRate: Int,
    val kda: Double,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val avgDamage: Int,
    val avgCs: Double,
) {
    companion object {
        fun from(result: ChampionPickStat) =
            ChampionPickStatResponse(
                champion = result.champion,
                championId = result.championId,
                picks = result.picks,
                wins = result.wins,
                winRate = result.winRate,
                kda = result.kda,
                avgKills = result.avgKills,
                avgDeaths = result.avgDeaths,
                avgAssists = result.avgAssists,
                avgDamage = result.avgDamage,
                avgCs = result.avgCs,
            )
    }
}

@Schema(name = "PlayerLeaderStat")
data class PlayerLeaderStatResponse(
    val riotId: String,
    val displayValue: String,
    val games: Int,
) {
    companion object {
        fun from(result: PlayerLeaderStat) =
            PlayerLeaderStatResponse(
                riotId = result.riotId,
                displayValue = result.displayValue,
                games = result.games,
            )
    }
}

@Schema(name = "OverviewStats")
data class OverviewStatsResponse(
    val matchCount: Int,
    val avgGameMinutes: Double,
    @field:Schema(description = "챔피언 통계")
    val topPickedChampions: List<ChampionPickStatResponse>,
    @field:Schema(description = "최소 3픽 이상")
    val topWinRateChampions: List<ChampionPickStatResponse>,
    val topBannedChampions: List<ChampionPickStatResponse>,
    @field:Schema(description = "플레이어 명예의 전당")
    val winRateLeader: PlayerLeaderStatResponse?,
    val kdaLeader: PlayerLeaderStatResponse?,
    val killsLeader: PlayerLeaderStatResponse?,
    val damageLeader: PlayerLeaderStatResponse?,
    val goldLeader: PlayerLeaderStatResponse?,
    val csLeader: PlayerLeaderStatResponse?,
    val visionLeader: PlayerLeaderStatResponse?,
    val objectiveDamageLeader: PlayerLeaderStatResponse?,
    val turretKillsLeader: PlayerLeaderStatResponse?,
    val pentaKillsLeader: PlayerLeaderStatResponse?,
    val wardsLeader: PlayerLeaderStatResponse?,
    val ccLeader: PlayerLeaderStatResponse?,
    val mostGamesPlayed: PlayerLeaderStatResponse?,
    val firstBloodLeader: PlayerLeaderStatResponse?,
    @field:Schema(description = "전체 오브젝트 집계")
    val totalBaronKills: Int,
    val totalDragonKills: Int,
    val totalTowerKills: Int,
    val totalRiftHeraldKills: Int,
    val totalInhibitorKills: Int,
    val totalFirstBloods: Int,
    val totalCs: Long,
) {
    companion object {
        fun from(result: OverviewStats) =
            OverviewStatsResponse(
                matchCount = result.matchCount,
                avgGameMinutes = result.avgGameMinutes,
                topPickedChampions = result.topPickedChampions.map(ChampionPickStatResponse::from),
                topWinRateChampions = result.topWinRateChampions.map(ChampionPickStatResponse::from),
                topBannedChampions = result.topBannedChampions.map(ChampionPickStatResponse::from),
                winRateLeader = result.winRateLeader?.let(PlayerLeaderStatResponse::from),
                kdaLeader = result.kdaLeader?.let(PlayerLeaderStatResponse::from),
                killsLeader = result.killsLeader?.let(PlayerLeaderStatResponse::from),
                damageLeader = result.damageLeader?.let(PlayerLeaderStatResponse::from),
                goldLeader = result.goldLeader?.let(PlayerLeaderStatResponse::from),
                csLeader = result.csLeader?.let(PlayerLeaderStatResponse::from),
                visionLeader = result.visionLeader?.let(PlayerLeaderStatResponse::from),
                objectiveDamageLeader = result.objectiveDamageLeader?.let(PlayerLeaderStatResponse::from),
                turretKillsLeader = result.turretKillsLeader?.let(PlayerLeaderStatResponse::from),
                pentaKillsLeader = result.pentaKillsLeader?.let(PlayerLeaderStatResponse::from),
                wardsLeader = result.wardsLeader?.let(PlayerLeaderStatResponse::from),
                ccLeader = result.ccLeader?.let(PlayerLeaderStatResponse::from),
                mostGamesPlayed = result.mostGamesPlayed?.let(PlayerLeaderStatResponse::from),
                firstBloodLeader = result.firstBloodLeader?.let(PlayerLeaderStatResponse::from),
                totalBaronKills = result.totalBaronKills,
                totalDragonKills = result.totalDragonKills,
                totalTowerKills = result.totalTowerKills,
                totalRiftHeraldKills = result.totalRiftHeraldKills,
                totalInhibitorKills = result.totalInhibitorKills,
                totalFirstBloods = result.totalFirstBloods,
                totalCs = result.totalCs,
            )
    }
}

@Schema(name = "WeeklyAwardEntry")
data class WeeklyAwardEntryResponse(
    val riotId: String,
    @field:Schema(description = "표시할 값 (숫자, 퍼센트, 챔피언명 등)")
    val displayValue: String,
    val games: Int,
) {
    companion object {
        fun from(result: WeeklyAwardEntry) =
            WeeklyAwardEntryResponse(
                riotId = result.riotId,
                displayValue = result.displayValue,
                games = result.games,
            )
    }
}

@Schema(name = "WeeklyAwardsResult")
data class WeeklyAwardsResponse(
    @field:Schema(description = "단일 경기 최다 사망")
    val mostDeaths: WeeklyAwardEntryResponse?,
    @field:Schema(description = "평균 KDA 최하위 (최소 5게임)")
    val worstKda: WeeklyAwardEntryResponse?,
    @field:Schema(description = "먹튀 골드왕")
    val highGoldLowDamage: WeeklyAwardEntryResponse?,
    @field:Schema(description = "항복 유발자")
    val mostSurrenders: WeeklyAwardEntryResponse?,
    @field:Schema(description = "펜타킬 영웅")
    val pentaKillHero: WeeklyAwardEntryResponse?,
    @field:Schema(description = "그래도 난 했다")
    val loneHero: WeeklyAwardEntryResponse?,
    @field:Schema(description = "승률 1위 (최소 5게임)")
    val highestWinRate: WeeklyAwardEntryResponse?,
    @field:Schema(description = "특정 챔피언 최다 플레이")
    val mostGamesChampion: WeeklyAwardEntryResponse?,
) {
    companion object {
        fun from(result: WeeklyAwardsResult) =
            WeeklyAwardsResponse(
                mostDeaths = result.mostDeaths?.let(WeeklyAwardEntryResponse::from),
                worstKda = result.worstKda?.let(WeeklyAwardEntryResponse::from),
                highGoldLowDamage = result.highGoldLowDamage?.let(WeeklyAwardEntryResponse::from),
                mostSurrenders = result.mostSurrenders?.let(WeeklyAwardEntryResponse::from),
                pentaKillHero = result.pentaKillHero?.let(WeeklyAwardEntryResponse::from),
                loneHero = result.loneHero?.let(WeeklyAwardEntryResponse::from),
                highestWinRate = result.highestWinRate?.let(WeeklyAwardEntryResponse::from),
                mostGamesChampion = result.mostGamesChampion?.let(WeeklyAwardEntryResponse::from),
            )
    }
}
