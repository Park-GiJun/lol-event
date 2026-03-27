package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionLengthTendency
import com.gijun.main.application.dto.stats.result.GameLengthBucket
import com.gijun.main.application.dto.stats.result.GameLengthTendencyEntry
import com.gijun.main.application.dto.stats.result.GameLengthTendencyResult
import com.gijun.main.application.port.`in`.GetGameLengthTendencyUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetGameLengthTendencyHandler(private val matchPersistencePort: MatchPersistencePort) : GetGameLengthTendencyUseCase {

    private enum class LengthBand { SHORT, MID, LONG }

    private fun bandOf(gameDuration: Int): LengthBand = when {
        gameDuration < 1500  -> LengthBand.SHORT
        gameDuration < 2100  -> LengthBand.MID
        else                 -> LengthBand.LONG
    }

    override fun getGameLengthTendency(mode: String): GameLengthTendencyResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        fun r1(v: Double) = (v * 10).toInt() / 10.0

        data class BucketAcc(
            var games: Int = 0,
            var wins: Int = 0,
            var kills: Int = 0,
            var deaths: Int = 0,
            var damage: Long = 0L,
            var cs: Int = 0,
            var durationSec: Long = 0L,
        )

        fun BucketAcc.toBucket(): GameLengthBucket {
            val g = games.coerceAtLeast(1)
            val durationMin = durationSec.toDouble() / 60.0
            val avgMin = if (games > 0) durationMin / games else 1.0
            return GameLengthBucket(
                games = games,
                wins = wins,
                winRate = if (games > 0) wins * 100 / games else 0,
                avgKills = r1(kills.toDouble() / g),
                avgDeaths = r1(deaths.toDouble() / g),
                avgDamage = r1(damage.toDouble() / g),
                avgCsPerMin = if (avgMin > 0) r1(cs.toDouble() / g / avgMin) else 0.0,
            )
        }

        // 플레이어별 구간별 집계
        data class PlayerAcc(
            val short: BucketAcc = BucketAcc(),
            val mid: BucketAcc = BucketAcc(),
            val long: BucketAcc = BucketAcc(),
        )

        val playerMap = mutableMapOf<String, PlayerAcc>()

        // 챔피언별 구간별 집계
        data class ChampAcc(
            val champion: String,
            var championId: Int = 0,
            val short: BucketAcc = BucketAcc(),
            val mid: BucketAcc = BucketAcc(),
            val long: BucketAcc = BucketAcc(),
        )

        val champMap = mutableMapOf<String, ChampAcc>()

        for (m in matches) {
            val band = bandOf(m.gameDuration)
            for (p in m.participants) {
                val pAcc = playerMap.getOrPut(p.riotId) { PlayerAcc() }
                val bAcc = when (band) {
                    LengthBand.SHORT -> pAcc.short
                    LengthBand.MID   -> pAcc.mid
                    LengthBand.LONG  -> pAcc.long
                }
                bAcc.games++
                if (p.win) bAcc.wins++
                bAcc.kills += p.kills
                bAcc.deaths += p.deaths
                bAcc.damage += p.damage
                bAcc.cs += p.cs
                bAcc.durationSec += m.gameDuration

                val cAcc = champMap.getOrPut(p.champion) { ChampAcc(p.champion) }
                cAcc.championId = p.championId
                val cbAcc = when (band) {
                    LengthBand.SHORT -> cAcc.short
                    LengthBand.MID   -> cAcc.mid
                    LengthBand.LONG  -> cAcc.long
                }
                cbAcc.games++
                if (p.win) cbAcc.wins++
                cbAcc.durationSec += m.gameDuration
            }
        }

        val players = playerMap.entries.map { (riotId, acc) ->
            val shortBucket = acc.short.toBucket()
            val midBucket = acc.mid.toBucket()
            val longBucket = acc.long.toBucket()
            val totalGames = acc.short.games + acc.mid.games + acc.long.games

            val sWr = shortBucket.winRate
            val lWr = longBucket.winRate
            val tendency = when {
                sWr > lWr + 15             -> "단기전형"
                lWr > sWr + 15             -> "장기전형"
                sWr > 55 && lWr > 55       -> "전천후"
                else                       -> "중기전형"
            }

            GameLengthTendencyEntry(
                riotId = riotId,
                totalGames = totalGames,
                shortGame = shortBucket,
                midGame = midBucket,
                longGame = longBucket,
                tendency = tendency,
            )
        }.sortedByDescending { it.totalGames }

        val championTendencies = champMap.values.map { acc ->
            val sWr = if (acc.short.games > 0) acc.short.wins * 100 / acc.short.games else 0
            val mWr = if (acc.mid.games > 0)   acc.mid.wins   * 100 / acc.mid.games   else 0
            val lWr = if (acc.long.games > 0)  acc.long.wins  * 100 / acc.long.games  else 0
            val bestLength = when (maxOf(sWr, mWr, lWr)) {
                sWr  -> "단기전"
                lWr  -> "장기전"
                else -> "중기전"
            }
            ChampionLengthTendency(
                champion = acc.champion,
                championId = acc.championId,
                shortWinRate = sWr,
                midWinRate = mWr,
                longWinRate = lWr,
                bestLength = bestLength,
            )
        }.sortedByDescending { acc -> acc.shortWinRate + acc.midWinRate + acc.longWinRate }

        return GameLengthTendencyResult(
            players = players,
            championTendencies = championTendencies,
        )
    }
}
