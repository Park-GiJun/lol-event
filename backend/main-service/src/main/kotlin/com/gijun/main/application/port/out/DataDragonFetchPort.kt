package com.gijun.main.application.port.out

import com.gijun.main.domain.model.dragon.DragonChampion
import com.gijun.main.domain.model.dragon.DragonItem
import com.gijun.main.domain.model.dragon.DragonRune
import com.gijun.main.domain.model.dragon.DragonSummonerSpell

interface DataDragonFetchPort {
    fun fetchLatestVersion(): String
    fun fetchChampions(version: String): List<DragonChampion>
    fun fetchItems(version: String): List<DragonItem>
    fun fetchSummonerSpells(version: String): List<DragonSummonerSpell>

    /** 룬 계열 5종과 그 안의 룬 전부. 계열 자체도 같은 목록에 포함된다. */
    fun fetchRunes(version: String): List<DragonRune>
}
