package com.gijun.main.application.handler.query

import com.gijun.main.domain.model.match.MatchParticipant

/**
 * 포지션 해석 유틸리티 — 모든 쿼리 핸들러에서 공유.
 *
 * 우선순위:
 * 1. assignedPosition (매치 저장 시 PositionDetector가 계산한 값)
 * 2. lane + role 기반 폴백 (assignedPosition이 비어 있는 과거 데이터 대응)
 */
object PositionResolver {

    /** TOP / JUNGLE / MID / ADC / SUPPORT / null */
    fun resolve(p: MatchParticipant): String? {
        // 1차: 저장된 assignedPosition (v1.0.1+에서 저장됨)
        val assigned = p.assignedPosition.takeIf { it.isNotBlank() && it != "UNKNOWN" }
        if (assigned != null) return normalizeLabel(assigned)

        // 2차: lane + role 폴백
        return fromLaneRole(p.lane, p.role, p)
    }

    private fun fromLaneRole(lane: String?, role: String?, p: MatchParticipant): String? = when {
        lane == "TOP" -> "TOP"
        lane == "JUNGLE" -> "JUNGLE"
        lane == "MIDDLE" || lane == "MID" -> "MID"
        lane == "BOTTOM" && role in listOf("SUPPORT", "DUO_SUPPORT") -> "SUPPORT"
        lane == "BOTTOM" && role in listOf("CARRY", "DUO_CARRY") -> "ADC"
        lane == "BOTTOM" -> {
            // role이 없으면 스탯 기반 추정 (딜+골드 높으면 ADC, 시야 높으면 서포터)
            if (p.damage + p.gold > p.visionScore * 100) "ADC" else "SUPPORT"
        }
        else -> null
    }

    private fun normalizeLabel(pos: String): String = when (pos.uppercase()) {
        "TOP" -> "TOP"
        "JUNGLE" -> "JUNGLE"
        "MID" -> "MID"
        "ADC" -> "ADC"
        "SUPPORT" -> "SUPPORT"
        else -> pos.uppercase()
    }
}
