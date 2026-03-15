package com.gijun.main.application.dto.match.result

import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.MatchTeam

data class SaveMatchesResult(val saved: Int, val skipped: Int, val total: Long)

data class MatchResult(
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
    val participants: List<ParticipantResult>,
    val teams: List<TeamResult>
) {
    companion object {
        fun from(domain: Match) = MatchResult(
            matchId = domain.matchId, queueId = domain.queueId,
            gameCreation = domain.gameCreation, gameDuration = domain.gameDuration,
            gameMode = domain.gameMode, gameType = domain.gameType, gameVersion = domain.gameVersion,
            mapId = domain.mapId, seasonId = domain.seasonId, platformId = domain.platformId,
            participants = domain.participants.map { ParticipantResult.from(it) },
            teams = domain.teams.map { TeamResult.from(it) }
        )
    }
}

data class ParticipantResult(
    val puuid: String?,
    val riotId: String,
    val champion: String,
    val championId: Int,
    val team: String,
    val teamId: Int,
    val spell1Id: Int,
    val spell2Id: Int,
    val win: Boolean,
    val kills: Int, val deaths: Int, val assists: Int,
    val damage: Int, val cs: Int, val gold: Int, val visionScore: Int,
    val champLevel: Int,
    val doubleKills: Int, val tripleKills: Int, val quadraKills: Int,
    val pentaKills: Int, val unrealKills: Int,
    val killingSprees: Int, val largestKillingSpree: Int,
    val largestMultiKill: Int, val largestCriticalStrike: Int, val longestTimeSpentLiving: Int,
    val firstBloodKill: Boolean, val firstBloodAssist: Boolean,
    val firstTowerKill: Boolean, val firstTowerAssist: Boolean,
    val firstInhibitorKill: Boolean, val firstInhibitorAssist: Boolean,
    val inhibitorKills: Int, val turretKills: Int,
    val wardsKilled: Int, val wardsPlaced: Int,
    val sightWardsBoughtInGame: Int, val visionWardsBoughtInGame: Int,
    val item0: Int, val item1: Int, val item2: Int, val item3: Int,
    val item4: Int, val item5: Int, val item6: Int,
    val perk0: Int, val perk0Var1: Int, val perk0Var2: Int, val perk0Var3: Int,
    val perk1: Int, val perk1Var1: Int, val perk1Var2: Int, val perk1Var3: Int,
    val perk2: Int, val perk2Var1: Int, val perk2Var2: Int, val perk2Var3: Int,
    val perk3: Int, val perk3Var1: Int, val perk3Var2: Int, val perk3Var3: Int,
    val perk4: Int, val perk4Var1: Int, val perk4Var2: Int, val perk4Var3: Int,
    val perk5: Int, val perk5Var1: Int, val perk5Var2: Int, val perk5Var3: Int,
    val perkPrimaryStyle: Int, val perkSubStyle: Int,
    val magicDamageDealt: Int, val magicDamageDealtToChampions: Int, val magicalDamageTaken: Int,
    val physicalDamageDealt: Int, val physicalDamageDealtToChampions: Int, val physicalDamageTaken: Int,
    val trueDamageDealt: Int, val trueDamageDealtToChampions: Int, val trueDamageTaken: Int,
    val totalDamageDealt: Int, val totalDamageDealtToChampions: Int, val totalDamageTaken: Int,
    val damageDealtToObjectives: Int, val damageDealtToTurrets: Int, val damageSelfMitigated: Int,
    val totalHeal: Int, val totalUnitsHealed: Int,
    val timeCCingOthers: Int, val totalTimeCrowdControlDealt: Int,
    val neutralMinionsKilled: Int, val neutralMinionsKilledTeamJungle: Int, val neutralMinionsKilledEnemyJungle: Int,
    val combatPlayerScore: Int, val objectivePlayerScore: Int,
    val totalPlayerScore: Int, val totalScoreRank: Int,
    val gameEndedInSurrender: Boolean, val gameEndedInEarlySurrender: Boolean,
    val causedEarlySurrender: Boolean, val earlySurrenderAccomplice: Boolean, val teamEarlySurrendered: Boolean,
    val playerAugment1: Int, val playerAugment2: Int, val playerAugment3: Int,
    val playerAugment4: Int, val playerAugment5: Int, val playerAugment6: Int,
    val playerSubteamId: Int, val subteamPlacement: Int, val roleBoundItem: Int,
    val lane: String?, val role: String?
) {
    companion object {
        fun from(domain: MatchParticipant) = ParticipantResult(
            puuid = domain.puuid, riotId = domain.riotId, champion = domain.champion,
            championId = domain.championId, team = domain.team, teamId = domain.teamId,
            spell1Id = domain.spell1Id, spell2Id = domain.spell2Id, win = domain.win,
            kills = domain.kills, deaths = domain.deaths, assists = domain.assists,
            damage = domain.damage, cs = domain.cs, gold = domain.gold, visionScore = domain.visionScore,
            champLevel = domain.champLevel, doubleKills = domain.doubleKills, tripleKills = domain.tripleKills,
            quadraKills = domain.quadraKills, pentaKills = domain.pentaKills, unrealKills = domain.unrealKills,
            killingSprees = domain.killingSprees, largestKillingSpree = domain.largestKillingSpree,
            largestMultiKill = domain.largestMultiKill, largestCriticalStrike = domain.largestCriticalStrike,
            longestTimeSpentLiving = domain.longestTimeSpentLiving,
            firstBloodKill = domain.firstBloodKill, firstBloodAssist = domain.firstBloodAssist,
            firstTowerKill = domain.firstTowerKill, firstTowerAssist = domain.firstTowerAssist,
            firstInhibitorKill = domain.firstInhibitorKill, firstInhibitorAssist = domain.firstInhibitorAssist,
            inhibitorKills = domain.inhibitorKills, turretKills = domain.turretKills,
            wardsKilled = domain.wardsKilled, wardsPlaced = domain.wardsPlaced,
            sightWardsBoughtInGame = domain.sightWardsBoughtInGame, visionWardsBoughtInGame = domain.visionWardsBoughtInGame,
            item0 = domain.item0, item1 = domain.item1, item2 = domain.item2, item3 = domain.item3,
            item4 = domain.item4, item5 = domain.item5, item6 = domain.item6,
            perk0 = domain.perk0, perk0Var1 = domain.perk0Var1, perk0Var2 = domain.perk0Var2, perk0Var3 = domain.perk0Var3,
            perk1 = domain.perk1, perk1Var1 = domain.perk1Var1, perk1Var2 = domain.perk1Var2, perk1Var3 = domain.perk1Var3,
            perk2 = domain.perk2, perk2Var1 = domain.perk2Var1, perk2Var2 = domain.perk2Var2, perk2Var3 = domain.perk2Var3,
            perk3 = domain.perk3, perk3Var1 = domain.perk3Var1, perk3Var2 = domain.perk3Var2, perk3Var3 = domain.perk3Var3,
            perk4 = domain.perk4, perk4Var1 = domain.perk4Var1, perk4Var2 = domain.perk4Var2, perk4Var3 = domain.perk4Var3,
            perk5 = domain.perk5, perk5Var1 = domain.perk5Var1, perk5Var2 = domain.perk5Var2, perk5Var3 = domain.perk5Var3,
            perkPrimaryStyle = domain.perkPrimaryStyle, perkSubStyle = domain.perkSubStyle,
            magicDamageDealt = domain.magicDamageDealt, magicDamageDealtToChampions = domain.magicDamageDealtToChampions,
            magicalDamageTaken = domain.magicalDamageTaken,
            physicalDamageDealt = domain.physicalDamageDealt, physicalDamageDealtToChampions = domain.physicalDamageDealtToChampions,
            physicalDamageTaken = domain.physicalDamageTaken,
            trueDamageDealt = domain.trueDamageDealt, trueDamageDealtToChampions = domain.trueDamageDealtToChampions,
            trueDamageTaken = domain.trueDamageTaken,
            totalDamageDealt = domain.totalDamageDealt, totalDamageDealtToChampions = domain.totalDamageDealtToChampions,
            totalDamageTaken = domain.totalDamageTaken,
            damageDealtToObjectives = domain.damageDealtToObjectives, damageDealtToTurrets = domain.damageDealtToTurrets,
            damageSelfMitigated = domain.damageSelfMitigated,
            totalHeal = domain.totalHeal, totalUnitsHealed = domain.totalUnitsHealed,
            timeCCingOthers = domain.timeCCingOthers, totalTimeCrowdControlDealt = domain.totalTimeCrowdControlDealt,
            neutralMinionsKilled = domain.neutralMinionsKilled,
            neutralMinionsKilledTeamJungle = domain.neutralMinionsKilledTeamJungle,
            neutralMinionsKilledEnemyJungle = domain.neutralMinionsKilledEnemyJungle,
            combatPlayerScore = domain.combatPlayerScore, objectivePlayerScore = domain.objectivePlayerScore,
            totalPlayerScore = domain.totalPlayerScore, totalScoreRank = domain.totalScoreRank,
            gameEndedInSurrender = domain.gameEndedInSurrender, gameEndedInEarlySurrender = domain.gameEndedInEarlySurrender,
            causedEarlySurrender = domain.causedEarlySurrender, earlySurrenderAccomplice = domain.earlySurrenderAccomplice,
            teamEarlySurrendered = domain.teamEarlySurrendered,
            playerAugment1 = domain.playerAugment1, playerAugment2 = domain.playerAugment2,
            playerAugment3 = domain.playerAugment3, playerAugment4 = domain.playerAugment4,
            playerAugment5 = domain.playerAugment5, playerAugment6 = domain.playerAugment6,
            playerSubteamId = domain.playerSubteamId, subteamPlacement = domain.subteamPlacement,
            roleBoundItem = domain.roleBoundItem, lane = domain.lane, role = domain.role
        )
    }
}

data class TeamResult(
    val teamId: Int, val win: Boolean,
    val baronKills: Int, val dragonKills: Int, val towerKills: Int,
    val inhibitorKills: Int, val riftHeraldKills: Int, val hordeKills: Int,
    val firstBlood: Boolean, val firstTower: Boolean,
    val firstBaron: Boolean, val firstInhibitor: Boolean, val firstDragon: Boolean
) {
    companion object {
        fun from(domain: MatchTeam) = TeamResult(
            teamId = domain.teamId, win = domain.win,
            baronKills = domain.baronKills, dragonKills = domain.dragonKills, towerKills = domain.towerKills,
            inhibitorKills = domain.inhibitorKills, riftHeraldKills = domain.riftHeraldKills, hordeKills = domain.hordeKills,
            firstBlood = domain.firstBlood, firstTower = domain.firstTower,
            firstBaron = domain.firstBaron, firstInhibitor = domain.firstInhibitor, firstDragon = domain.firstDragon
        )
    }
}
