package com.gijun.main.infrastructure.adapter.`in`.web.stats.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gijun.main.application.dto.result.ChampionLengthTendency
import com.gijun.main.application.dto.result.ComebackIndexEntry
import com.gijun.main.application.dto.result.ComebackIndexResult
import com.gijun.main.application.dto.result.ComebackMatchEntry
import com.gijun.main.application.dto.result.DayPatternEntry
import com.gijun.main.application.dto.result.EarlyGameDominanceEntry
import com.gijun.main.application.dto.result.EarlyGameDominanceResult
import com.gijun.main.application.dto.result.GameLengthBucket
import com.gijun.main.application.dto.result.GameLengthTendencyEntry
import com.gijun.main.application.dto.result.GameLengthTendencyResult
import com.gijun.main.application.dto.result.HourPatternEntry
import com.gijun.main.application.dto.result.ObjectiveCorrelationResult
import com.gijun.main.application.dto.result.ObjectiveStat
import com.gijun.main.application.dto.result.TimePatternResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "ObjectiveStat")
data class ObjectiveStatResponse(
    val objective: String,
    val label: String,
    @field:Schema(description = "FIRST = 먼저 챙긴 팀 / MAJORITY = 더 많이 챙긴 팀.")
    val basis: String,
    val totalGames: Int,
    @field:Schema(description = "그 오브젝트의 주인이 가려진 경기 수. MAJORITY 는 양 팀이 같으면 안 센다.")
    val gamesWithFirst: Int,
    val winsWithFirst: Int,
    val winRateWithFirst: Int,
    val winRateWithout: Int,
) {
    companion object {
        fun from(result: ObjectiveStat) =
            ObjectiveStatResponse(
                objective = result.objective,
                label = result.label,
                basis = result.basis,
                totalGames = result.totalGames,
                gamesWithFirst = result.gamesWithFirst,
                winsWithFirst = result.winsWithFirst,
                winRateWithFirst = result.winRateWithFirst,
                winRateWithout = result.winRateWithout,
            )
    }
}

@Schema(name = "ObjectiveCorrelationResult")
data class ObjectiveCorrelationResponse(
    val totalGames: Int,
    val objectives: List<ObjectiveStatResponse>,
) {
    companion object {
        fun from(result: ObjectiveCorrelationResult) =
            ObjectiveCorrelationResponse(
                totalGames = result.totalGames,
                objectives = result.objectives.map(ObjectiveStatResponse::from),
            )
    }
}

@Schema(name = "DayPatternEntry")
data class DayPatternEntryResponse(
    @field:Schema(description = "1=Monday..7=Sunday (ISO)")
    val dayOfWeek: Int,
    @field:Schema(description = "\"월\", \"화\", ...")
    val dayName: String,
    val sessions: Int,
    val games: Int,
) {
    companion object {
        fun from(result: DayPatternEntry) =
            DayPatternEntryResponse(
                dayOfWeek = result.dayOfWeek,
                dayName = result.dayName,
                sessions = result.sessions,
                games = result.games,
            )
    }
}

@Schema(name = "HourPatternEntry")
data class HourPatternEntryResponse(
    val hour: Int,
    val games: Int,
) {
    companion object {
        fun from(result: HourPatternEntry) =
            HourPatternEntryResponse(
                hour = result.hour,
                games = result.games,
            )
    }
}

@Schema(name = "TimePatternResult")
data class TimePatternResponse(
    val byDay: List<DayPatternEntryResponse>,
    val byHour: List<HourPatternEntryResponse>,
    val busiestDay: String?,
    val busiestHour: Int?,
    val totalGames: Int,
) {
    companion object {
        fun from(result: TimePatternResult) =
            TimePatternResponse(
                byDay = result.byDay.map(DayPatternEntryResponse::from),
                byHour = result.byHour.map(HourPatternEntryResponse::from),
                busiestDay = result.busiestDay,
                busiestHour = result.busiestHour,
                totalGames = result.totalGames,
            )
    }
}

@Schema(name = "GameLengthBucket")
data class GameLengthBucketResponse(
    val games: Int,
    val wins: Int,
    val winRate: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgDamage: Double,
    val avgCsPerMin: Double,
) {
    companion object {
        fun from(result: GameLengthBucket) =
            GameLengthBucketResponse(
                games = result.games,
                wins = result.wins,
                winRate = result.winRate,
                avgKills = result.avgKills,
                avgDeaths = result.avgDeaths,
                avgDamage = result.avgDamage,
                avgCsPerMin = result.avgCsPerMin,
            )
    }
}

@Schema(name = "GameLengthTendencyEntry")
data class GameLengthTendencyEntryResponse(
    val riotId: String,
    val totalGames: Int,
    val shortGame: GameLengthBucketResponse,
    val midGame: GameLengthBucketResponse,
    val longGame: GameLengthBucketResponse,
    val tendency: String,
) {
    companion object {
        fun from(result: GameLengthTendencyEntry) =
            GameLengthTendencyEntryResponse(
                riotId = result.riotId,
                totalGames = result.totalGames,
                shortGame = GameLengthBucketResponse.from(result.shortGame),
                midGame = GameLengthBucketResponse.from(result.midGame),
                longGame = GameLengthBucketResponse.from(result.longGame),
                tendency = result.tendency,
            )
    }
}

@Schema(name = "ChampionLengthTendency")
data class ChampionLengthTendencyResponse(
    val champion: String,
    val championId: Int,
    val shortWinRate: Int,
    val midWinRate: Int,
    val longWinRate: Int,
    val bestLength: String,
) {
    companion object {
        fun from(result: ChampionLengthTendency) =
            ChampionLengthTendencyResponse(
                champion = result.champion,
                championId = result.championId,
                shortWinRate = result.shortWinRate,
                midWinRate = result.midWinRate,
                longWinRate = result.longWinRate,
                bestLength = result.bestLength,
            )
    }
}

@Schema(name = "GameLengthTendencyResult")
data class GameLengthTendencyResponse(
    val players: List<GameLengthTendencyEntryResponse>,
    val championTendencies: List<ChampionLengthTendencyResponse>,
) {
    companion object {
        fun from(result: GameLengthTendencyResult) =
            GameLengthTendencyResponse(
                players = result.players.map(GameLengthTendencyEntryResponse::from),
                championTendencies = result.championTendencies.map(ChampionLengthTendencyResponse::from),
            )
    }
}

@Schema(name = "EarlyGameDominanceEntry")
data class EarlyGameDominanceEntryResponse(
    val riotId: String,
    val games: Int,
    val firstBloodRate: Double,
    val firstTowerRate: Double,
    val earlyGameScore: Double,
    val firstBloodWinRate: Int,
    val noFirstBloodWinRate: Int,
    val badges: List<String>,
) {
    companion object {
        fun from(result: EarlyGameDominanceEntry) =
            EarlyGameDominanceEntryResponse(
                riotId = result.riotId,
                games = result.games,
                firstBloodRate = result.firstBloodRate,
                firstTowerRate = result.firstTowerRate,
                earlyGameScore = result.earlyGameScore,
                firstBloodWinRate = result.firstBloodWinRate,
                noFirstBloodWinRate = result.noFirstBloodWinRate,
                badges = result.badges,
            )
    }
}

@Schema(name = "EarlyGameDominanceResult")
data class EarlyGameDominanceResponse(
    val rankings: List<EarlyGameDominanceEntryResponse>,
    val firstBloodKing: String?,
    val towerDestroyer: String?,
    val overallFirstBloodWinRate: Double,
    val overallFirstTowerWinRate: Double,
) {
    companion object {
        fun from(result: EarlyGameDominanceResult) =
            EarlyGameDominanceResponse(
                rankings = result.rankings.map(EarlyGameDominanceEntryResponse::from),
                firstBloodKing = result.firstBloodKing,
                towerDestroyer = result.towerDestroyer,
                overallFirstBloodWinRate = result.overallFirstBloodWinRate,
                overallFirstTowerWinRate = result.overallFirstTowerWinRate,
            )
    }
}

@Schema(name = "ComebackIndexEntry")
data class ComebackIndexEntryResponse(
    val riotId: String,
    val totalGames: Int,
    val totalWinRate: Int,
    val contestGames: Int,
    val contestWinRate: Int,
    val comebackBonus: Int,
    @get:JsonProperty("isKing")
    val isKing: Boolean,
) {
    companion object {
        fun from(result: ComebackIndexEntry) =
            ComebackIndexEntryResponse(
                riotId = result.riotId,
                totalGames = result.totalGames,
                totalWinRate = result.totalWinRate,
                contestGames = result.contestGames,
                contestWinRate = result.contestWinRate,
                comebackBonus = result.comebackBonus,
                isKing = result.isKing,
            )
    }
}

@Schema(name = "ComebackMatchEntry")
data class ComebackMatchEntryResponse(
    val matchId: String,
    val gameCreation: Long,
    val gameDurationMin: Double,
    val winnerParticipants: List<String>,
) {
    companion object {
        fun from(result: ComebackMatchEntry) =
            ComebackMatchEntryResponse(
                matchId = result.matchId,
                gameCreation = result.gameCreation,
                gameDurationMin = result.gameDurationMin,
                winnerParticipants = result.winnerParticipants,
            )
    }
}

@Schema(name = "ComebackIndexResult")
data class ComebackIndexResponse(
    val rankings: List<ComebackIndexEntryResponse>,
    val comebackKing: String?,
    val topComebackMatches: List<ComebackMatchEntryResponse>,
) {
    companion object {
        fun from(result: ComebackIndexResult) =
            ComebackIndexResponse(
                rankings = result.rankings.map(ComebackIndexEntryResponse::from),
                comebackKing = result.comebackKing,
                topComebackMatches = result.topComebackMatches.map(ComebackMatchEntryResponse::from),
            )
    }
}
