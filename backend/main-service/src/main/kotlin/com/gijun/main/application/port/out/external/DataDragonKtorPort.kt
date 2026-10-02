package com.gijun.main.application.port.out.external

import com.gijun.main.domain.dragon.model.DragonChampionModel
import com.gijun.main.domain.dragon.model.DragonItemModel
import com.gijun.main.domain.dragon.model.DragonRuneModel
import com.gijun.main.domain.dragon.model.DragonSummonerSpellModel

interface DataDragonKtorPort {
    fun fetchLatestVersion(): String

    fun fetchChampions(version: String): List<DragonChampionModel>

    fun fetchItems(version: String): List<DragonItemModel>

    fun fetchSummonerSpells(version: String): List<DragonSummonerSpellModel>

    /** 룬 계열 5종과 그 안의 룬 전부. 계열 자체도 같은 목록에 포함된다. */
    fun fetchRunes(version: String): List<DragonRuneModel>
}
