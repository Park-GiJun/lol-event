package com.gijun.main.domain.dragon.model

data class DragonItemModel(
    val itemId: Int,
    val nameKo: String,
    val description: String?,
    val imageFull: String?,
    val imageUrl: String?,
    val goldTotal: Int,
    val version: String?,
)
