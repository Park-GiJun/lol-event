package com.gijun.main.infrastructure.cache

import com.gijun.main.application.dto.DragonChampionDto
import com.gijun.main.application.dto.DragonItemDto
import com.gijun.main.application.dto.DragonSummonerSpellDto
import com.gijun.main.application.port.out.DragonDataPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class DataDragonCacheStore(private val dragonDataPort: DragonDataPort) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val champions = ConcurrentHashMap<Int, DragonChampionDto>()
    private val items = ConcurrentHashMap<Int, DragonItemDto>()
    private val spells = ConcurrentHashMap<Int, DragonSummonerSpellDto>()

    fun warmUp() {
        champions.clear()
        items.clear()
        spells.clear()

        dragonDataPort.findAllChampions().forEach {
            champions[it.championId] = DragonChampionDto(
                championId = it.championId, championKey = it.championKey,
                nameKo = it.nameKo, titleKo = it.titleKo,
                imageUrl = it.imageUrl, version = it.version
            )
        }
        dragonDataPort.findAllItems().forEach {
            items[it.itemId] = DragonItemDto(
                itemId = it.itemId, nameKo = it.nameKo, description = it.description,
                imageUrl = it.imageUrl, goldTotal = it.goldTotal, version = it.version
            )
        }
        dragonDataPort.findAllSpells().forEach {
            spells[it.spellId] = DragonSummonerSpellDto(
                spellId = it.spellId, spellKey = it.spellKey, nameKo = it.nameKo,
                description = it.description, imageUrl = it.imageUrl, version = it.version
            )
        }

        log.info("[DataDragon Cache] warmUp 완료 - 챔피언: ${champions.size}, 아이템: ${items.size}, 스펠: ${spells.size}")
    }

    fun getChampion(championId: Int): DragonChampionDto? = champions[championId]
    fun getItem(itemId: Int): DragonItemDto? = items[itemId]
    fun getSpell(spellId: Int): DragonSummonerSpellDto? = spells[spellId]

    fun getAllChampions(): List<DragonChampionDto> = champions.values.sortedBy { it.nameKo }
    fun getAllItems(): List<DragonItemDto> = items.values.sortedBy { it.itemId }
    fun getAllSpells(): List<DragonSummonerSpellDto> = spells.values.sortedBy { it.spellId }

    fun isLoaded() = champions.isNotEmpty()
}
