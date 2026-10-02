package com.gijun.main.application.dto.result

data class ReassignPositionsResult(
    val matchesScanned: Int,
    val matchesSkippedAram: Int,
    val teamsScanned: Int,
    val teamsAlreadyValid: Int,
    val teamsFixed: Int,
    val participantsUpdated: Int,
)
