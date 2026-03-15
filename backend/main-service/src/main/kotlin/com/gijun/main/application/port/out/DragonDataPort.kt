package com.gijun.main.application.port.out

import com.gijun.main.domain.model.dragon.DragonChampion
import com.gijun.main.domain.model.dragon.DragonItem
import com.gijun.main.domain.model.dragon.DragonSummonerSpell

interface DragonDataPort {
    fun saveAllChampions(champions: List<DragonChampion>)
    fun saveAllItems(items: List<DragonItem>)
    fun saveAllSpells(spells: List<DragonSummonerSpell>)

    fun findAllChampions(): List<DragonChampion>
    fun findAllItems(): List<DragonItem>
    fun findAllSpells(): List<DragonSummonerSpell>

    fun findChampionById(championId: Int): DragonChampion?
    fun findItemById(itemId: Int): DragonItem?
    fun findSpellById(spellId: Int): DragonSummonerSpell?
}
