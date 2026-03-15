package com.gijun.main.application.dto.dragon.result

import com.gijun.main.domain.model.dragon.DragonChampion
import com.gijun.main.domain.model.dragon.DragonItem
import com.gijun.main.domain.model.dragon.DragonSummonerSpell

data class DragonSyncResult(
    val version: String,
    val champions: Int,
    val items: Int,
    val spells: Int
)

data class DragonChampionResult(
    val championId: Int,
    val championKey: String,
    val nameKo: String,
    val titleKo: String?,
    val imageUrl: String?,
    val version: String?
) {
    companion object {
        fun from(domain: DragonChampion) = DragonChampionResult(
            championId = domain.championId, championKey = domain.championKey,
            nameKo = domain.nameKo, titleKo = domain.titleKo,
            imageUrl = domain.imageUrl, version = domain.version
        )
    }
}

data class DragonItemResult(
    val itemId: Int,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    val goldTotal: Int,
    val version: String?
) {
    companion object {
        fun from(domain: DragonItem) = DragonItemResult(
            itemId = domain.itemId, nameKo = domain.nameKo, description = domain.description,
            imageUrl = domain.imageUrl, goldTotal = domain.goldTotal, version = domain.version
        )
    }
}

data class DragonSummonerSpellResult(
    val spellId: Int,
    val spellKey: String,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    val version: String?
) {
    companion object {
        fun from(domain: DragonSummonerSpell) = DragonSummonerSpellResult(
            spellId = domain.spellId, spellKey = domain.spellKey, nameKo = domain.nameKo,
            description = domain.description, imageUrl = domain.imageUrl, version = domain.version
        )
    }
}
