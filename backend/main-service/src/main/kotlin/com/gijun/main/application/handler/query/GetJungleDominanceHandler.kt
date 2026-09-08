package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.JungleDominanceEntry
import com.gijun.main.application.dto.stats.result.JungleDominanceResult
import com.gijun.main.application.port.`in`.GetJungleDominanceUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.application.port.out.StatsCachePort

@Service
@Transactional(readOnly = true)
class GetJungleDominanceHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetJungleDominanceUseCase {

    companion object {
        /** 파밍형으로 가르는 평균 정글 몹 수. 정글러 표본 상위 25% 경계. */
        private const val FARMING_JUNGLE_CS = 195.0
    }

    fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getJungleDominance(mode: String): JungleDominanceResult = cache.getOrCompute("jungle-dominance:$mode") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class PlayerAcc(
            var games: Int = 0,
            var totalInvadeRatio: Double = 0.0,
            var totalObjShare: Double = 0.0,
            var totalKp: Double = 0.0,
            var totalJungleCs: Double = 0.0,
            var totalVisionPerMin: Double = 0.0,
            val champions: MutableMap<String, Int> = mutableMapOf(),
            val championIds: MutableMap<String, Int> = mutableMapOf(),
        )

        val accMap = mutableMapOf<String, PlayerAcc>()

        for (match in matches) {
            val byTeam = match.participants.groupBy { it.teamId }
            val teamKills = byTeam.mapValues { (_, ps) -> ps.sumOf { it.kills } }
            val teamObjDamage = byTeam.mapValues { (_, ps) -> ps.sumOf { it.damageDealtToObjectives } }

            val durationMin = maxOf(1.0, match.gameDuration / 60.0)
            val junglers = match.participants.filter { it.neutralMinionsKilled >= 30 }

            for (p in junglers) {
                val acc = accMap.getOrPut(p.riotId) { PlayerAcc() }
                val teamKillsTotal = teamKills[p.teamId] ?: 1
                val teamObjTotal = teamObjDamage[p.teamId] ?: 1

                val invadeRatio = p.neutralMinionsKilledEnemyJungle.toDouble() / maxOf(1, p.neutralMinionsKilled)
                val objShare = p.damageDealtToObjectives.toDouble() / maxOf(1, teamObjTotal)
                val kp = (p.kills + p.assists).toDouble() / maxOf(1, teamKillsTotal)
                val visionPerMin = p.visionScore / durationMin

                acc.games++
                acc.totalInvadeRatio += invadeRatio
                acc.totalObjShare += objShare
                acc.totalKp += kp
                acc.totalJungleCs += p.neutralMinionsKilled.toDouble()
                acc.totalVisionPerMin += visionPerMin
                acc.champions[p.champion] = (acc.champions[p.champion] ?: 0) + 1
                acc.championIds[p.champion] = p.championId
            }
        }

        val rankings = accMap.entries
            .filter { it.value.games > 0 }
            .map { (riotId, acc) ->
                val g = acc.games.toDouble()
                val avgInvadeRatio = acc.totalInvadeRatio / g
                val avgObjShare = acc.totalObjShare / g
                val avgKp = acc.totalKp / g
                val avgJungleCs = acc.totalJungleCs / g
                val avgVisionPerMin = acc.totalVisionPerMin / g

                // 카정 비율(avgInvadeRatio)은 빼 뒀다.
                // neutral_minions_killed_enemy_jungle 이 수집분 1,716행 전부 0 이라 항상 0 이 나온다.
                // 예전에는 이 항에 0.25 를 주고 있어서 점수가 통째로 4분의 3 스케일로 눌려 있었다.
                // 남은 셋에 비중을 나눠 실제 0~1 범위를 되찾는다. 순위 자체는 예전과 같다
                // (상수 0 이 빠지는 것이라 상대 순서는 안 바뀐다).
                val jungleDominance =
                    avgObjShare * 0.40 +
                    avgKp * 0.40 +
                    avgVisionPerMin * 0.20

                // 태그도 같은 이유로 다시 짰다. 예전 조건은
                //   "공격형"      = 카정 > 0.15 → 절대 참이 안 됨. 죽은 가지였다.
                //   "안전 갱킹형" = 킬관여 > 0.6 && 카정 < 0.1 → 뒤 조건이 항상 참이라 사실상 킬관여만 봤다.
                // 카정 대신 정글 몹 수로 파밍형을 가른다. 기준값 195 는 실제 정글러 309표본의
                // 상위 25% 경계다(중앙값 171, 최대 351).
                val playStyleTag = when {
                    avgObjShare > 0.35 -> "오브젝트 특화"
                    avgKp > 0.6 -> "갱킹형"
                    avgJungleCs >= FARMING_JUNGLE_CS -> "파밍형"
                    else -> "밸런스형"
                }

                val topChampion = acc.champions.maxByOrNull { it.value }?.key

                JungleDominanceEntry(
                    riotId = riotId,
                    games = acc.games,
                    avgInvadeRatio = r2(avgInvadeRatio),
                    avgObjShare = r2(avgObjShare),
                    avgKp = r2(avgKp),
                    avgJungleCs = r2(avgJungleCs),
                    avgJungleDominance = r2(jungleDominance),
                    playStyleTag = playStyleTag,
                    topChampion = topChampion,
                    topChampionId = topChampion?.let { acc.championIds[it] },
                )
            }
            .sortedByDescending { it.avgJungleDominance }

        JungleDominanceResult(rankings = rankings)
    }
}
