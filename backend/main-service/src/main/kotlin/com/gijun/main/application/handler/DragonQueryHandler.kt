package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.DragonChampionResult
import com.gijun.main.application.dto.result.DragonItemResult
import com.gijun.main.application.dto.result.DragonRuneResult
import com.gijun.main.application.dto.result.DragonSummonerSpellResult
import com.gijun.main.application.port.`in`.GetDragonChampionUseCase
import com.gijun.main.application.port.`in`.GetDragonChampionsUseCase
import com.gijun.main.application.port.`in`.GetDragonItemUseCase
import com.gijun.main.application.port.`in`.GetDragonItemsUseCase
import com.gijun.main.application.port.`in`.GetDragonRuneUseCase
import com.gijun.main.application.port.`in`.GetDragonRunesUseCase
import com.gijun.main.application.port.`in`.GetDragonSpellUseCase
import com.gijun.main.application.port.`in`.GetDragonSpellsUseCase
import com.gijun.main.application.port.out.cache.DragonCacheQueryPort
import com.gijun.main.domain.dragon.model.DragonChampionModel
import com.gijun.main.domain.dragon.model.DragonItemModel
import com.gijun.main.domain.dragon.model.DragonRuneModel
import com.gijun.main.domain.dragon.model.DragonSummonerSpellModel
import org.springframework.stereotype.Service

/**
 * Data Dragon 정적 데이터 조회. 전부 캐시에서 낸다 — DB 를 치지 않으므로 트랜잭션도 없다.
 */
@Service
class DragonQueryHandler(
    private val dragonCacheQueryPort: DragonCacheQueryPort,
) : GetDragonChampionsUseCase,
    GetDragonChampionUseCase,
    GetDragonItemsUseCase,
    GetDragonItemUseCase,
    GetDragonSpellsUseCase,
    GetDragonSpellUseCase,
    GetDragonRunesUseCase,
    GetDragonRuneUseCase {
    override fun getDragonChampions(): List<DragonChampionResult> =
        dragonCacheQueryPort
            .findAllChampions()
            .sortedBy(DragonChampionModel::nameKo)
            .map(DragonChampionResult::from)

    override fun getDragonChampion(championId: Int): DragonChampionResult? =
        dragonCacheQueryPort.findChampionById(championId)?.let(DragonChampionResult::from)

    override fun getDragonItems(): List<DragonItemResult> =
        dragonCacheQueryPort
            .findAllItems()
            .sortedBy(DragonItemModel::itemId)
            .map(DragonItemResult::from)

    override fun getDragonItem(itemId: Int): DragonItemResult? = dragonCacheQueryPort.findItemById(itemId)?.let(DragonItemResult::from)

    override fun getDragonSpells(): List<DragonSummonerSpellResult> =
        dragonCacheQueryPort
            .findAllSpells()
            .sortedBy(DragonSummonerSpellModel::spellId)
            .map(DragonSummonerSpellResult::from)

    override fun getDragonSpell(spellId: Int): DragonSummonerSpellResult? =
        dragonCacheQueryPort.findSpellById(spellId)?.let(DragonSummonerSpellResult::from)

    override fun getDragonRunes(): List<DragonRuneResult> =
        dragonCacheQueryPort
            .findAllRunes()
            .sortedWith(compareBy(DragonRuneModel::styleId, DragonRuneModel::slot, DragonRuneModel::runeId))
            .map(DragonRuneResult::from)

    override fun getDragonRune(runeId: Int): DragonRuneResult? = dragonCacheQueryPort.findRuneById(runeId)?.let(DragonRuneResult::from)
}
