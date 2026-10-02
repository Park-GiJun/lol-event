package com.gijun.main.application.port.out.cache

import com.gijun.main.domain.dragon.model.DragonChampionModel
import com.gijun.main.domain.dragon.model.DragonItemModel
import com.gijun.main.domain.dragon.model.DragonRuneModel
import com.gijun.main.domain.dragon.model.DragonSummonerSpellModel

/**
 * Data Dragon 정적 데이터 캐시 — 조회.
 *
 * 챔피언·아이템·스펠·룬은 패치 때만 바뀌고 화면이 매 요청마다 전부 읽는다. DB 를 치지 않고
 * 메모리에서 낸다.
 */
interface DragonCacheQueryPort {
    fun findAllChampions(): List<DragonChampionModel>

    fun findAllItems(): List<DragonItemModel>

    fun findAllSpells(): List<DragonSummonerSpellModel>

    fun findAllRunes(): List<DragonRuneModel>

    fun findChampionById(championId: Int): DragonChampionModel?

    fun findItemById(itemId: Int): DragonItemModel?

    fun findSpellById(spellId: Int): DragonSummonerSpellModel?

    fun findRuneById(runeId: Int): DragonRuneModel?
}

/** Data Dragon 정적 데이터 캐시 — 적재. */
interface DragonCacheCommandPort {
    /** 네 종을 통째로 갈아 끼운다. 동기화 직후와 기동 시에 부른다. */
    fun replaceAll(
        champions: List<DragonChampionModel>,
        items: List<DragonItemModel>,
        spells: List<DragonSummonerSpellModel>,
        runes: List<DragonRuneModel>,
    )
}
