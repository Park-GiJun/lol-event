package com.gijun.main.domain.dragon.model

data class DragonSummonerSpellModel(
    val spellId: Int,
    val spellKey: String,
    val nameKo: String,
    val description: String?,
    val imageFull: String?,
    val imageUrl: String?,
    val version: String?,
)
