package com.gijun.main.application.dto

data class DragonChampionDto(
    val championId: Int,
    val championKey: String,
    val nameKo: String,
    val titleKo: String?,
    val imageUrl: String?,
    val version: String?
)

data class DragonItemDto(
    val itemId: Int,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    val goldTotal: Int,
    val version: String?
)

data class DragonSummonerSpellDto(
    val spellId: Int,
    val spellKey: String,
    val nameKo: String,
    val description: String?,
    val imageUrl: String?,
    val version: String?
)

data class DragonSyncResponse(
    val version: String,
    val champions: Int,
    val items: Int,
    val spells: Int
)
