package com.gijun.main.application.handler.command

import com.gijun.main.application.dto.*
import com.gijun.main.application.port.`in`.DeleteMatchUseCase
import com.gijun.main.application.port.`in`.SaveMatchesUseCase
import com.gijun.main.application.port.out.MatchPort
import com.gijun.main.domain.model.Match
import com.gijun.main.domain.model.MatchParticipant
import com.gijun.main.domain.model.MatchTeam
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SaveMatchesHandler(private val matchPort: MatchPort) : SaveMatchesUseCase, DeleteMatchUseCase {

    override fun save(request: SaveMatchesRequest): SaveMatchesResponse {
        var saved = 0; var skipped = 0
        for (dto in request.matches) {
            val match = matchPort.findByMatchId(dto.matchId)?.also {
                // 이미 존재하는 경우: participants/teams 초기화 후 재삽입 (upsert)
                it.participants.clear()
                it.teams.clear()
            } ?: Match(
                matchId = dto.matchId,
                queueId = dto.queueId,
                gameCreation = dto.gameCreation,
                gameDuration = dto.gameDuration,
                gameMode = dto.gameMode,
                gameType = dto.gameType,
                gameVersion = dto.gameVersion,
                mapId = dto.mapId,
                seasonId = dto.seasonId,
                platformId = dto.platformId
            )
            dto.participants.forEach { p ->
                match.participants.add(MatchParticipant(
                    match = match,
                    puuid = p.puuid,
                    riotId = p.riotId,
                    champion = p.champion,
                    championId = p.championId,
                    team = p.team,
                    teamId = p.teamId,
                    spell1Id = p.spell1Id,
                    spell2Id = p.spell2Id,
                    win = p.win,
                    kills = p.kills,
                    deaths = p.deaths,
                    assists = p.assists,
                    damage = p.damage,
                    cs = p.cs,
                    gold = p.gold,
                    visionScore = p.visionScore,
                    champLevel = p.champLevel,
                    doubleKills = p.doubleKills,
                    tripleKills = p.tripleKills,
                    quadraKills = p.quadraKills,
                    pentaKills = p.pentaKills,
                    unrealKills = p.unrealKills,
                    killingSprees = p.killingSprees,
                    largestKillingSpree = p.largestKillingSpree,
                    largestMultiKill = p.largestMultiKill,
                    largestCriticalStrike = p.largestCriticalStrike,
                    longestTimeSpentLiving = p.longestTimeSpentLiving,
                    firstBloodKill = p.firstBloodKill,
                    firstBloodAssist = p.firstBloodAssist,
                    firstTowerKill = p.firstTowerKill,
                    firstTowerAssist = p.firstTowerAssist,
                    firstInhibitorKill = p.firstInhibitorKill,
                    firstInhibitorAssist = p.firstInhibitorAssist,
                    inhibitorKills = p.inhibitorKills,
                    turretKills = p.turretKills,
                    wardsKilled = p.wardsKilled,
                    wardsPlaced = p.wardsPlaced,
                    sightWardsBoughtInGame = p.sightWardsBoughtInGame,
                    visionWardsBoughtInGame = p.visionWardsBoughtInGame,
                    item0 = p.item0, item1 = p.item1, item2 = p.item2, item3 = p.item3,
                    item4 = p.item4, item5 = p.item5, item6 = p.item6,
                    perk0 = p.perk0, perk0Var1 = p.perk0Var1, perk0Var2 = p.perk0Var2, perk0Var3 = p.perk0Var3,
                    perk1 = p.perk1, perk1Var1 = p.perk1Var1, perk1Var2 = p.perk1Var2, perk1Var3 = p.perk1Var3,
                    perk2 = p.perk2, perk2Var1 = p.perk2Var1, perk2Var2 = p.perk2Var2, perk2Var3 = p.perk2Var3,
                    perk3 = p.perk3, perk3Var1 = p.perk3Var1, perk3Var2 = p.perk3Var2, perk3Var3 = p.perk3Var3,
                    perk4 = p.perk4, perk4Var1 = p.perk4Var1, perk4Var2 = p.perk4Var2, perk4Var3 = p.perk4Var3,
                    perk5 = p.perk5, perk5Var1 = p.perk5Var1, perk5Var2 = p.perk5Var2, perk5Var3 = p.perk5Var3,
                    perkPrimaryStyle = p.perkPrimaryStyle,
                    perkSubStyle = p.perkSubStyle,
                    magicDamageDealt = p.magicDamageDealt,
                    magicDamageDealtToChampions = p.magicDamageDealtToChampions,
                    magicalDamageTaken = p.magicalDamageTaken,
                    physicalDamageDealt = p.physicalDamageDealt,
                    physicalDamageDealtToChampions = p.physicalDamageDealtToChampions,
                    physicalDamageTaken = p.physicalDamageTaken,
                    trueDamageDealt = p.trueDamageDealt,
                    trueDamageDealtToChampions = p.trueDamageDealtToChampions,
                    trueDamageTaken = p.trueDamageTaken,
                    totalDamageDealt = p.totalDamageDealt,
                    totalDamageDealtToChampions = p.totalDamageDealtToChampions,
                    totalDamageTaken = p.totalDamageTaken,
                    damageDealtToObjectives = p.damageDealtToObjectives,
                    damageDealtToTurrets = p.damageDealtToTurrets,
                    damageSelfMitigated = p.damageSelfMitigated,
                    totalHeal = p.totalHeal,
                    totalUnitsHealed = p.totalUnitsHealed,
                    timeCCingOthers = p.timeCCingOthers,
                    totalTimeCrowdControlDealt = p.totalTimeCrowdControlDealt,
                    neutralMinionsKilled = p.neutralMinionsKilled,
                    neutralMinionsKilledTeamJungle = p.neutralMinionsKilledTeamJungle,
                    neutralMinionsKilledEnemyJungle = p.neutralMinionsKilledEnemyJungle,
                    combatPlayerScore = p.combatPlayerScore,
                    objectivePlayerScore = p.objectivePlayerScore,
                    totalPlayerScore = p.totalPlayerScore,
                    totalScoreRank = p.totalScoreRank,
                    gameEndedInSurrender = p.gameEndedInSurrender,
                    gameEndedInEarlySurrender = p.gameEndedInEarlySurrender,
                    causedEarlySurrender = p.causedEarlySurrender,
                    earlySurrenderAccomplice = p.earlySurrenderAccomplice,
                    teamEarlySurrendered = p.teamEarlySurrendered,
                    playerAugment1 = p.playerAugment1,
                    playerAugment2 = p.playerAugment2,
                    playerAugment3 = p.playerAugment3,
                    playerAugment4 = p.playerAugment4,
                    playerAugment5 = p.playerAugment5,
                    playerAugment6 = p.playerAugment6,
                    playerSubteamId = p.playerSubteamId,
                    subteamPlacement = p.subteamPlacement,
                    roleBoundItem = p.roleBoundItem,
                    lane = p.lane,
                    role = p.role
                ))
            }
            dto.teams.forEach { t ->
                match.teams.add(MatchTeam(
                    match = match,
                    teamId = t.teamId,
                    win = t.win,
                    baronKills = t.baronKills,
                    dragonKills = t.dragonKills,
                    towerKills = t.towerKills,
                    inhibitorKills = t.inhibitorKills,
                    riftHeraldKills = t.riftHeraldKills,
                    hordeKills = t.hordeKills,
                    firstBlood = t.firstBlood,
                    firstTower = t.firstTower,
                    firstBaron = t.firstBaron,
                    firstInhibitor = t.firstInhibitor,
                    firstDragon = t.firstDragon
                ))
            }
            matchPort.save(match)
            saved++
        }
        val total = matchPort.countByQueueIds(listOf(0, 3130, 3270))
        return SaveMatchesResponse(saved, skipped, total)
    }

    override fun delete(matchId: String) = matchPort.deleteByMatchId(matchId)
}
