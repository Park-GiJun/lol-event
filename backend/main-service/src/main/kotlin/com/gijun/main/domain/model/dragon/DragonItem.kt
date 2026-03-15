package com.gijun.main.domain.model

data class DragonItem(
    val itemId: Int,
    val nameKo: String,
    val description: String?,
    val imageFull: String?,
    val imageUrl: String?,
    val goldTotal: Int,
    val version: String?
)
