package com.gijun.main.application.dto.result

import com.gijun.main.domain.dragon.model.DragonChampionModel
import com.gijun.main.domain.dragon.model.DragonItemModel
import com.gijun.main.domain.dragon.model.DragonRuneModel
import com.gijun.main.domain.dragon.model.DragonSummonerSpellModel

data class DragonSyncResult(
    val version: String,
    val champions: Int,
    val items: Int,
    val spells: Int,
    val runes: Int,
)

data class DragonChampionResult(
    val championId: Int,
    val championKey: String,
    val nameKo: String,
    val titleKo: String?,
    val imageUrl: String?,
    val version: String?,
) {
    companion object {
        fun from(domain: DragonChampionModel) =
            DragonChampionResult(
                championId = domain.championId,
                championKey = domain.championKey,
                nameKo = domain.nameKo,
                titleKo = domain.titleKo,
                imageUrl = domain.imageUrl,
                version = domain.version,
            )
    }
}

data class DragonItemResult(
    val itemId: Int,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    val goldTotal: Int,
    val version: String?,
) {
    companion object {
        fun from(domain: DragonItemModel) =
            DragonItemResult(
                itemId = domain.itemId,
                nameKo = domain.nameKo,
                description = domain.description,
                imageUrl = domain.imageUrl,
                goldTotal = domain.goldTotal,
                version = domain.version,
            )
    }
}

data class DragonSummonerSpellResult(
    val spellId: Int,
    val spellKey: String,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    val version: String?,
) {
    companion object {
        fun from(domain: DragonSummonerSpellModel) =
            DragonSummonerSpellResult(
                spellId = domain.spellId,
                spellKey = domain.spellKey,
                nameKo = domain.nameKo,
                description = domain.description,
                imageUrl = domain.imageUrl,
                version = domain.version,
            )
    }
}

data class DragonRuneResult(
    val runeId: Int,
    val runeKey: String,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    /** 소속 계열 id. 계열 행은 runeId 와 같다. */
    val styleId: Int,
    val styleNameKo: String?,
    /** 계열 안 줄 번호. 0 = 핵심 룬, -1 = 계열 자신. */
    val slot: Int,
    val version: String?,
) {
    companion object {
        fun from(domain: DragonRuneModel) =
            DragonRuneResult(
                runeId = domain.runeId,
                runeKey = domain.runeKey,
                nameKo = domain.nameKo,
                description = domain.description,
                imageUrl = domain.imageUrl,
                styleId = domain.styleId,
                styleNameKo = domain.styleNameKo,
                slot = domain.slot,
                version = domain.version,
            )
    }
}
