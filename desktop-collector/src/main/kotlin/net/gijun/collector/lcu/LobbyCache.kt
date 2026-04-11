package net.gijun.collector.lcu

import net.gijun.collector.api.DuoSynergy
import net.gijun.collector.api.PlayerStats
import net.gijun.collector.api.RivalEntry

data class CachedPlayer(val riotId: String, val summonerName: String, val isMe: Boolean)

object LobbyCache {
    var blueTeam: List<CachedPlayer> = emptyList()
        private set
    var redTeam: List<CachedPlayer> = emptyList()
        private set
    var cachedAt: Long = 0
        private set

    // PlayerStats from backend, keyed by riotId
    val playerStats: MutableMap<String, PlayerStats> = mutableMapOf()

    // Duo synergy data for current lobby
    var duoSynergies: List<DuoSynergy> = emptyList()

    // Rival matchups between blue and red teams
    var rivalMatchups: List<RivalEntry> = emptyList()

    fun isValid(): Boolean = blueTeam.isNotEmpty() || redTeam.isNotEmpty()

    fun updateFromLobby(blue: List<TeamMemberInfo>, red: List<TeamMemberInfo>) {
        blueTeam = blue.map { CachedPlayer(it.riotId, it.summonerName, it.isMe) }
        redTeam = red.map { CachedPlayer(it.riotId, it.summonerName, it.isMe) }
        cachedAt = System.currentTimeMillis()
    }

    fun getEnemyTeam(iAmBlue: Boolean): List<CachedPlayer> = if (iAmBlue) redTeam else blueTeam
    fun getMyTeam(iAmBlue: Boolean): List<CachedPlayer> = if (iAmBlue) blueTeam else redTeam

    fun iAmBlue(): Boolean = blueTeam.any { it.isMe }

    fun clear() {
        blueTeam = emptyList()
        redTeam = emptyList()
        playerStats.clear()
        duoSynergies = emptyList()
        rivalMatchups = emptyList()
        cachedAt = 0
    }
}
