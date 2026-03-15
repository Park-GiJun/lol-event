package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.*
import com.gijun.main.application.port.`in`.GetMatchesUseCase
import com.gijun.main.application.port.out.MatchPort
import com.gijun.main.domain.model.Match
import com.gijun.main.domain.model.MatchParticipant
import com.gijun.main.domain.model.MatchTeam
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMatchesHandler(private val matchPort: MatchPort) : GetMatchesUseCase {
    override fun getAll(mode: String): List<MatchDto> =
        matchPort.findAllWithParticipants(modeToQueueIds(mode)).map { it.toDto() }

    private fun Match.toDto() = MatchDto(
        matchId = matchId, queueId = queueId,
        gameCreation = gameCreation, gameDuration = gameDuration,
        gameMode = gameMode, gameType = gameType, gameVersion = gameVersion,
        mapId = mapId, seasonId = seasonId, platformId = platformId,
        participants = participants.map { it.toDto() },
        teams = teams.map { it.toDto() }
    )

    private fun MatchParticipant.toDto() = ParticipantDto(
        puuid = puuid, riotId = riotId, champion = champion, championId = championId,
        team = team, teamId = teamId, spell1Id = spell1Id, spell2Id = spell2Id,
        win = win, kills = kills, deaths = deaths, assists = assists,
        damage = damage, cs = cs, gold = gold, visionScore = visionScore,
        champLevel = champLevel, doubleKills = doubleKills, tripleKills = tripleKills,
        quadraKills = quadraKills, pentaKills = pentaKills, unrealKills = unrealKills,
        killingSprees = killingSprees, largestKillingSpree = largestKillingSpree,
        largestMultiKill = largestMultiKill, largestCriticalStrike = largestCriticalStrike,
        longestTimeSpentLiving = longestTimeSpentLiving,
        firstBloodKill = firstBloodKill, firstBloodAssist = firstBloodAssist,
        firstTowerKill = firstTowerKill, firstTowerAssist = firstTowerAssist,
        firstInhibitorKill = firstInhibitorKill, firstInhibitorAssist = firstInhibitorAssist,
        inhibitorKills = inhibitorKills, turretKills = turretKills,
        wardsKilled = wardsKilled, wardsPlaced = wardsPlaced,
        sightWardsBoughtInGame = sightWardsBoughtInGame, visionWardsBoughtInGame = visionWardsBoughtInGame,
        item0 = item0, item1 = item1, item2 = item2, item3 = item3,
        item4 = item4, item5 = item5, item6 = item6,
        perk0 = perk0, perk0Var1 = perk0Var1, perk0Var2 = perk0Var2, perk0Var3 = perk0Var3,
        perk1 = perk1, perk1Var1 = perk1Var1, perk1Var2 = perk1Var2, perk1Var3 = perk1Var3,
        perk2 = perk2, perk2Var1 = perk2Var1, perk2Var2 = perk2Var2, perk2Var3 = perk2Var3,
        perk3 = perk3, perk3Var1 = perk3Var1, perk3Var2 = perk3Var2, perk3Var3 = perk3Var3,
        perk4 = perk4, perk4Var1 = perk4Var1, perk4Var2 = perk4Var2, perk4Var3 = perk4Var3,
        perk5 = perk5, perk5Var1 = perk5Var1, perk5Var2 = perk5Var2, perk5Var3 = perk5Var3,
        perkPrimaryStyle = perkPrimaryStyle, perkSubStyle = perkSubStyle,
        magicDamageDealt = magicDamageDealt, magicDamageDealtToChampions = magicDamageDealtToChampions,
        magicalDamageTaken = magicalDamageTaken,
        physicalDamageDealt = physicalDamageDealt, physicalDamageDealtToChampions = physicalDamageDealtToChampions,
        physicalDamageTaken = physicalDamageTaken,
        trueDamageDealt = trueDamageDealt, trueDamageDealtToChampions = trueDamageDealtToChampions,
        trueDamageTaken = trueDamageTaken,
        totalDamageDealt = totalDamageDealt, totalDamageDealtToChampions = totalDamageDealtToChampions,
        totalDamageTaken = totalDamageTaken,
        damageDealtToObjectives = damageDealtToObjectives, damageDealtToTurrets = damageDealtToTurrets,
        damageSelfMitigated = damageSelfMitigated,
        totalHeal = totalHeal, totalUnitsHealed = totalUnitsHealed,
        timeCCingOthers = timeCCingOthers, totalTimeCrowdControlDealt = totalTimeCrowdControlDealt,
        neutralMinionsKilled = neutralMinionsKilled,
        neutralMinionsKilledTeamJungle = neutralMinionsKilledTeamJungle,
        neutralMinionsKilledEnemyJungle = neutralMinionsKilledEnemyJungle,
        combatPlayerScore = combatPlayerScore, objectivePlayerScore = objectivePlayerScore,
        totalPlayerScore = totalPlayerScore, totalScoreRank = totalScoreRank,
        gameEndedInSurrender = gameEndedInSurrender,
        gameEndedInEarlySurrender = gameEndedInEarlySurrender,
        causedEarlySurrender = causedEarlySurrender,
        earlySurrenderAccomplice = earlySurrenderAccomplice,
        teamEarlySurrendered = teamEarlySurrendered,
        playerAugment1 = playerAugment1, playerAugment2 = playerAugment2,
        playerAugment3 = playerAugment3, playerAugment4 = playerAugment4,
        playerAugment5 = playerAugment5, playerAugment6 = playerAugment6,
        playerSubteamId = playerSubteamId, subteamPlacement = subteamPlacement,
        roleBoundItem = roleBoundItem, lane = lane, role = role
    )

    private fun MatchTeam.toDto() = TeamDto(
        teamId = teamId, win = win, baronKills = baronKills, dragonKills = dragonKills,
        towerKills = towerKills, inhibitorKills = inhibitorKills,
        riftHeraldKills = riftHeraldKills, hordeKills = hordeKills,
        firstBlood = firstBlood, firstTower = firstTower, firstBaron = firstBaron,
        firstInhibitor = firstInhibitor, firstDragon = firstDragon
    )
}

fun modeToQueueIds(mode: String): List<Int> = when (mode) {
    "aram" -> listOf(3270)
    "all"  -> listOf(0, 3130, 3270)
    else   -> listOf(0, 3130)  // normal (기본): 칼바람 제외
}
