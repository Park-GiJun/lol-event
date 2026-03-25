package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.ChampionMatchupResult
import com.gijun.main.application.dto.stats.result.MatchupStat
import com.gijun.main.application.port.`in`.GetChampionMatchupUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetChampionMatchupHandler(
    private val matchPersistencePort: MatchPersistencePort,
) : GetChampionMatchupUseCase {

    override fun getMatchup(champion: String?, vsChampion: String?, mode: String, samePosition: Boolean): ChampionMatchupResult {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        // (myChampion, myChampionId, opponentChampion, opponentId, myWin) 쌍 생성
        data class Pair(
            val myChamp: String, val myChampId: Int,
            val oppChamp: String, val oppId: Int,
            val myWin: Boolean,
        )

        val pairs = matches.flatMap { m ->
            val byTeam = m.participants.groupBy { it.teamId }
            byTeam.values.flatMap { myTeam ->
                val enemies = m.participants.filter { it.teamId != myTeam.first().teamId }
                myTeam.flatMap { me ->
                    val opponents = if (samePosition) {
                        val myPos = normalizePosition(me.lane, me.role) ?: return@flatMap emptyList()
                        enemies.filter { normalizePosition(it.lane, it.role) == myPos }
                    } else {
                        enemies
                    }
                    opponents.map { opp ->
                        Pair(me.champion, me.championId, opp.champion, opp.championId, me.win)
                    }
                }
            }
        }

        return when {
            champion != null -> {
                // X 의 관점: X vs 각 상대 챔피언 승률
                val filtered = pairs.filter { it.myChamp.equals(champion, ignoreCase = true) }
                val champName = filtered.firstOrNull()?.myChamp ?: champion
                val champId   = filtered.firstOrNull()?.myChampId ?: 0

                val matchups = filtered
                    .groupBy { it.oppChamp }
                    .map { (opp, es) ->
                        val w = es.count { it.myWin }
                        MatchupStat(opp, es.first().oppId, es.size, w, w * 100 / es.size)
                    }
                    .filter { it.games >= 2 }
                    .sortedByDescending { it.winRate }

                ChampionMatchupResult(champName, champId, matchups)
            }

            vsChampion != null -> {
                // 카운터픽 관점: vsChampion 을 상대한 챔피언들의 승률 (높을수록 카운터)
                val filtered = pairs.filter { it.oppChamp.equals(vsChampion, ignoreCase = true) }
                val vsName = filtered.firstOrNull()?.oppChamp ?: vsChampion
                val vsId   = filtered.firstOrNull()?.oppId ?: 0

                val matchups = filtered
                    .groupBy { it.myChamp }
                    .map { (myChamp, es) ->
                        val w = es.count { it.myWin }
                        MatchupStat(myChamp, es.first().myChampId, es.size, w, w * 100 / es.size)
                    }
                    .filter { it.games >= 2 }
                    .sortedByDescending { it.winRate }

                ChampionMatchupResult(vsName, vsId, matchups)
            }

            else -> ChampionMatchupResult("", 0, emptyList())
        }
    }

    /** LCU lane/role → 정규화 포지션 (같은 라인 매칭용) */
    private fun normalizePosition(lane: String?, role: String?): String? = when {
        lane == "TOP"                                                         -> "TOP"
        lane == "JUNGLE"                                                      -> "JUNGLE"
        lane == "MIDDLE"                                                      -> "MID"
        lane == "BOTTOM" && role in listOf("CARRY", "DUO_CARRY")             -> "BOT"
        lane == "BOTTOM" && role in listOf("SUPPORT", "DUO_SUPPORT")         -> "SUP"
        lane == "BOTTOM" && role == "SOLO"                                   -> "BOT"
        else                                                                  -> null
    }
}
