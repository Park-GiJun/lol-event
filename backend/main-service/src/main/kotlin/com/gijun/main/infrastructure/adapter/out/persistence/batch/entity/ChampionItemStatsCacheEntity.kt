package com.gijun.main.infrastructure.adapter.out.persistence.batch.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "champion_item_stats_snapshot",
    schema = "lol_event",
    uniqueConstraints = [UniqueConstraint(columnNames = ["champion", "mode", "item_id"])],
)
class ChampionItemStatsCacheEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val champion: String,
    @Column(nullable = false) val mode: String,
    @Column(name = "item_id", nullable = false) val itemId: Int,
    @Column(nullable = false) val picks: Int = 0,
    @Column(nullable = false) val wins: Int = 0,
    @Column(nullable = false) val winRate: Int = 0,
    @Column(nullable = false) val aggregatedAt: LocalDateTime = LocalDateTime.now(),
)
