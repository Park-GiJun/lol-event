package com.gijun.main.domain.service

import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.Position

/**
 * Stateless domain service that assigns positions to match participants.
 * Logic is identical to the original EloCalculationHandler implementation.
 */
object PositionDetector {

    /**
     * Assigns a position to each participant in the team.
     * Returns a map of riotId -> Position.
     */
    fun assignPositions(team: List<MatchParticipant>): Map<String, Position> {
        val assigned = mutableMapOf<String, Position>()
        val taken = mutableSetOf<Position>()
        val pending = mutableListOf<MatchParticipant>()

        for (p in team) {
            val pos = primaryPosition(p)
            if (pos != null && pos !in taken) {
                assigned[p.riotId] = pos; taken.add(pos)
            } else {
                pending.add(p)
            }
        }

        val remaining = mutableListOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)
            .apply { removeAll(taken) }

        // Bottom lane disambiguation
        if (Position.ADC in remaining && Position.SUPPORT in remaining) {
            val bottomPending = pending.filter {
                it.lane == "BOTTOM" || it.role in listOf("DUO_CARRY", "DUO_SUPPORT", "DUO")
            }
            if (bottomPending.size == 2) {
                val adc = bottomPending.maxByOrNull { it.damage + it.gold }!!
                val sup = bottomPending.first { it.riotId != adc.riotId }
                assigned[adc.riotId] = Position.ADC;     remaining.remove(Position.ADC)
                assigned[sup.riotId] = Position.SUPPORT;  remaining.remove(Position.SUPPORT)
                pending.removeAll(bottomPending.toSet())
            }
        }

        // Greedy scoring for remaining positions
        for (pos in remaining.toList()) {
            if (pending.isEmpty()) break
            val best = pending.maxByOrNull { positionScore(it, pos) }!!
            assigned[best.riotId] = pos
            pending.remove(best); remaining.remove(pos)
        }
        return assigned
    }

    /**
     * Assigns positions to all participants in a match (both teams),
     * returning a new list of participants with assignedPosition filled in.
     */
    fun assignPositionsToAll(participants: List<MatchParticipant>): List<MatchParticipant> {
        val teamGroups = participants.groupBy { it.teamId }
        return participants.map { p ->
            val team = teamGroups[p.teamId] ?: listOf(p)
            val positions = assignPositions(team)
            val pos = positions[p.riotId] ?: Position.UNKNOWN
            p.copy(assignedPosition = pos.name)
        }
    }

    fun primaryPosition(p: MatchParticipant): Position? = when {
        p.lane == "TOP"                                 -> Position.TOP
        p.lane == "JUNGLE"                              -> Position.JUNGLE
        p.lane == "MIDDLE"                              -> Position.MID
        p.lane == "BOTTOM" && p.role == "CARRY"         -> Position.ADC
        p.lane == "BOTTOM" && p.role == "DUO_CARRY"     -> Position.ADC
        p.lane == "BOTTOM" && p.role == "SUPPORT"       -> Position.SUPPORT
        p.lane == "BOTTOM" && p.role == "DUO_SUPPORT"   -> Position.SUPPORT
        else                                            -> null
    }

    fun positionScore(p: MatchParticipant, pos: Position): Double = when (pos) {
        Position.TOP     -> p.cs * 0.35 + p.damage * 0.3 + p.totalDamageTaken * 0.25 + p.turretKills * 5.0
        Position.JUNGLE  -> p.neutralMinionsKilled * 2.5 + p.damageDealtToObjectives * 0.5
        Position.MID     -> p.damage * 0.6 + p.cs * 0.4
        Position.ADC     -> p.damage * 0.5 + p.gold * 0.3 + p.cs * 0.2
        Position.SUPPORT -> p.visionScore * 3.0 + p.timeCCingOthers * 1.5 + p.wardsPlaced * 2.0
        Position.UNKNOWN -> 0.0
    }
}
