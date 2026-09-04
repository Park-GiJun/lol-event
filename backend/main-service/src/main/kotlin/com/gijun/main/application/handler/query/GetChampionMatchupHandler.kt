package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionLaneStrength
import com.gijun.main.application.dto.stats.result.ChampionMatchupResult
import com.gijun.main.application.dto.stats.result.LaneGap
import com.gijun.main.application.dto.stats.result.MatchupStat
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.application.port.out.StatsCachePort
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.service.RankingScore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 챔피언 상성.
 *
 * 예전 구현은 두 가지가 잘못돼 있었다.
 *
 * 1. samePosition 이 기본 false 라 탑 챔피언과 상대 서포터가 "상성" 으로 잡혔다.
 *    한 경기에서 한 사람이 상대 다섯 명과 전부 짝지어지니 표본은 다섯 배로 부풀지만
 *    내용은 라인전과 아무 관계가 없다. 이제 같은 라인끼리만 맞춘다.
 * 2. 관측 승률로 정렬해서 1경기 1승이 맨 위에 왔다. 축소 보정 값으로 정렬한다.
 *
 * 그리고 승률만으로는 "왜 유리한지" 를 못 본다. 상대 라이너 대비 골드·CS·딜량·킬·시야
 * 격차를 같이 낸다. 이게 상성의 실제 내용이다.
 */
@Service
@Transactional(readOnly = true)
class GetChampionMatchupHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsCachePort,
) : GetChampionMatchupUseCase {

    /** 라인전 1:1. 같은 라인에 정확히 한 명씩일 때만 만든다. */
    private data class Duel(
        val me: MatchParticipant,
        val opp: MatchParticipant,
        val position: String,
    )

    override fun getMatchup(champion: String?, vsChampion: String?, mode: String): ChampionMatchupResult =
        cache.getOrCompute("champion-matchup:$champion:$vsChampion:$mode") {
            val duels = buildDuels(mode)

            when {
                champion != null -> {
                    val mine = duels.filter { it.me.champion.equals(champion, ignoreCase = true) }
                    val name = mine.firstOrNull()?.me?.champion ?: champion
                    val id = mine.firstOrNull()?.me?.championId ?: 0
                    ChampionMatchupResult(
                        champion = name,
                        championId = id,
                        laneStrength = laneStrength(mine),
                        matchups = matchups(mine) { it.opp },
                        minGames = RankingScore.MIN_GAMES,
                    )
                }

                vsChampion != null -> {
                    // 카운터 관점 — 이 챔피언을 상대한 쪽의 성적. 시점을 뒤집어 같은 계산을 쓴다.
                    val against = duels
                        .filter { it.opp.champion.equals(vsChampion, ignoreCase = true) }
                        .map { Duel(me = it.opp, opp = it.me, position = it.position) }
                    val name = against.firstOrNull()?.me?.champion ?: vsChampion
                    val id = against.firstOrNull()?.me?.championId ?: 0
                    ChampionMatchupResult(
                        champion = name,
                        championId = id,
                        laneStrength = laneStrength(against),
                        matchups = matchups(against) { it.opp },
                        minGames = RankingScore.MIN_GAMES,
                    )
                }

                else -> ChampionMatchupResult("", 0, emptyList(), emptyList(), RankingScore.MIN_GAMES)
            }
        }

    // ────────── 라인전 1:1 만들기 ──────────

    private fun buildDuels(mode: String): List<Duel> {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))
        return matches.flatMap { m ->
            val teams = m.participants.groupBy { it.teamId }
            if (teams.size != 2) return@flatMap emptyList()
            val (a, b) = teams.values.toList()

            // 같은 라인에 정확히 한 명씩일 때만 맞춘다. 포지션이 깨진 팀은 조용히 건너뛴다.
            val byPosB = b.groupBy { PositionResolver.resolve(it) }
            a.mapNotNull { me ->
                val pos = PositionResolver.resolve(me) ?: return@mapNotNull null
                val opp = byPosB[pos]?.singleOrNull() ?: return@mapNotNull null
                Duel(me, opp, pos)
            }.flatMap { d ->
                // 양쪽 관점을 모두 담는다. a 팀만 담으면 b 팀 챔피언 조회가 비게 된다.
                listOf(d, Duel(me = d.opp, opp = d.me, position = d.position))
            }
        }
    }

    // ────────── 집계 ──────────

    private fun gapOf(duels: List<Duel>): LaneGap {
        val n = duels.size.coerceAtLeast(1)
        fun avg(pick: (Duel) -> Double) = duels.sumOf(pick) / n
        return LaneGap(
            goldDiff = avg { (it.me.gold - it.opp.gold).toDouble() }.toInt(),
            csDiff = r1(avg { (it.me.cs - it.opp.cs).toDouble() }),
            damageDiff = avg { (it.me.damage - it.opp.damage).toDouble() }.toInt(),
            killDiff = r1(avg { (it.me.kills - it.opp.kills).toDouble() }),
            visionDiff = r1(avg { (it.me.visionScore - it.opp.visionScore).toDouble() }),
        )
    }

    /** 챔피언 x 라인. 개별 상성보다 표본이 두터워 이쪽이 실제로 읽힌다. */
    private fun laneStrength(duels: List<Duel>): List<ChampionLaneStrength> =
        duels.groupBy { it.position }
            .map { (pos, group) ->
                val w = group.count { it.me.win }
                ChampionLaneStrength(
                    champion = group.first().me.champion,
                    championId = group.first().me.championId,
                    position = pos,
                    games = group.size,
                    wins = w,
                    winRate = w * 100 / group.size,
                    adjustedWinRate = r2(RankingScore.shrunkWinRate(w, group.size)),
                    sampleGrade = RankingScore.sampleGrade(group.size),
                    gap = gapOf(group),
                )
            }
            .sortedByDescending { it.games }

    private fun matchups(duels: List<Duel>, key: (Duel) -> MatchParticipant): List<MatchupStat> =
        duels.groupBy { key(it).champion }
            .map { (_, group) ->
                val w = group.count { it.me.win }
                MatchupStat(
                    opponent = key(group.first()).champion,
                    opponentId = key(group.first()).championId,
                    position = group.first().position,
                    games = group.size,
                    wins = w,
                    winRate = w * 100 / group.size,
                    adjustedWinRate = r2(RankingScore.shrunkWinRate(w, group.size)),
                    sampleGrade = RankingScore.sampleGrade(group.size),
                    gap = gapOf(group),
                )
            }
            .filter { it.games >= RankingScore.MIN_GAMES }
            .sortedWith(compareByDescending<MatchupStat> { it.adjustedWinRate }.thenByDescending { it.games })

    private fun r1(v: Double) = Math.round(v * 10) / 10.0
    private fun r2(v: Double) = Math.round(v * 100) / 100.0
}
