package com.gijun.main.infrastructure.adapter.out.persistence.dragon

import com.gijun.main.domain.dragon.model.DragonChampionModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "ddragon_champions", schema = "lol_event")
class DragonChampionJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true) val championId: Int,
    @Column(nullable = false, unique = true) val championKey: String,
    @Column(nullable = false) var nameKo: String,
    @Column var titleKo: String? = null,
    @Column var imageFull: String? = null,
    @Column var imageUrl: String? = null,
    @Column var version: String? = null,
    @Column(nullable = false) var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toModel() =
        DragonChampionModel(
            championId = championId,
            championKey = championKey,
            nameKo = nameKo,
            titleKo = titleKo,
            imageFull = imageFull,
            imageUrl = imageUrl,
            version = version,
        )

    companion object {
        fun from(domain: DragonChampionModel) =
            DragonChampionJpaEntity(
                championId = domain.championId,
                championKey = domain.championKey,
                nameKo = domain.nameKo,
                titleKo = domain.titleKo,
                imageFull = domain.imageFull,
                imageUrl = domain.imageUrl,
                version = domain.version,
            )
    }
}
