package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionPickStat
import com.gijun.main.application.dto.stats.result.OverviewStats
import com.gijun.main.application.dto.stats.result.PlayerLeaderStat
import com.gijun.main.application.port.`in`.GetOverviewStatsUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import com.gijun.main.domain.model.match.MatchParticipant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetOverviewStatsHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetOverviewStatsUseCase {

    private fun r1(v: Double) = (v * 10).toInt() / 10.0
    private fun r2(v: Double) = (v * 100).toInt() / 100.0

    /** 참가자 + 해당 경기의 게임 시간(초) */
    private data class Entry(val p: MatchParticipant, val durationSec: Int) {
        val minutes: Double get() = if (durationSec > 0) durationSec / 60.0 else 1.0
    }

    override fun getOverviewStats(mode: String): OverviewStats {
        val matches  = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))
        val allTeams = matches.flatMap { it.teams }

        // 참가자에 게임시간 결합
        val allEntries = matches.flatMap { m ->
            m.participants.map { Entry(it, m.gameDuration) }
        }
        val allP = allEntries.map { it.p }

        // ── 챔피언 픽 통계 ────────────────────────────────────
        val champStats = allP.groupBy { it.champion }.map { (champ, ps) ->
            val wins = ps.count { it.win }
            ChampionPickStat(
                champion   = champ,
                championId = ps.first().championId,
                picks      = ps.size,
                wins       = wins,
                winRate    = wins * 100 / ps.size,
            )
        }
        val topPicked   = champStats.sortedByDescending { it.picks }.take(15)
        val topWinRate  = champStats.filter { it.picks >= 3 }.sortedByDescending { it.winRate }.take(8)

        // ── 플레이어별 집계 ───────────────────────────────────
        val byPlayer = allEntries.groupBy { it.p.riotId }
        val MIN = 3

        /**
         * @param minGames 최소 경기 수
         * @param score    플레이어 점수 함수 (높을수록 우수)
         * @param display  표시 문자열 함수
         */
        fun leader(
            minGames: Int = MIN,
            score:   (List<Entry>) -> Double,
            display: (List<Entry>, Double) -> String,
        ): PlayerLeaderStat? =
            byPlayer
                .filter  { it.value.size >= minGames }
                .mapValues { (_, es) -> score(es) }
                .maxByOrNull { it.value }
                ?.let { (riotId, v) ->
                    PlayerLeaderStat(riotId, display(byPlayer[riotId]!!, v), byPlayer[riotId]!!.size)
                }

        // 공통 계산 헬퍼
        fun perMin(es: List<Entry>, sum: (Entry) -> Int): Double {
            val total   = es.sumOf(sum).toDouble()
            val minutes = es.sumOf { it.durationSec } / 60.0
            return if (minutes > 0) total / minutes else 0.0
        }
        fun perGame(es: List<Entry>, sum: (Entry) -> Int): Double =
            es.sumOf(sum).toDouble() / es.size

        fun kdaOf(es: List<Entry>): Double {
            val k = es.sumOf { it.p.kills }
            val d = es.sumOf { it.p.deaths }
            val a = es.sumOf { it.p.assists }
            return if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()
        }

        // ── 명예의 전당 — per match ────────────────────────────
        val winRateLeader = leader(
            score   = { es -> perGame(es) { if (it.p.win) 1 else 0 } * 100 },
            display = { _, v -> "${v.toInt()}%" },
        )

        val kdaLeader = leader(
            score   = { es -> kdaOf(es) },
            display = { _, v -> "${r2(v)} KDA" },
        )

        val killsLeader = leader(
            score   = { es -> perGame(es) { it.p.kills } },
            display = { _, v -> "${r1(v)} 킬/경기" },
        )

        // ── 명예의 전당 — per minute ──────────────────────────
        val damageLeader = leader(
            score   = { es -> perMin(es) { it.p.damage } },
            display = { _, v -> "${v.toLong().toLocaleString()}/분" },
        )

        val goldLeader = leader(
            score   = { es -> perMin(es) { it.p.gold } },
            display = { _, v -> "${v.toLong().toLocaleString()}/분" },
        )

        val csLeader = leader(
            score   = { es -> perMin(es) { it.p.cs } },
            display = { _, v -> "${r1(v)} CS/분" },
        )

        val visionLeader = leader(
            score   = { es -> perMin(es) { it.p.visionScore } },
            display = { _, v -> "${r1(v)} 점/분" },
        )

        val objectiveDamageLeader = leader(
            score   = { es -> perMin(es) { it.p.damageDealtToObjectives } },
            display = { _, v -> "${v.toLong().toLocaleString()}/분" },
        )

        // ── 명예의 전당 — per match (기타) ───────────────────
        val turretKillsLeader = leader(
            score   = { es -> perGame(es) { it.p.turretKills } },
            display = { _, v -> "${r1(v)} 포탑/경기" },
        )

        val pentaKillsLeader = leader(
            minGames = 1,
            score    = { es -> es.sumOf { it.p.pentaKills }.toDouble() },
            display  = { _, v -> "${v.toInt()}회" },
        )

        val wardsLeader = leader(
            score   = { es -> perGame(es) { it.p.wardsPlaced } },
            display = { _, v -> "${r1(v)} 개/경기" },
        )

        val ccLeader = leader(
            score   = { es -> perMin(es) { it.p.timeCCingOthers } },
            display = { _, v -> "${r1(v)} 초/분" },
        )

        val mostGamesPlayed = byPlayer
            .maxByOrNull { it.value.size }
            ?.let { (riotId, es) -> PlayerLeaderStat(riotId, "${es.size}판", es.size) }

        // ── 전체 오브젝트 + 경기 시간 집계 ──────────────────
        val totalDurationSec = matches.sumOf { it.gameDuration }
        val avgGameMin       = if (matches.isNotEmpty())
            r1(totalDurationSec.toDouble() / matches.size / 60.0) else 0.0

        return OverviewStats(
            matchCount           = matches.size,
            avgGameMinutes       = avgGameMin,
            topPickedChampions   = topPicked,
            topWinRateChampions  = topWinRate,
            winRateLeader        = winRateLeader,
            kdaLeader            = kdaLeader,
            killsLeader          = killsLeader,
            damageLeader         = damageLeader,
            goldLeader           = goldLeader,
            csLeader             = csLeader,
            visionLeader         = visionLeader,
            objectiveDamageLeader = objectiveDamageLeader,
            turretKillsLeader    = turretKillsLeader,
            pentaKillsLeader     = pentaKillsLeader,
            wardsLeader          = wardsLeader,
            ccLeader             = ccLeader,
            mostGamesPlayed      = mostGamesPlayed,
            totalBaronKills      = allTeams.sumOf { it.baronKills },
            totalDragonKills     = allTeams.sumOf { it.dragonKills },
            totalTowerKills      = allTeams.sumOf { it.towerKills },
            totalRiftHeraldKills = allTeams.sumOf { it.riftHeraldKills },
            totalInhibitorKills  = allTeams.sumOf { it.inhibitorKills },
            totalFirstBloods     = matches.count { m -> m.teams.any { it.firstBlood } },
        )
    }

    private fun Long.toLocaleString() = String.format("%,d", this)
}
