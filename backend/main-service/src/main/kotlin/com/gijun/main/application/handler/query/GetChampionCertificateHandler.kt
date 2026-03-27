package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionCertEntry
import com.gijun.main.application.dto.stats.result.ChampionCertificateResult
import com.gijun.main.application.port.`in`.GetChampionCertificateUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetChampionCertificateHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetChampionCertificateUseCase {

    private fun r1(v: Double) = (v * 10).toInt() / 10.0
    private fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getChampionCertificates(mode: String, minGames: Int): ChampionCertificateResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class PlayRecord(
            val riotId: String,
            val champion: String,
            val championId: Int,
            val win: Boolean,
            val kills: Int,
            val deaths: Int,
            val assists: Int,
            val damage: Int,
        )

        // Group by (riotId, champion)
        val grouped = matches
            .flatMap { m -> m.participants.map { p -> PlayRecord(p.riotId, p.champion, p.championId, p.win, p.kills, p.deaths, p.assists, p.damage) } }
            .groupBy { "${it.riotId}||${it.champion}" }

        val allEntries = grouped.map { (_, records) ->
            val g = records.size
            val w = records.count { it.win }
            val winRate = w * 100 / g
            val k = records.sumOf { it.kills }
            val d = records.sumOf { it.deaths }
            val a = records.sumOf { it.assists }
            val kda = if (d > 0) r2((k + a).toDouble() / d) else (k + a).toDouble()
            val avgDamage = r2(records.sumOf { it.damage }.toDouble() / g)
            val rep = records.first()
            ChampionCertEntry(
                riotId = rep.riotId,
                champion = rep.champion,
                championId = rep.championId,
                games = g,
                wins = w,
                winRate = winRate,
                avgKills = r1(k.toDouble() / g),
                avgDeaths = r1(d.toDouble() / g),
                avgAssists = r1(a.toDouble() / g),
                kda = kda,
                avgDamage = avgDamage,
                certified = g >= minGames && winRate >= 50,
            )
        }

        val certifiedMasters = allEntries
            .filter { it.certified }
            .sortedByDescending { it.winRate }

        // topChampionMasters: for each champion, the player with best win rate (among those with >= minGames)
        val topChampionMasters = allEntries
            .filter { it.games >= minGames }
            .groupBy { it.champion }
            .mapValues { (_, entries) -> entries.maxByOrNull { it.winRate }!! }

        return ChampionCertificateResult(
            certifiedMasters = certifiedMasters,
            topChampionMasters = topChampionMasters,
        )
    }
}
