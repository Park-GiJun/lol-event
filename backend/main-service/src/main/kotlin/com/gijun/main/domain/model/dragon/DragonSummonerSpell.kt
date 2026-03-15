package com.gijun.main.domain.model.dragon

data class DragonSummonerSpell(
    val spellId: Int,
    val spellKey: String,
    val nameKo: String,
    val description: String?,
    val imageFull: String?,
    val imageUrl: String?,
    val version: String?
)
