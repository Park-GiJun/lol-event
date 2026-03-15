package com.gijun.main.application.port.out

data class RiotAccount(val puuid: String, val gameName: String, val tagLine: String)

interface RiotApiPort {
    fun getAccount(gameName: String, tagLine: String): RiotAccount
}
