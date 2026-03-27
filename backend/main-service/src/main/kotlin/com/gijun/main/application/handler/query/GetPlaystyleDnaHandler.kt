package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.PlaystyleDnaEntry
import com.gijun.main.application.dto.stats.result.PlaystyleDnaResult
import com.gijun.main.application.port.`in`.GetPlaystyleDnaUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPlaystyleDnaHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetPlaystyleDnaUseCase {

    private fun r2(v: Double) = (v * 100).toInt() / 100.0

    override fun getPlaystyleDna(mode: String): PlaystyleDnaResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class PlayerRaw(
            val riotId: String,
            val games: Int,
            val aggression: Double,
            val durability: Double,
            val teamPlay: Double,
            val objectiveFocus: Double,
            val economy: Double,
            val visionControl: Double,
        )

        data class MatchParticipantStats(
            val kills: Int, val deaths: Int, val assists: Int,
            val doubleKills: Int, val tripleKills: Int, val pentaKills: Int,
            val longestTimeSpentLiving: Int,
            val damage: Int,
            val damageDealtToObjectives: Int,
            val damageDealtToTurrets: Int,
            val cs: Int,
            val gold: Int,
            val visionScore: Int,
            val wardsPlaced: Int,
            val wardsKilled: Int,
            val gameDuration: Int,
            val teamKills: Int,
        )

        // Collect per-match per-participant stats
        val allStats = mutableListOf<Pair<String, MatchParticipantStats>>()

        for (match in matches) {
            val durationSec = match.gameDuration

            for (teamId in listOf(100, 200)) {
                val teamParticipants = match.participants.filter { it.teamId == teamId }
                val teamKills = teamParticipants.sumOf { it.kills }

                for (p in teamParticipants) {
                    allStats.add(
                        p.riotId to MatchParticipantStats(
                            kills = p.kills,
                            deaths = p.deaths,
                            assists = p.assists,
                            doubleKills = p.doubleKills,
                            tripleKills = p.tripleKills,
                            pentaKills = p.pentaKills,
                            longestTimeSpentLiving = p.longestTimeSpentLiving,
                            damage = p.damage,
                            damageDealtToObjectives = p.damageDealtToObjectives,
                            damageDealtToTurrets = p.damageDealtToTurrets,
                            cs = p.cs,
                            gold = p.gold,
                            visionScore = p.visionScore,
                            wardsPlaced = p.wardsPlaced,
                            wardsKilled = p.wardsKilled,
                            gameDuration = durationSec,
                            teamKills = teamKills,
                        )
                    )
                }
            }
        }

        // Compute raw scores per player
        val rawByPlayer = allStats.groupBy { it.first }.map { (riotId, entries) ->
            val g = entries.size
            val stats = entries.map { it.second }

            val killsAvg = stats.map { it.kills }.average()
            val deathsAvg = stats.map { it.deaths }.average()
            val assistsAvg = stats.map { it.assists }.average()
            val doubleAvg = stats.map { it.doubleKills }.average()
            val tripleAvg = stats.map { it.tripleKills }.average()
            val pentaAvg = stats.map { it.pentaKills }.average()
            val longestLivingAvg = stats.map { it.longestTimeSpentLiving }.average()
            val durationAvg = stats.map { it.gameDuration }.average()
            val durationMinAvg = durationAvg / 60.0
            val kpAvg = stats.map { s ->
                val tk = s.teamKills
                if (tk > 0) (s.kills + s.assists).toDouble() / tk else 0.0
            }.average()
            val objAvg = stats.map { it.damageDealtToObjectives }.average()
            val turretAvg = stats.map { it.damageDealtToTurrets }.average()
            val csAvg = stats.map { it.cs }.average()
            val goldAvg = stats.map { it.gold }.average()
            val visionAvg = stats.map { it.visionScore }.average()
            val wardsPlacedAvg = stats.map { it.wardsPlaced }.average()
            val wardsKilledAvg = stats.map { it.wardsKilled }.average()

            val aggressionRaw = killsAvg * 3 + doubleAvg * 5 + tripleAvg * 8 + pentaAvg * 20
            val durabilityRaw = longestLivingAvg / maxOf(1.0, durationAvg) * 40 + (1.0 / maxOf(1.0, deathsAvg)) * 20
            val teamPlayRaw = assistsAvg * 2 + kpAvg * 30
            val objectiveFocusRaw = objAvg * 0.01 + turretAvg * 0.02
            val economyRaw = (csAvg / maxOf(1.0, durationMinAvg)) * 10 + goldAvg * 0.01
            val visionControlRaw = visionAvg / maxOf(1.0, durationMinAvg) * 20 + wardsPlacedAvg * 2 + wardsKilledAvg * 3

            PlayerRaw(riotId, g, aggressionRaw, durabilityRaw, teamPlayRaw, objectiveFocusRaw, economyRaw, visionControlRaw)
        }

        if (rawByPlayer.isEmpty()) return PlaystyleDnaResult(players = emptyList())

        // Min-max normalization per axis
        fun normalize(values: List<Double>): List<Double> {
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 0.0
            return if (max == min) values.map { 50.0 }
            else values.map { (it - min) / (max - min) * 100.0 }
        }

        val aggressionNorm = normalize(rawByPlayer.map { it.aggression })
        val durabilityNorm = normalize(rawByPlayer.map { it.durability })
        val teamPlayNorm = normalize(rawByPlayer.map { it.teamPlay })
        val objectiveFocusNorm = normalize(rawByPlayer.map { it.objectiveFocus })
        val economyNorm = normalize(rawByPlayer.map { it.economy })
        val visionControlNorm = normalize(rawByPlayer.map { it.visionControl })

        val players = rawByPlayer.indices.map { i ->
            val raw = rawByPlayer[i]
            val agg = r2(aggressionNorm[i])
            val dur = r2(durabilityNorm[i])
            val tp = r2(teamPlayNorm[i])
            val obj = r2(objectiveFocusNorm[i])
            val eco = r2(economyNorm[i])
            val vis = r2(visionControlNorm[i])

            val styleTag = when {
                agg > 70 -> "킬몰이형"
                tp > 70 && agg < 40 -> "팀빌더형"
                obj > 70 -> "오브젝트 사냥꾼"
                dur > 70 && tp > 60 -> "탱커 리더형"
                eco > 70 && obj > 60 -> "운영형"
                vis > 70 -> "정보 지배자"
                else -> "올라운더"
            }

            PlaystyleDnaEntry(
                riotId = raw.riotId,
                games = raw.games,
                aggression = agg,
                durability = dur,
                teamPlay = tp,
                objectiveFocus = obj,
                economy = eco,
                visionControl = vis,
                styleTag = styleTag,
            )
        }.sortedBy { it.riotId }

        return PlaystyleDnaResult(players = players)
    }
}
