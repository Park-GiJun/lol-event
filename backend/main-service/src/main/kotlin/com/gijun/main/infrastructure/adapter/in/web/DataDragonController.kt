package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.dragon.result.DragonChampionResult
import com.gijun.main.application.dto.dragon.result.DragonItemResult
import com.gijun.main.application.dto.dragon.result.DragonSummonerSpellResult
import com.gijun.main.application.dto.dragon.result.DragonSyncResult
import com.gijun.main.application.port.`in`.SyncDataDragonUseCase
import com.gijun.main.infrastructure.cache.DataDragonCacheStore
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "DataDragon", description = "DataDragon 정적 데이터 API")
@RestController
@RequestMapping("/api/ddragon")
class DataDragonController(
    private val syncDataDragonUseCase: SyncDataDragonUseCase,
    private val cacheStore: DataDragonCacheStore
) {

    @Operation(summary = "DataDragon 동기화", description = "최신 버전의 챔피언/아이템/스펠 데이터를 DataDragon에서 받아 DB에 저장하고 캐시를 갱신합니다")
    @PostMapping("/sync")
    fun sync(): CommonApiResponse<DragonSyncResult> =
        CommonApiResponse(success = true, data = syncDataDragonUseCase.sync())

    @Operation(summary = "챔피언 목록 조회")
    @GetMapping("/champions")
    fun champions(): CommonApiResponse<List<DragonChampionResult>> =
        CommonApiResponse(success = true, data = cacheStore.getAllChampions())

    @Operation(summary = "챔피언 단건 조회")
    @GetMapping("/champions/{championId}")
    fun champion(@PathVariable championId: Int): CommonApiResponse<DragonChampionResult?> =
        CommonApiResponse(success = true, data = cacheStore.getChampion(championId))

    @Operation(summary = "아이템 목록 조회")
    @GetMapping("/items")
    fun items(): CommonApiResponse<List<DragonItemResult>> =
        CommonApiResponse(success = true, data = cacheStore.getAllItems())

    @Operation(summary = "아이템 단건 조회")
    @GetMapping("/items/{itemId}")
    fun item(@PathVariable itemId: Int): CommonApiResponse<DragonItemResult?> =
        CommonApiResponse(success = true, data = cacheStore.getItem(itemId))

    @Operation(summary = "소환사 스펠 목록 조회")
    @GetMapping("/spells")
    fun spells(): CommonApiResponse<List<DragonSummonerSpellResult>> =
        CommonApiResponse(success = true, data = cacheStore.getAllSpells())

    @Operation(summary = "소환사 스펠 단건 조회")
    @GetMapping("/spells/{spellId}")
    fun spell(@PathVariable spellId: Int): CommonApiResponse<DragonSummonerSpellResult?> =
        CommonApiResponse(success = true, data = cacheStore.getSpell(spellId))
}
