package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.DragonChampionResult
import com.gijun.main.application.dto.result.DragonItemResult
import com.gijun.main.application.dto.result.DragonRuneResult
import com.gijun.main.application.dto.result.DragonSummonerSpellResult

interface GetDragonChampionsUseCase {
    /** 한국어 이름순. */
    fun getDragonChampions(): List<DragonChampionResult>
}

interface GetDragonChampionUseCase {
    fun getDragonChampion(championId: Int): DragonChampionResult?
}

interface GetDragonItemsUseCase {
    fun getDragonItems(): List<DragonItemResult>
}

interface GetDragonItemUseCase {
    fun getDragonItem(itemId: Int): DragonItemResult?
}

interface GetDragonSpellsUseCase {
    fun getDragonSpells(): List<DragonSummonerSpellResult>
}

interface GetDragonSpellUseCase {
    fun getDragonSpell(spellId: Int): DragonSummonerSpellResult?
}

interface GetDragonRunesUseCase {
    /** 계열 → 줄 → 룬 순. 화면에서 룬 페이지 모양대로 그리기 좋게 정렬해 둔다. */
    fun getDragonRunes(): List<DragonRuneResult>
}

interface GetDragonRuneUseCase {
    /** 계열 id 로도 조회된다. */
    fun getDragonRune(runeId: Int): DragonRuneResult?
}
