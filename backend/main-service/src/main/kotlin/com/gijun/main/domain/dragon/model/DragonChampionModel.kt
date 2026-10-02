package com.gijun.main.domain.dragon.model

data class DragonChampionModel(
    val championId: Int,
    val championKey: String,
    val nameKo: String,
    val titleKo: String?,
    val imageFull: String?,
    val imageUrl: String?,
    val version: String?,
)
