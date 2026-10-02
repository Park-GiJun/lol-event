package com.gijun.main.infrastructure.adapter.`in`.web.dragon.dto

import com.gijun.main.application.dto.result.DragonChampionResult
import com.gijun.main.application.dto.result.DragonItemResult
import com.gijun.main.application.dto.result.DragonRuneResult
import com.gijun.main.application.dto.result.DragonSummonerSpellResult
import com.gijun.main.application.dto.result.DragonSyncResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "DragonSyncResult")
data class DragonSyncResponse(
    val version: String,
    val champions: Int,
    val items: Int,
    val spells: Int,
    val runes: Int,
) {
    companion object {
        fun from(result: DragonSyncResult) =
            DragonSyncResponse(
                version = result.version,
                champions = result.champions,
                items = result.items,
                spells = result.spells,
                runes = result.runes,
            )
    }
}

@Schema(name = "DragonChampionResult")
data class DragonChampionResponse(
    val championId: Int,
    val championKey: String,
    val nameKo: String,
    val titleKo: String?,
    val imageUrl: String?,
    val version: String?,
) {
    companion object {
        fun from(result: DragonChampionResult) =
            DragonChampionResponse(
                championId = result.championId,
                championKey = result.championKey,
                nameKo = result.nameKo,
                titleKo = result.titleKo,
                imageUrl = result.imageUrl,
                version = result.version,
            )
    }
}

@Schema(name = "DragonItemResult")
data class DragonItemResponse(
    val itemId: Int,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    val goldTotal: Int,
    val version: String?,
) {
    companion object {
        fun from(result: DragonItemResult) =
            DragonItemResponse(
                itemId = result.itemId,
                nameKo = result.nameKo,
                description = result.description,
                imageUrl = result.imageUrl,
                goldTotal = result.goldTotal,
                version = result.version,
            )
    }
}

@Schema(name = "DragonSummonerSpellResult")
data class DragonSummonerSpellResponse(
    val spellId: Int,
    val spellKey: String,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    val version: String?,
) {
    companion object {
        fun from(result: DragonSummonerSpellResult) =
            DragonSummonerSpellResponse(
                spellId = result.spellId,
                spellKey = result.spellKey,
                nameKo = result.nameKo,
                description = result.description,
                imageUrl = result.imageUrl,
                version = result.version,
            )
    }
}

@Schema(name = "DragonRuneResult")
data class DragonRuneResponse(
    val runeId: Int,
    val runeKey: String,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    @field:Schema(description = "소속 계열 id. 계열 행은 runeId 와 같다.")
    val styleId: Int,
    val styleNameKo: String?,
    @field:Schema(description = "계열 안 줄 번호. 0 = 핵심 룬, -1 = 계열 자신.")
    val slot: Int,
    val version: String?,
) {
    companion object {
        fun from(result: DragonRuneResult) =
            DragonRuneResponse(
                runeId = result.runeId,
                runeKey = result.runeKey,
                nameKo = result.nameKo,
                description = result.description,
                imageUrl = result.imageUrl,
                styleId = result.styleId,
                styleNameKo = result.styleNameKo,
                slot = result.slot,
                version = result.version,
            )
    }
}
