package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.MvpPlayerStat
import com.gijun.main.application.dto.stats.result.MvpStatsResult
import com.gijun.main.application.port.`in`.GetMvpStatsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.domain.model.match.MatchParticipant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMvpStatsHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetMvpStatsUseCase {

    /**
     * MVP 점수 계산 공식:
     *  - KDA 기여: (kills * 3 + assists * 1.5) / max(deaths, 1)
     *  - 팀 데미지 기여율: (내 데미지 / 팀 총 데미지) * 40
     *  - 시야: visionScore / gameDuration_min
     *  - CS: cs / gameDuration_min * 0.5
     *  - 승리 보너스: +20
     */
    private fun calcScore(p: MatchParticipant, teamTotalDamage: Int, durationMin: Double): Double {
        val kdaPart     = (p.kills * 3.0 + p.assists * 1.5) / maxOf(p.deaths, 1)
        val damagePart  = if (teamTotalDamage > 0) p.damage.toDouble() / teamTotalDamage * 40 else 0.0
        val visionPart  = if (durationMin > 0) p.visionScore / durationMin else 0.0
        val csPart      = if (durationMin > 0) p.cs / durationMin * 0.5 else 0.0
        val winBonus    = if (p.win) 20.0 else 0.0
        return kdaPart + damagePart + visionPart + csPart + winBonus
    }

    override fun getMvpStats(mode: String): MvpStatsResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

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
            val scores = match.participants.associateWith { p ->
                calcScore(p, teamDamage[p.teamId] ?: 1, durationMin)
            }

            // 팀 MVP: 팀 내 최고 점수
            for ((_, team) in byTeam) {
                val mvp = team.maxByOrNull { scores[it] ?: 0.0 } ?: continue
                accMap.getOrPut(mvp.riotId) { PlayerAcc() }.mvpCount++
                accMap[mvp.riotId]!!.mvpChampions[mvp.champion] =
                    (accMap[mvp.riotId]!!.mvpChampions[mvp.champion] ?: 0) + 1
                accMap[mvp.riotId]!!.mvpChampionIds[mvp.champion] = mvp.championId
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

        val rankings = accMap.entries
            .filter { it.value.games > 0 }
            .map { (riotId, acc) ->
                MvpPlayerStat(
                    riotId       = riotId,
                    games        = acc.games,
                    mvpCount     = acc.mvpCount,
                    aceCount     = acc.aceCount,
                    mvpRate      = acc.mvpCount * 100 / acc.games,
                    avgMvpScore  = r2(acc.totalScore / acc.games),
                    topChampion  = acc.mvpChampions.maxByOrNull { it.value }?.key,
                    topChampionId = acc.mvpChampions.maxByOrNull { it.value }?.key
                        ?.let { acc.mvpChampionIds[it] },
                )
            }
            .sortedWith(compareByDescending<MvpPlayerStat> { it.mvpCount }.thenByDescending { it.aceCount })

        return MvpStatsResult(rankings, matches.size)
    }
}
