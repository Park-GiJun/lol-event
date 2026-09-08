package com.gijun.main.infrastructure.adapter.out.persistence.dragon.entity

import com.gijun.main.domain.model.dragon.DragonRune
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ddragon_runes", schema = "lol_event")
class DragonRuneEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true) val runeId: Int,
    @Column(nullable = false) var runeKey: String,
    @Column(nullable = false) var nameKo: String,
    @Column(columnDefinition = "TEXT") var description: String? = null,
    @Column var iconPath: String? = null,
    @Column var imageUrl: String? = null,
    @Column(nullable = false) var styleId: Int = 0,
    @Column var styleNameKo: String? = null,
    @Column(nullable = false) var slot: Int = 0,
    @Column var version: String? = null,
    @Column(nullable = false) var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toDomain() = DragonRune(
        runeId = runeId, runeKey = runeKey, nameKo = nameKo, description = description,
        iconPath = iconPath, imageUrl = imageUrl, styleId = styleId, styleNameKo = styleNameKo,
        slot = slot, version = version,
    )

    companion object {
        fun from(domain: DragonRune) = DragonRuneEntity(
            runeId = domain.runeId, runeKey = domain.runeKey, nameKo = domain.nameKo,
            description = domain.description, iconPath = domain.iconPath, imageUrl = domain.imageUrl,
            styleId = domain.styleId, styleNameKo = domain.styleNameKo, slot = domain.slot,
            version = domain.version,
        )
    }
}
