package com.gijun.main.infrastructure.adapter.`in`.web.match.dto

import com.gijun.main.application.dto.result.MatchPageResult
import com.gijun.main.application.dto.result.MatchResult
import com.gijun.main.application.dto.result.MatchSummaryResult
import com.gijun.main.application.dto.result.ParticipantResult
import com.gijun.main.application.dto.result.ParticipantSummaryResult
import com.gijun.main.application.dto.result.SaveMatchesResult
import com.gijun.main.application.dto.result.TeamResult
import com.gijun.main.application.dto.result.TeamSummaryResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "SaveMatchesResult")
data class SaveMatchesResponse(
    val saved: Int,
    val skipped: Int,
    val total: Long,
) {
    companion object {
        fun from(result: SaveMatchesResult) =
            SaveMatchesResponse(
                saved = result.saved,
                skipped = result.skipped,
                total = result.total,
            )
    }
}

@Schema(name = "MatchResult")
data class MatchResponse(
    val matchId: String,
    val queueId: Int,
    val gameCreation: Long,
    val gameDuration: Int,
    val gameMode: String?,
    val gameType: String?,
    val gameVersion: String?,
    val mapId: Int?,
    val seasonId: Int?,
    val platformId: String?,
    val participants: List<ParticipantResponse>,
    val teams: List<TeamResponse>,
) {
    companion object {
        fun from(result: MatchResult) =
            MatchResponse(
                matchId = result.matchId,
                queueId = result.queueId,
                gameCreation = result.gameCreation,
                gameDuration = result.gameDuration,
                gameMode = result.gameMode,
                gameType = result.gameType,
                gameVersion = result.gameVersion,
                mapId = result.mapId,
                seasonId = result.seasonId,
                platformId = result.platformId,
                participants = result.participants.map(ParticipantResponse::from),
                teams = result.teams.map(TeamResponse::from),
            )
    }
}

@Schema(name = "ParticipantResult")
data class ParticipantResponse(
    val puuid: String?,
    val riotId: String,
    val champion: String,
    val championId: Int,
    val team: String,
    val teamId: Int,
    val spell1Id: Int,
    val spell2Id: Int,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val damage: Int,
    val cs: Int,
    val gold: Int,
    val visionScore: Int,
    val champLevel: Int,
    val doubleKills: Int,
    val tripleKills: Int,
    val quadraKills: Int,
    val pentaKills: Int,
    val unrealKills: Int,
    val killingSprees: Int,
    val largestKillingSpree: Int,
    val largestMultiKill: Int,
    val largestCriticalStrike: Int,
    val longestTimeSpentLiving: Int,
    val firstBloodKill: Boolean,
    val firstBloodAssist: Boolean,
    val firstTowerKill: Boolean,
    val firstTowerAssist: Boolean,
    val firstInhibitorKill: Boolean,
    val firstInhibitorAssist: Boolean,
    val inhibitorKills: Int,
    val turretKills: Int,
    val wardsKilled: Int,
    val wardsPlaced: Int,
    val sightWardsBoughtInGame: Int,
    val visionWardsBoughtInGame: Int,
    val item0: Int,
    val item1: Int,
    val item2: Int,
    val item3: Int,
    val item4: Int,
    val item5: Int,
    val item6: Int,
    val perk0: Int,
    val perk0Var1: Int,
    val perk0Var2: Int,
    val perk0Var3: Int,
    val perk1: Int,
    val perk1Var1: Int,
    val perk1Var2: Int,
    val perk1Var3: Int,
    val perk2: Int,
    val perk2Var1: Int,
    val perk2Var2: Int,
    val perk2Var3: Int,
    val perk3: Int,
    val perk3Var1: Int,
    val perk3Var2: Int,
    val perk3Var3: Int,
    val perk4: Int,
    val perk4Var1: Int,
    val perk4Var2: Int,
    val perk4Var3: Int,
    val perk5: Int,
    val perk5Var1: Int,
    val perk5Var2: Int,
    val perk5Var3: Int,
    val perkPrimaryStyle: Int,
    val perkSubStyle: Int,
    val magicDamageDealt: Int,
    val magicDamageDealtToChampions: Int,
    val magicalDamageTaken: Int,
    val physicalDamageDealt: Int,
    val physicalDamageDealtToChampions: Int,
    val physicalDamageTaken: Int,
    val trueDamageDealt: Int,
    val trueDamageDealtToChampions: Int,
    val trueDamageTaken: Int,
    val totalDamageDealt: Int,
    val totalDamageDealtToChampions: Int,
    val totalDamageTaken: Int,
    val damageDealtToObjectives: Int,
    val damageDealtToTurrets: Int,
    val damageSelfMitigated: Int,
    val totalHeal: Int,
    val totalUnitsHealed: Int,
    val timeCCingOthers: Int,
    val totalTimeCrowdControlDealt: Int,
    val neutralMinionsKilled: Int,
    val neutralMinionsKilledTeamJungle: Int,
    val neutralMinionsKilledEnemyJungle: Int,
    val combatPlayerScore: Int,
    val objectivePlayerScore: Int,
    val totalPlayerScore: Int,
    val totalScoreRank: Int,
    val gameEndedInSurrender: Boolean,
    val gameEndedInEarlySurrender: Boolean,
    val causedEarlySurrender: Boolean,
    val earlySurrenderAccomplice: Boolean,
    val teamEarlySurrendered: Boolean,
    val playerAugment1: Int,
    val playerAugment2: Int,
    val playerAugment3: Int,
    val playerAugment4: Int,
    val playerAugment5: Int,
    val playerAugment6: Int,
    val playerSubteamId: Int,
    val subteamPlacement: Int,
    val roleBoundItem: Int,
    val lane: String?,
    val role: String?,
    @field:Schema(
        description =
            "포지션 재배정 백필 결과(TOP/JUNGLE/MIDDLE/BOTTOM/UTILITY). Riot 원본 lane/role 은 5v5 내전에서 심하게 왜곡돼 있어 그대로 쓰면 " +
                "안 된다.",
    )
    val assignedPosition: String,
) {
    companion object {
        fun from(result: ParticipantResult) =
            ParticipantResponse(
                puuid = result.puuid,
                riotId = result.riotId,
                champion = result.champion,
                championId = result.championId,
                team = result.team,
                teamId = result.teamId,
                spell1Id = result.spell1Id,
                spell2Id = result.spell2Id,
                win = result.win,
                kills = result.kills,
                deaths = result.deaths,
                assists = result.assists,
                damage = result.damage,
                cs = result.cs,
                gold = result.gold,
                visionScore = result.visionScore,
                champLevel = result.champLevel,
                doubleKills = result.doubleKills,
                tripleKills = result.tripleKills,
                quadraKills = result.quadraKills,
                pentaKills = result.pentaKills,
                unrealKills = result.unrealKills,
                killingSprees = result.killingSprees,
                largestKillingSpree = result.largestKillingSpree,
                largestMultiKill = result.largestMultiKill,
                largestCriticalStrike = result.largestCriticalStrike,
                longestTimeSpentLiving = result.longestTimeSpentLiving,
                firstBloodKill = result.firstBloodKill,
                firstBloodAssist = result.firstBloodAssist,
                firstTowerKill = result.firstTowerKill,
                firstTowerAssist = result.firstTowerAssist,
                firstInhibitorKill = result.firstInhibitorKill,
                firstInhibitorAssist = result.firstInhibitorAssist,
                inhibitorKills = result.inhibitorKills,
                turretKills = result.turretKills,
                wardsKilled = result.wardsKilled,
                wardsPlaced = result.wardsPlaced,
                sightWardsBoughtInGame = result.sightWardsBoughtInGame,
                visionWardsBoughtInGame = result.visionWardsBoughtInGame,
                item0 = result.item0,
                item1 = result.item1,
                item2 = result.item2,
                item3 = result.item3,
                item4 = result.item4,
                item5 = result.item5,
                item6 = result.item6,
                perk0 = result.perk0,
                perk0Var1 = result.perk0Var1,
                perk0Var2 = result.perk0Var2,
                perk0Var3 = result.perk0Var3,
                perk1 = result.perk1,
                perk1Var1 = result.perk1Var1,
                perk1Var2 = result.perk1Var2,
                perk1Var3 = result.perk1Var3,
                perk2 = result.perk2,
                perk2Var1 = result.perk2Var1,
                perk2Var2 = result.perk2Var2,
                perk2Var3 = result.perk2Var3,
                perk3 = result.perk3,
                perk3Var1 = result.perk3Var1,
                perk3Var2 = result.perk3Var2,
                perk3Var3 = result.perk3Var3,
                perk4 = result.perk4,
                perk4Var1 = result.perk4Var1,
                perk4Var2 = result.perk4Var2,
                perk4Var3 = result.perk4Var3,
                perk5 = result.perk5,
                perk5Var1 = result.perk5Var1,
                perk5Var2 = result.perk5Var2,
                perk5Var3 = result.perk5Var3,
                perkPrimaryStyle = result.perkPrimaryStyle,
                perkSubStyle = result.perkSubStyle,
                magicDamageDealt = result.magicDamageDealt,
                magicDamageDealtToChampions = result.magicDamageDealtToChampions,
                magicalDamageTaken = result.magicalDamageTaken,
                physicalDamageDealt = result.physicalDamageDealt,
                physicalDamageDealtToChampions = result.physicalDamageDealtToChampions,
                physicalDamageTaken = result.physicalDamageTaken,
                trueDamageDealt = result.trueDamageDealt,
                trueDamageDealtToChampions = result.trueDamageDealtToChampions,
                trueDamageTaken = result.trueDamageTaken,
                totalDamageDealt = result.totalDamageDealt,
                totalDamageDealtToChampions = result.totalDamageDealtToChampions,
                totalDamageTaken = result.totalDamageTaken,
                damageDealtToObjectives = result.damageDealtToObjectives,
                damageDealtToTurrets = result.damageDealtToTurrets,
                damageSelfMitigated = result.damageSelfMitigated,
                totalHeal = result.totalHeal,
                totalUnitsHealed = result.totalUnitsHealed,
                timeCCingOthers = result.timeCCingOthers,
                totalTimeCrowdControlDealt = result.totalTimeCrowdControlDealt,
                neutralMinionsKilled = result.neutralMinionsKilled,
                neutralMinionsKilledTeamJungle = result.neutralMinionsKilledTeamJungle,
                neutralMinionsKilledEnemyJungle = result.neutralMinionsKilledEnemyJungle,
                combatPlayerScore = result.combatPlayerScore,
                objectivePlayerScore = result.objectivePlayerScore,
                totalPlayerScore = result.totalPlayerScore,
                totalScoreRank = result.totalScoreRank,
                gameEndedInSurrender = result.gameEndedInSurrender,
                gameEndedInEarlySurrender = result.gameEndedInEarlySurrender,
                causedEarlySurrender = result.causedEarlySurrender,
                earlySurrenderAccomplice = result.earlySurrenderAccomplice,
                teamEarlySurrendered = result.teamEarlySurrendered,
                playerAugment1 = result.playerAugment1,
                playerAugment2 = result.playerAugment2,
                playerAugment3 = result.playerAugment3,
                playerAugment4 = result.playerAugment4,
                playerAugment5 = result.playerAugment5,
                playerAugment6 = result.playerAugment6,
                playerSubteamId = result.playerSubteamId,
                subteamPlacement = result.subteamPlacement,
                roleBoundItem = result.roleBoundItem,
                lane = result.lane,
                role = result.role,
                assignedPosition = result.assignedPosition,
            )
    }
}

@Schema(name = "TeamResult")
data class TeamResponse(
    val teamId: Int,
    val win: Boolean,
    val baronKills: Int,
    val dragonKills: Int,
    val towerKills: Int,
    val inhibitorKills: Int,
    val riftHeraldKills: Int,
    val hordeKills: Int,
    val firstBlood: Boolean,
    val firstTower: Boolean,
    val firstBaron: Boolean,
    val firstInhibitor: Boolean,
    val firstDragon: Boolean,
) {
    companion object {
        fun from(result: TeamResult) =
            TeamResponse(
                teamId = result.teamId,
                win = result.win,
                baronKills = result.baronKills,
                dragonKills = result.dragonKills,
                towerKills = result.towerKills,
                inhibitorKills = result.inhibitorKills,
                riftHeraldKills = result.riftHeraldKills,
                hordeKills = result.hordeKills,
                firstBlood = result.firstBlood,
                firstTower = result.firstTower,
                firstBaron = result.firstBaron,
                firstInhibitor = result.firstInhibitor,
                firstDragon = result.firstDragon,
            )
    }
}

@Schema(name = "MatchPageResult")
data class MatchPageResponse(
    val matches: List<MatchSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: MatchPageResult) =
            MatchPageResponse(
                matches = result.matches.map(MatchSummaryResponse::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                hasNext = result.hasNext,
            )
    }
}

@Schema(name = "MatchSummaryResult")
data class MatchSummaryResponse(
    val matchId: String,
    val queueId: Int,
    val gameCreation: Long,
    val gameDuration: Int,
    val gameMode: String?,
    val participants: List<ParticipantSummaryResponse>,
    val teams: List<TeamSummaryResponse>,
) {
    companion object {
        fun from(result: MatchSummaryResult) =
            MatchSummaryResponse(
                matchId = result.matchId,
                queueId = result.queueId,
                gameCreation = result.gameCreation,
                gameDuration = result.gameDuration,
                gameMode = result.gameMode,
                participants = result.participants.map(ParticipantSummaryResponse::from),
                teams = result.teams.map(TeamSummaryResponse::from),
            )
    }
}

@Schema(name = "ParticipantSummaryResult")
data class ParticipantSummaryResponse(
    val puuid: String?,
    val riotId: String,
    val champion: String,
    val championId: Int,
    val team: String,
    val teamId: Int,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val damage: Int,
    val cs: Int,
    val gold: Int,
    val visionScore: Int,
    val champLevel: Int,
    val spell1Id: Int,
    val spell2Id: Int,
    val perkPrimaryStyle: Int,
    val perkSubStyle: Int,
    val perk0: Int,
    val item0: Int,
    val item1: Int,
    val item2: Int,
    val item3: Int,
    val item4: Int,
    val item5: Int,
    val item6: Int,
    @field:Schema(description = "목록의 포지션 표기는 이 값만 쓴다. 원본 lane/role 은 내려보내지 않는다.")
    val assignedPosition: String,
) {
    companion object {
        fun from(result: ParticipantSummaryResult) =
            ParticipantSummaryResponse(
                puuid = result.puuid,
                riotId = result.riotId,
                champion = result.champion,
                championId = result.championId,
                team = result.team,
                teamId = result.teamId,
                win = result.win,
                kills = result.kills,
                deaths = result.deaths,
                assists = result.assists,
                damage = result.damage,
                cs = result.cs,
                gold = result.gold,
                visionScore = result.visionScore,
                champLevel = result.champLevel,
                spell1Id = result.spell1Id,
                spell2Id = result.spell2Id,
                perkPrimaryStyle = result.perkPrimaryStyle,
                perkSubStyle = result.perkSubStyle,
                perk0 = result.perk0,
                item0 = result.item0,
                item1 = result.item1,
                item2 = result.item2,
                item3 = result.item3,
                item4 = result.item4,
                item5 = result.item5,
                item6 = result.item6,
                assignedPosition = result.assignedPosition,
            )
    }
}

@Schema(name = "TeamSummaryResult")
data class TeamSummaryResponse(
    val teamId: Int,
    val win: Boolean,
    val baronKills: Int,
    val dragonKills: Int,
    val towerKills: Int,
) {
    companion object {
        fun from(result: TeamSummaryResult) =
            TeamSummaryResponse(
                teamId = result.teamId,
                win = result.win,
                baronKills = result.baronKills,
                dragonKills = result.dragonKills,
                towerKills = result.towerKills,
            )
    }
}
