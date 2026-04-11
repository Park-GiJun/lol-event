package com.gijun.main.application.port.out

data class RiotAccount(val puuid: String, val gameName: String, val tagLine: String)

data class SummonerData(
    val id: String,
    val accountId: String,
    val puuid: String,
    val profileIconId: Int,
    val summonerLevel: Long,
)

data class RankedEntry(
    val queueType: String,
    val tier: String,
    val rank: String,
    val leaguePoints: Int,
    val wins: Int,
    val losses: Int,
)

data class ChampionMasteryData(
    val championId: Int,
    val championLevel: Int,
    val championPoints: Int,
)

interface RiotApiPort {
    fun getAccount(gameName: String, tagLine: String): RiotAccount
    fun getSummonerByPuuid(puuid: String): SummonerData?
    fun getRankedEntries(summonerId: String): List<RankedEntry>
    fun getChampionMastery(puuid: String, top: Int = 10): List<ChampionMasteryData>
}
