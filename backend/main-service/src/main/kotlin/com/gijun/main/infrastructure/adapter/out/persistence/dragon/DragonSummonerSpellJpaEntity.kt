package com.gijun.main.infrastructure.adapter.out.persistence.dragon

import com.gijun.main.domain.dragon.model.DragonSummonerSpellModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "ddragon_summoner_spells", schema = "lol_event")
class DragonSummonerSpellJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true) val spellId: Int,
    @Column(nullable = false, unique = true) val spellKey: String,
    @Column(nullable = false) var nameKo: String,
    @Column(columnDefinition = "TEXT") var description: String? = null,
    @Column var imageFull: String? = null,
    @Column var imageUrl: String? = null,
    @Column var version: String? = null,
    @Column(nullable = false) var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toModel() =
        DragonSummonerSpellModel(
            spellId = spellId,
            spellKey = spellKey,
            nameKo = nameKo,
            description = description,
            imageFull = imageFull,
            imageUrl = imageUrl,
            version = version,
        )

    companion object {
        fun from(domain: DragonSummonerSpellModel) =
            DragonSummonerSpellJpaEntity(
                spellId = domain.spellId,
                spellKey = domain.spellKey,
                nameKo = domain.nameKo,
                description = domain.description,
                imageFull = domain.imageFull,
                imageUrl = domain.imageUrl,
                version = domain.version,
            )
    }
}
