package com.gijun.main.infrastructure.adapter.`in`.web.match.dto

import com.gijun.main.application.dto.result.ReassignPositionsResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "ReassignPositionsResult")
data class ReassignPositionsResponse(
    val matchesScanned: Int,
    val matchesSkippedAram: Int,
    val teamsScanned: Int,
    val teamsAlreadyValid: Int,
    val teamsFixed: Int,
    val participantsUpdated: Int,
) {
    companion object {
        fun from(result: ReassignPositionsResult) =
            ReassignPositionsResponse(
                matchesScanned = result.matchesScanned,
                matchesSkippedAram = result.matchesSkippedAram,
                teamsScanned = result.teamsScanned,
                teamsAlreadyValid = result.teamsAlreadyValid,
                teamsFixed = result.teamsFixed,
                participantsUpdated = result.participantsUpdated,
            )
    }
}
