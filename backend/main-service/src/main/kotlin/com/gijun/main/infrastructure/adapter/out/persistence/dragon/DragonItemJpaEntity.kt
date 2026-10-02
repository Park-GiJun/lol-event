package com.gijun.main.infrastructure.adapter.out.persistence.dragon

import com.gijun.main.domain.dragon.model.DragonItemModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "ddragon_items", schema = "lol_event")
class DragonItemJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true) val itemId: Int,
    @Column(nullable = false) var nameKo: String,
    @Column(columnDefinition = "TEXT") var description: String? = null,
    @Column var imageFull: String? = null,
    @Column var imageUrl: String? = null,
    @Column var goldTotal: Int = 0,
    @Column var version: String? = null,
    @Column(nullable = false) var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toModel() =
        DragonItemModel(
            itemId = itemId,
            nameKo = nameKo,
            description = description,
            imageFull = imageFull,
            imageUrl = imageUrl,
            goldTotal = goldTotal,
            version = version,
        )

    companion object {
        fun from(domain: DragonItemModel) =
            DragonItemJpaEntity(
                itemId = domain.itemId,
                nameKo = domain.nameKo,
                description = domain.description,
                imageFull = domain.imageFull,
                imageUrl = domain.imageUrl,
                goldTotal = domain.goldTotal,
                version = domain.version,
            )
    }
}
