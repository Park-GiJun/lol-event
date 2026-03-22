package com.gijun.main.infrastructure.adapter.out.persistence.batch.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "player_stats_snapshot", schema = "lol_event",
    uniqueConstraints = [UniqueConstraint(columnNames = ["riot_id", "mode"])])
class PlayerStatsCacheEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val riotId: String,
    @Column(nullable = false) val mode: String,
    @Column(nullable = false) val games: Int = 0,
    @Column(nullable = false) val wins: Int = 0,
    @Column(nullable = false) val losses: Int = 0,
    @Column(nullable = false) val winRate: Int = 0,
    @Column(nullable = false, columnDefinition = "NUMERIC(6,2)") val avgKills: Double = 0.0,
    @Column(nullable = false, columnDefinition = "NUMERIC(6,2)") val avgDeaths: Double = 0.0,
    @Column(nullable = false, columnDefinition = "NUMERIC(6,2)") val avgAssists: Double = 0.0,
    @Column(nullable = false, columnDefinition = "NUMERIC(6,2)") val kda: Double = 0.0,
    @Column(nullable = false) val avgDamage: Int = 0,
    @Column(nullable = false, columnDefinition = "NUMERIC(6,2)") val avgCs: Double = 0.0,
    @Column(nullable = false) val avgGold: Int = 0,
    @Column(nullable = false, columnDefinition = "NUMERIC(6,2)") val avgVisionScore: Double = 0.0,
    @Column val topChampion: String? = null,
    @Column(nullable = false) val aggregatedAt: LocalDateTime = LocalDateTime.now(),
)
