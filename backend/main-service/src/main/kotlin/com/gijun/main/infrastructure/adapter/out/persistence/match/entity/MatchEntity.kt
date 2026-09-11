package com.gijun.main.infrastructure.adapter.out.persistence.match.entity

import com.gijun.main.domain.model.match.LaneMethod
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
    /** 이 경기의 라인 승자 판정 방법. 검증용 메타데이터라 계산에는 쓰지 않고 기록만 한다. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16) val laneMethod: LaneMethod? = null,
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
        mapId = mapId, seasonId = seasonId, platformId = platformId,
        laneMethod = laneMethod,
        // timelineRaw 는 일부러 싣지 않는다. 별도 테이블에 있고 무거워서, 필요한 쪽이 따로 가져간다.
        createdAt = createdAt,
        participants = participants.map { it.toDomain() }.toMutableList(),
        teams = teams.map { it.toDomain() }.toMutableList()
    )

    companion object {
        fun from(domain: Match) = MatchEntity(
            id = domain.id, matchId = domain.matchId, queueId = domain.queueId,
            gameCreation = domain.gameCreation, gameDuration = domain.gameDuration,
            gameMode = domain.gameMode, gameType = domain.gameType, gameVersion = domain.gameVersion,
            mapId = domain.mapId, seasonId = domain.seasonId, platformId = domain.platformId,
            laneMethod = domain.laneMethod,
            createdAt = domain.createdAt
        )
    }
}
