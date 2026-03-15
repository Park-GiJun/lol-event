package com.gijun.main.infrastructure.adapter.out.persistence.dragon.entity

import com.gijun.main.domain.model.dragon.DragonChampion
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ddragon_champions", schema = "lol_event")
class DragonChampionEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true) val championId: Int,
    @Column(nullable = false, unique = true) val championKey: String,
    @Column(nullable = false) var nameKo: String,
    @Column var titleKo: String? = null,
    @Column var imageFull: String? = null,
    @Column var imageUrl: String? = null,
    @Column var version: String? = null,
    @Column(nullable = false) var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain() = DragonChampion(
        championId = championId, championKey = championKey,
        nameKo = nameKo, titleKo = titleKo,
        imageFull = imageFull, imageUrl = imageUrl, version = version
    )

    companion object {
        fun from(domain: DragonChampion) = DragonChampionEntity(
            championId = domain.championId, championKey = domain.championKey,
            nameKo = domain.nameKo, titleKo = domain.titleKo,
            imageFull = domain.imageFull, imageUrl = domain.imageUrl, version = domain.version
        )
    }
}
