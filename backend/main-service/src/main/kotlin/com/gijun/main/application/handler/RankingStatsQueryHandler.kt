package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetLaneLeaderboardQuery
import com.gijun.main.application.dto.result.KillParticipationEntry
import com.gijun.main.application.dto.result.KillParticipationResult
import com.gijun.main.application.dto.result.LaneLeaderboardResult
import com.gijun.main.application.dto.result.MvpPlayerStat
import com.gijun.main.application.dto.result.MvpStatsResult
import com.gijun.main.application.dto.result.PlayerLaneStat
import com.gijun.main.application.port.`in`.GetKillParticipationUseCase
import com.gijun.main.application.port.`in`.GetLaneLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetMvpStatsUseCase
import com.gijun.main.application.port.out.cache.StatsResultCacheQueryPort
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.model.MatchParticipantModel
import com.gijun.main.domain.match.service.PositionResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 순위표 — 라인별 랭킹, MVP, 킬 관여.
 *
 * Elo 리더보드는 레이팅 도메인 소관이라 [RatingQueryHandler] 에 있다.
 */
@Service
@Transactional(readOnly = true)
class RankingStatsQueryHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val statsResultCacheQueryPort: StatsResultCacheQueryPort,
) : GetLaneLeaderboardUseCase,
    GetMvpStatsUseCase,
    GetKillParticipationUseCase {
    private fun r1(v: Double) = (v * 10).toInt() / 10.0

    private fun r2(v: Double) = (v * 100).toInt() / 100.0

    private fun kda(
        k: Int,
        d: Int,
        a: Int,
    ) = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()

    override fun getLaneLeaderboard(query: GetLaneLeaderboardQuery): LaneLeaderboardResult =
        statsResultCacheQueryPort.getOrCompute("lane-leaderboard:${query.lane}:${query.mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(query.mode.queueIds)

            data class Entry(
                val riotId: String,
                val champion: String,
                val championId: Int,
                val win: Boolean,
                val kills: Int,
                val deaths: Int,
                val assists: Int,
                val damage: Int,
                val cs: Int,
                val gold: Int,
                val visionScore: Int,
                val damageTaken: Int,
                val objectiveDamage: Int,
                val wardsPlaced: Int,
                val ccTime: Int,
                val neutralMinions: Int,
            )

            val entries =
                matches.flatMap { m ->
                    m.participants.mapNotNull { p ->
                        val pos = PositionResolver.resolve(p)
                        if (pos != query.lane) return@mapNotNull null
                        Entry(
                            riotId = p.riotId,
                            champion = p.champion,
                            championId = p.championId,
                            win = p.win,
                            kills = p.kills,
                            deaths = p.deaths,
                            assists = p.assists,
                            damage = p.damage,
                            cs = p.cs,
                            gold = p.gold,
                            visionScore = p.visionScore,
                            damageTaken = p.totalDamageTaken,
                            objectiveDamage = p.damageDealtToObjectives,
                            wardsPlaced = p.wardsPlaced,
                            ccTime = p.timeCCingOthers,
                            neutralMinions = p.neutralMinionsKilled,
                        )
                    }
                }

            val players =
                entries
                    .groupBy { it.riotId }
                    .map { (riotId, es) ->
                        val g = es.size
                        val w = es.count { it.win }
                        val k = es.sumOf { it.kills }
                        val d = es.sumOf { it.deaths }
                        val a = es.sumOf { it.assists }
                        val topChampEntry = es.groupBy { it.championId }.maxByOrNull { it.value.size }
                        PlayerLaneStat(
                            riotId = riotId,
                            games = g,
                            wins = w,
                            winRate = w * 100 / g,
                            avgKills = r1(k.toDouble() / g),
                            avgDeaths = r1(d.toDouble() / g),
                            avgAssists = r1(a.toDouble() / g),
                            kda = kda(k, d, a),
                            avgDamage = es.sumOf { it.damage } / g,
                            avgCs = r1(es.sumOf { it.cs }.toDouble() / g),
                            avgGold = es.sumOf { it.gold } / g,
                            avgVisionScore = r1(es.sumOf { it.visionScore }.toDouble() / g),
                            avgDamageTaken = es.sumOf { it.damageTaken } / g,
                            avgObjectiveDamage = es.sumOf { it.objectiveDamage } / g,
                            avgWardsPlaced = r1(es.sumOf { it.wardsPlaced }.toDouble() / g),
                            avgCcTime = r1(es.sumOf { it.ccTime }.toDouble() / g),
                            avgNeutralMinions = r1(es.sumOf { it.neutralMinions }.toDouble() / g),
                            topChampion = topChampEntry?.value?.first()?.champion,
                            topChampionId = topChampEntry?.key,
                        )
                    }.sortedByDescending { it.games }

            LaneLeaderboardResult(lane = query.lane, players = players)
        }

    /**
     * MVP 점수 계산 공식:
     *  - KDA 기여: (kills * 3 + assists * 1.5) / max(deaths, 1)
     *  - 팀 데미지 기여율: (내 데미지 / 팀 총 데미지) * 40
     *  - 시야: visionScore / gameDuration_min
     *  - CS: cs / gameDuration_min * 0.5
     *  - 승리 보너스: +20
     */
    private fun calcScore(
        p: MatchParticipantModel,
        teamTotalDamage: Int,
        durationMin: Double,
    ): Double {
        val kdaPart = (p.kills * 3.0 + p.assists * 1.5) / maxOf(p.deaths, 1)
        val damagePart = if (teamTotalDamage > 0) p.damage.toDouble() / teamTotalDamage * 40 else 0.0
        val visionPart = if (durationMin > 0) p.visionScore / durationMin else 0.0
        val csPart = if (durationMin > 0) p.cs / durationMin * 0.5 else 0.0
        val winBonus = if (p.win) 20.0 else 0.0
        return kdaPart + damagePart + visionPart + csPart + winBonus
    }

    override fun getMvpStats(mode: GameMode): MvpStatsResult =
        statsResultCacheQueryPort.getOrCompute("mvp-stats:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class PlayerAcc(
                var games: Int = 0,
                var mvpCount: Int = 0,
                var aceCount: Int = 0,
                var totalScore: Double = 0.0,
                val mvpChampions: MutableMap<String, Int> = mutableMapOf(),
                val mvpChampionIds: MutableMap<String, Int> = mutableMapOf(),
            )

            val accMap = mutableMapOf<String, PlayerAcc>()

            for (match in matches) {
                val durationMin = if (match.gameDuration > 0) match.gameDuration / 60.0 else 1.0
                val byTeam = match.participants.groupBy { it.teamId }

                // 팀별 총 데미지 계산
                val teamDamage = byTeam.mapValues { (_, ps) -> ps.sumOf { it.damage } }

                // 각 참가자 점수 계산
                val scores =
                    match.participants.associateWith { p ->
                        calcScore(p, teamDamage[p.teamId] ?: 1, durationMin)
                    }

                // 팀 MVP: 팀 내 최고 점수
                for ((_, team) in byTeam) {
                    val mvp = team.maxByOrNull { scores[it] ?: 0.0 } ?: continue
                    val acc = accMap.getOrPut(mvp.riotId) { PlayerAcc() }
                    acc.mvpCount++
                    acc.mvpChampions[mvp.champion] = (acc.mvpChampions[mvp.champion] ?: 0) + 1
                    acc.mvpChampionIds[mvp.champion] = mvp.championId
                }

                // ACE: 전체 최고 점수
                val ace = match.participants.maxByOrNull { scores[it] ?: 0.0 }
                if (ace != null) accMap.getOrPut(ace.riotId) { PlayerAcc() }.aceCount++

                // 전체 점수 누적
                for (p in match.participants) {
                    val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                    acc.games++
                    acc.totalScore += scores[p] ?: 0.0
                }
            }

            fun r2(v: Double) = (v * 100).toInt() / 100.0

            val rankings =
                accMap.entries
                    .filter { it.value.games > 0 }
                    .map { (riotId, acc) ->
                        MvpPlayerStat(
                            riotId = riotId,
                            games = acc.games,
                            mvpCount = acc.mvpCount,
                            aceCount = acc.aceCount,
                            mvpRate = acc.mvpCount * 100 / acc.games,
                            avgMvpScore = r2(acc.totalScore / acc.games),
                            topChampion = acc.mvpChampions.maxByOrNull { it.value }?.key,
                            topChampionId =
                                acc.mvpChampions
                                    .maxByOrNull { it.value }
                                    ?.key
                                    ?.let { acc.mvpChampionIds[it] },
                        )
                    }.sortedWith(compareByDescending<MvpPlayerStat> { it.mvpCount }.thenByDescending { it.aceCount })

            MvpStatsResult(rankings, matches.size)
        }

    override fun getKillParticipation(mode: GameMode): KillParticipationResult =
        statsResultCacheQueryPort.getOrCompute("kill-participation:${mode.key}") {
            val matches = matchQueryPersistencePort.findAllWithParticipants(mode.queueIds)

            data class PlayerAcc(
                var games: Int = 0,
                var totalKp: Double = 0.0,
                var winKp: Double = 0.0,
                var winGames: Int = 0,
                var lossKp: Double = 0.0,
                var lossGames: Int = 0,
                var totalKills: Int = 0,
                var totalAssists: Int = 0,
            )

            val accMap = mutableMapOf<String, PlayerAcc>()

            for (match in matches) {
                val teams = match.participants.groupBy { it.team }
                for ((_, teamParts) in teams) {
                    val teamKills = maxOf(1, teamParts.sumOf { it.kills })
                    for (p in teamParts) {
                        val kp = (p.kills + p.assists).toDouble() / teamKills
                        val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                        acc.games++
                        acc.totalKp += kp
                        acc.totalKills += p.kills
                        acc.totalAssists += p.assists
                        if (p.win) {
                            acc.winKp += kp
                            acc.winGames++
                        } else {
                            acc.lossKp += kp
                            acc.lossGames++
                        }
                    }
                }
            }

            fun r2(v: Double) = (v * 10000).toInt() / 100.0

            val rankings =
                accMap.entries
                    .filter { it.value.games >= 1 }
                    .map { (riotId, acc) ->
                        val g = acc.games.toDouble()
                        KillParticipationEntry(
                            riotId = riotId,
                            games = acc.games,
                            avgKp = r2(acc.totalKp / g),
                            avgKpWin = if (acc.winGames > 0) r2(acc.winKp / acc.winGames) else 0.0,
                            avgKpLoss = if (acc.lossGames > 0) r2(acc.lossKp / acc.lossGames) else 0.0,
                            avgKills = (acc.totalKills.toDouble() / g * 10).toInt() / 10.0,
                            avgAssists = (acc.totalAssists.toDouble() / g * 10).toInt() / 10.0,
                        )
                    }.sortedByDescending { it.avgKp }

            KillParticipationResult(
                rankings = rankings,
                kpKing = rankings.firstOrNull()?.riotId,
            )
        }
}
