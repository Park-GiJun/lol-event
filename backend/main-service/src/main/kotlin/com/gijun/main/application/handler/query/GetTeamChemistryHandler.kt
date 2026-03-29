package com.gijun.main.application.handler.query

import com.gijun.main.application.dto.stats.result.TeamChemistryEntry
import com.gijun.main.application.dto.stats.result.TeamChemistryResult
import com.gijun.main.application.port.`in`.GetTeamChemistryUseCase
import com.gijun.main.application.port.out.MatchPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.gijun.main.infrastructure.adapter.out.cache.StatsQueryCache

@Service
@Transactional(readOnly = true)
class GetTeamChemistryHandler(
    private val matchPersistencePort: MatchPersistencePort,
    private val cache: StatsQueryCache,
) : GetTeamChemistryUseCase {

    private fun <T> List<T>.combinations(n: Int): List<List<T>> {
        if (n == 0) return listOf(emptyList())
        if (this.isEmpty() || this.size < n) return emptyList()
        val head = this.first()
        val tail = this.drop(1)
        val withHead = tail.combinations(n - 1).map { listOf(head) + it }
        val withoutHead = tail.combinations(n)
        return withHead + withoutHead
    }

    override fun getTeamChemistry(mode: String, minGames: Int): TeamChemistryResult = cache.getOrCompute("team-chemistry:$mode:$minGames") {
        val matches = matchPersistencePort.findAllWithParticipants(modeToQueueIds(mode))

        data class ChemRecord(var games: Int = 0, var wins: Int = 0)

        val duoMap = mutableMapOf<String, ChemRecord>()
        val trioMap = mutableMapOf<String, ChemRecord>()
        val fullTeamMap = mutableMapOf<String, ChemRecord>()

        for (match in matches) {
            for (teamId in listOf(100, 200)) {
                val teamPlayers = match.participants.filter { it.teamId == teamId }
                val won = teamPlayers.any { it.win }
                val sortedIds = teamPlayers.map { it.riotId }.sorted()

                // 2-person combos
                for (combo in sortedIds.combinations(2)) {
                    val key = combo.joinToString("|")
                    val r = duoMap.getOrPut(key) { ChemRecord() }
                    r.games++
                    if (won) r.wins++
                }

                // 3-person combos
                for (combo in sortedIds.combinations(3)) {
                    val key = combo.joinToString("|")
                    val r = trioMap.getOrPut(key) { ChemRecord() }
                    r.games++
                    if (won) r.wins++
                }

                // 5-person full team (only if exactly 5 players)
                if (sortedIds.size == 5) {
                    val key = sortedIds.joinToString("|")
                    val r = fullTeamMap.getOrPut(key) { ChemRecord() }
                    r.games++
                    if (won) r.wins++
                }
            }
        }

        fun Map<String, ChemRecord>.toEntries(size: Int): List<TeamChemistryEntry> =
            this.entries
                .filter { it.value.games >= minGames }
                .map { (key, r) ->
                    TeamChemistryEntry(
                        players = key.split("|"),
                        games = r.games,
                        wins = r.wins,
                        winRate = if (r.games > 0) r.wins * 100 / r.games else 0,
                        compositionSize = size,
                    )
                }

        val duoEntries = duoMap.toEntries(2).sortedByDescending { it.winRate }
        val trioEntries = trioMap.toEntries(3).sortedByDescending { it.winRate }
        val fullTeamEntries = fullTeamMap.toEntries(5).sortedByDescending { it.winRate }

        TeamChemistryResult(
            bestDuos = duoEntries.take(10),
            bestTrios = trioEntries.take(10),
            bestFullTeams = fullTeamEntries.take(5),
            worstDuos = duoEntries.sortedBy { it.winRate }.take(10),
        )
    }
}
