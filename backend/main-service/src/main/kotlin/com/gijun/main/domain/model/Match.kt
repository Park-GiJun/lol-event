package com.gijun.main.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "matches", schema = "lol_event")
class Match(
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
    val participants: MutableList<MatchParticipant> = mutableListOf(),
    @OneToMany(mappedBy = "match", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val teams: MutableList<MatchTeam> = mutableListOf()
)
