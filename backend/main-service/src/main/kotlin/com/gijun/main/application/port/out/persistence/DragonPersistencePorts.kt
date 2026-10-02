package com.gijun.main.application.port.out.persistence

import com.gijun.main.domain.dragon.model.DragonChampionModel
import com.gijun.main.domain.dragon.model.DragonItemModel
import com.gijun.main.domain.dragon.model.DragonRuneModel
import com.gijun.main.domain.dragon.model.DragonSummonerSpellModel

interface DragonQueryPersistencePort {
    fun findAllChampions(): List<DragonChampionModel>

    fun findAllItems(): List<DragonItemModel>

    fun findAllSpells(): List<DragonSummonerSpellModel>

    fun findAllRunes(): List<DragonRuneModel>

    fun findChampionById(championId: Int): DragonChampionModel?

    fun findItemById(itemId: Int): DragonItemModel?

    fun findSpellById(spellId: Int): DragonSummonerSpellModel?

    fun findRuneById(runeId: Int): DragonRuneModel?
}

interface DragonCommandPersistencePort {
    fun saveAllChampions(champions: List<DragonChampionModel>)

    fun saveAllItems(items: List<DragonItemModel>)

    fun saveAllSpells(spells: List<DragonSummonerSpellModel>)

    fun saveAllRunes(runes: List<DragonRuneModel>)
}
