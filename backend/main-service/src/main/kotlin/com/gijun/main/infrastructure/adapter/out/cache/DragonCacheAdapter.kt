package com.gijun.main.infrastructure.adapter.out.cache

import com.gijun.main.application.port.out.cache.DragonCacheCommandPort
import com.gijun.main.application.port.out.cache.DragonCacheQueryPort
import com.gijun.main.domain.dragon.model.DragonChampionModel
import com.gijun.main.domain.dragon.model.DragonItemModel
import com.gijun.main.domain.dragon.model.DragonRuneModel
import com.gijun.main.domain.dragon.model.DragonSummonerSpellModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Data Dragon 인메모리 캐시.
 *
 * 조회·적재 포트를 한 클래스가 구현한다 — 둘이 같은 맵을 봐야 해서 가를 수 없다.
 */
@Component
class DragonCacheAdapter :
    DragonCacheQueryPort,
    DragonCacheCommandPort {
    private val log = LoggerFactory.getLogger(javaClass)

    private val champions = ConcurrentHashMap<Int, DragonChampionModel>()
    private val items = ConcurrentHashMap<Int, DragonItemModel>()
    private val spells = ConcurrentHashMap<Int, DragonSummonerSpellModel>()
    private val runes = ConcurrentHashMap<Int, DragonRuneModel>()

    override fun replaceAll(
        champions: List<DragonChampionModel>,
        items: List<DragonItemModel>,
        spells: List<DragonSummonerSpellModel>,
        runes: List<DragonRuneModel>,
    ) {
        this.champions.clear()
        this.items.clear()
        this.spells.clear()
        this.runes.clear()

        champions.forEach { this.champions[it.championId] = it }
        items.forEach { this.items[it.itemId] = it }
        spells.forEach { this.spells[it.spellId] = it }
        runes.forEach { this.runes[it.runeId] = it }

        log.info(
            "[DataDragon Cache] 적재 완료 - 챔피언: {}, 아이템: {}, 스펠: {}, 룬: {}",
            this.champions.size,
            this.items.size,
            this.spells.size,
            this.runes.size,
        )
    }

    override fun findAllChampions(): List<DragonChampionModel> = champions.values.toList()

    override fun findAllItems(): List<DragonItemModel> = items.values.toList()

    override fun findAllSpells(): List<DragonSummonerSpellModel> = spells.values.toList()

    override fun findAllRunes(): List<DragonRuneModel> = runes.values.toList()

    override fun findChampionById(championId: Int): DragonChampionModel? = champions[championId]

    override fun findItemById(itemId: Int): DragonItemModel? = items[itemId]

    override fun findSpellById(spellId: Int): DragonSummonerSpellModel? = spells[spellId]

    override fun findRuneById(runeId: Int): DragonRuneModel? = runes[runeId]
}
