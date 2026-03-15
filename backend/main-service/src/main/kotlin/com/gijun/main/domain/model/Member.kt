package com.gijun.main.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "members", schema = "lol_event")
class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val riotId: String,
    @Column(nullable = false, unique = true) val puuid: String,
    @Column(nullable = false) val registeredAt: LocalDateTime = LocalDateTime.now()
)
