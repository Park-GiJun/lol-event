package com.gijun.main.application.port.out

import com.gijun.main.domain.model.dragon.DragonChampion
import com.gijun.main.domain.model.dragon.DragonItem
import com.gijun.main.domain.model.dragon.DragonSummonerSpell

interface DataDragonFetchPort {
    fun fetchLatestVersion(): String
    fun fetchChampions(version: String): List<DragonChampion>
    fun fetchItems(version: String): List<DragonItem>
    fun fetchSummonerSpells(version: String): List<DragonSummonerSpell>
}
