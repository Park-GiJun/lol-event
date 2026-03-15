package com.gijun.main.infrastructure.adapter.out.persistence.batch.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "champion_stats_snapshot", schema = "lol_event",
    uniqueConstraints = [UniqueConstraint(columnNames = ["champion", "mode"])])
class ChampionStatsCacheEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val champion: String,
    @Column(nullable = false) val championId: Int = 0,
    @Column(nullable = false) val mode: String,
    @Column(nullable = false) val games: Int = 0,
    @Column(nullable = false) val wins: Int = 0,
    @Column(nullable = false) val winRate: Int = 0,
    @Column(nullable = false) val totalBans: Int = 0,
    @Column(nullable = false) val aggregatedAt: LocalDateTime = LocalDateTime.now(),
)
