package com.gijun.main.infrastructure.adapter.out.persistence.match.entity

import com.gijun.main.domain.model.match.Match
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "matches", schema = "lol_event")
class MatchEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true) val matchId: String,
    @Column(nullable = false) val queueId: Int,
    @Column(nullable = false) val gameCreation: Long,
    @Column(nullable = false) val gameDuration: Int,
    @Column val gameMode: String? = null,
    @Column val gameType: String? = null,
    @Column val gameVersion: String? = null,
    @Column val mapId: Int? = null,
    @Column val seasonId: Int? = null,
    @Column val platformId: String? = null,
    @Column(nullable = false) val createdAt: LocalDateTime = LocalDateTime.now(),
    @OneToMany(mappedBy = "match", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val participants: MutableList<MatchParticipantEntity> = mutableListOf(),
    @OneToMany(mappedBy = "match", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val teams: MutableList<MatchTeamEntity> = mutableListOf()
) {
    fun toDomain() = Match(
        id = id, matchId = matchId, queueId = queueId,
        gameCreation = gameCreation, gameDuration = gameDuration,
        gameMode = gameMode, gameType = gameType, gameVersion = gameVersion,
        mapId = mapId, seasonId = seasonId, platformId = platformId, createdAt = createdAt,
        participants = participants.map { it.toDomain() }.toMutableList(),
        teams = teams.map { it.toDomain() }.toMutableList()
    )

    companion object {
        fun from(domain: Match) = MatchEntity(
            id = domain.id, matchId = domain.matchId, queueId = domain.queueId,
            gameCreation = domain.gameCreation, gameDuration = domain.gameDuration,
            gameMode = domain.gameMode, gameType = domain.gameType, gameVersion = domain.gameVersion,
            mapId = domain.mapId, seasonId = domain.seasonId, platformId = domain.platformId,
            createdAt = domain.createdAt
        )
    }
}
