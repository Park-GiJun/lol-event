package com.gijun.main.infrastructure.adapter.out.cache

import com.gijun.main.application.dto.dragon.result.DragonChampionResult
import com.gijun.main.application.dto.dragon.result.DragonItemResult
import com.gijun.main.application.dto.dragon.result.DragonRuneResult
import com.gijun.main.application.dto.dragon.result.DragonSummonerSpellResult
import com.gijun.main.application.port.out.DataDragonCachePort
import com.gijun.main.application.port.out.DragonDataPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class DataDragonCacheStore(private val dragonDataPort: DragonDataPort) : DataDragonCachePort {

    private val log = LoggerFactory.getLogger(javaClass)

    private val champions = ConcurrentHashMap<Int, DragonChampionResult>()
    private val items = ConcurrentHashMap<Int, DragonItemResult>()
    private val spells = ConcurrentHashMap<Int, DragonSummonerSpellResult>()
    private val runes = ConcurrentHashMap<Int, DragonRuneResult>()

    override fun warmUp() {
        champions.clear()
        items.clear()
        spells.clear()
        runes.clear()

        dragonDataPort.findAllChampions().forEach { champions[it.championId] = DragonChampionResult.from(it) }
        dragonDataPort.findAllItems().forEach { items[it.itemId] = DragonItemResult.from(it) }
        dragonDataPort.findAllSpells().forEach { spells[it.spellId] = DragonSummonerSpellResult.from(it) }
        dragonDataPort.findAllRunes().forEach { runes[it.runeId] = DragonRuneResult.from(it) }

        log.info("[DataDragon Cache] warmUp 완료 - 챔피언: ${champions.size}, 아이템: ${items.size}, 스펠: ${spells.size}, 룬: ${runes.size}")
    }

    fun getChampion(championId: Int): DragonChampionResult? = champions[championId]
    fun getItem(itemId: Int): DragonItemResult? = items[itemId]
    fun getSpell(spellId: Int): DragonSummonerSpellResult? = spells[spellId]
    fun getRune(runeId: Int): DragonRuneResult? = runes[runeId]

    fun getAllChampions(): List<DragonChampionResult> = champions.values.sortedBy { it.nameKo }
    fun getAllItems(): List<DragonItemResult> = items.values.sortedBy { it.itemId }
    fun getAllSpells(): List<DragonSummonerSpellResult> = spells.values.sortedBy { it.spellId }

    /** 계열 → 줄 → 룬 순. 화면에서 룬 페이지 모양대로 그리기 좋게 정렬해 둔다. */
    fun getAllRunes(): List<DragonRuneResult> = runes.values.sortedWith(compareBy({ it.styleId }, { it.slot }, { it.runeId }))

    fun isLoaded() = champions.isNotEmpty()
}
