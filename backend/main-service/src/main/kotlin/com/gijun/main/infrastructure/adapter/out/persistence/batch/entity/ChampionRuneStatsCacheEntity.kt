package com.gijun.main.infrastructure.adapter.out.persistence.batch.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "champion_rune_stats_snapshot",
    schema = "lol_event",
    uniqueConstraints = [UniqueConstraint(columnNames = ["champion", "mode", "keystone", "primary_style", "sub_style"])],
)
class ChampionRuneStatsCacheEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val champion: String,
    @Column(nullable = false) val mode: String,
    @Column(nullable = false) val keystone: Int,
    @Column(name = "primary_style", nullable = false) val primaryStyle: Int,
    @Column(name = "sub_style", nullable = false) val subStyle: Int,
    @Column(nullable = false) val picks: Int = 0,
    @Column(nullable = false) val wins: Int = 0,
    @Column(nullable = false) val winRate: Int = 0,
    @Column(nullable = false) val aggregatedAt: LocalDateTime = LocalDateTime.now(),
)
