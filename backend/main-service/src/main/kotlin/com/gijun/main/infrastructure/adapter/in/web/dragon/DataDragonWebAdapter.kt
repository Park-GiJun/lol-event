package com.gijun.main.infrastructure.adapter.`in`.web.dragon

import com.gijun.main.application.port.`in`.GetDragonChampionUseCase
import com.gijun.main.application.port.`in`.GetDragonChampionsUseCase
import com.gijun.main.application.port.`in`.GetDragonItemUseCase
import com.gijun.main.application.port.`in`.GetDragonItemsUseCase
import com.gijun.main.application.port.`in`.GetDragonRuneUseCase
import com.gijun.main.application.port.`in`.GetDragonRunesUseCase
import com.gijun.main.application.port.`in`.GetDragonSpellUseCase
import com.gijun.main.application.port.`in`.GetDragonSpellsUseCase
import com.gijun.main.application.port.`in`.SyncDataDragonUseCase
import com.gijun.main.infrastructure.adapter.`in`.web.dragon.dto.DragonChampionResponse
import com.gijun.main.infrastructure.adapter.`in`.web.dragon.dto.DragonItemResponse
import com.gijun.main.infrastructure.adapter.`in`.web.dragon.dto.DragonRuneResponse
import com.gijun.main.infrastructure.adapter.`in`.web.dragon.dto.DragonSummonerSpellResponse
import com.gijun.main.infrastructure.adapter.`in`.web.dragon.dto.DragonSyncResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Data Dragon 정적 데이터(챔피언·아이템·스펠·룬).
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/ddragon/champions` · `/{championId}` | 챔피언 |
 * | GET | `/api/ddragon/items` · `/{itemId}` | 아이템 |
 * | GET | `/api/ddragon/spells` · `/{spellId}` | 소환사 스펠 |
 * | GET | `/api/ddragon/runes` · `/{runeId}` | 룬 |
 * | POST | `/api/ddragon/sync` | 최신 버전으로 동기화 |
 * ```
 * **단건 조회는 없는 id 에도 404 를 내지 않고 `data: null` 을 돌려준다.** 화면이 패치 직후의
 * 새 id 를 물어 올 수 있고, 그때마다 오류 모달이 뜨면 안 된다.
 */
@RestController
@RequestMapping("/api/ddragon", version = "1.0")
@Tag(name = "DataDragon", description = "DataDragon 정적 데이터 API")
class DataDragonWebAdapter(
    private val getDragonChampionsUseCase: GetDragonChampionsUseCase,
    private val getDragonChampionUseCase: GetDragonChampionUseCase,
    private val getDragonItemsUseCase: GetDragonItemsUseCase,
    private val getDragonItemUseCase: GetDragonItemUseCase,
    private val getDragonSpellsUseCase: GetDragonSpellsUseCase,
    private val getDragonSpellUseCase: GetDragonSpellUseCase,
    private val getDragonRunesUseCase: GetDragonRunesUseCase,
    private val getDragonRuneUseCase: GetDragonRuneUseCase,
    private val syncDataDragonUseCase: SyncDataDragonUseCase,
) {
    // ===== 조회 =====

    @GetMapping("/champions")
    @Operation(summary = "챔피언 목록 조회")
    fun getDragonChampions(): CommonApiResponse<List<DragonChampionResponse>> =
        CommonApiResponse.success(getDragonChampionsUseCase.getDragonChampions().map(DragonChampionResponse::from))

    @GetMapping("/champions/{championId}")
    @Operation(summary = "챔피언 단건 조회")
    fun getDragonChampion(
        @Parameter(description = "챔피언 ID", example = "157")
        @PathVariable championId: Int,
    ): CommonApiResponse<DragonChampionResponse?> =
        CommonApiResponse.success(getDragonChampionUseCase.getDragonChampion(championId)?.let(DragonChampionResponse::from))

    @GetMapping("/items")
    @Operation(summary = "아이템 목록 조회")
    fun getDragonItems(): CommonApiResponse<List<DragonItemResponse>> =
        CommonApiResponse.success(getDragonItemsUseCase.getDragonItems().map(DragonItemResponse::from))

    @GetMapping("/items/{itemId}")
    @Operation(summary = "아이템 단건 조회")
    fun getDragonItem(
        @Parameter(description = "아이템 ID", example = "3157")
        @PathVariable itemId: Int,
    ): CommonApiResponse<DragonItemResponse?> =
        CommonApiResponse.success(getDragonItemUseCase.getDragonItem(itemId)?.let(DragonItemResponse::from))

    @GetMapping("/spells")
    @Operation(summary = "소환사 스펠 목록 조회")
    fun getDragonSpells(): CommonApiResponse<List<DragonSummonerSpellResponse>> =
        CommonApiResponse.success(getDragonSpellsUseCase.getDragonSpells().map(DragonSummonerSpellResponse::from))

    @GetMapping("/spells/{spellId}")
    @Operation(summary = "소환사 스펠 단건 조회")
    fun getDragonSpell(
        @Parameter(description = "스펠 ID", example = "4")
        @PathVariable spellId: Int,
    ): CommonApiResponse<DragonSummonerSpellResponse?> =
        CommonApiResponse.success(getDragonSpellUseCase.getDragonSpell(spellId)?.let(DragonSummonerSpellResponse::from))

    @GetMapping("/runes")
    @Operation(
        summary = "룬 목록 조회",
        description =
            "룬 계열 5종과 그 안의 룬을 한 목록으로 반환합니다. " +
                "계열 행은 runeId 가 styleId 와 같고 slot 이 -1 입니다. slot 0 이 핵심 룬(키스톤)입니다.",
    )
    fun getDragonRunes(): CommonApiResponse<List<DragonRuneResponse>> =
        CommonApiResponse.success(getDragonRunesUseCase.getDragonRunes().map(DragonRuneResponse::from))

    @GetMapping("/runes/{runeId}")
    @Operation(summary = "룬 단건 조회")
    fun getDragonRune(
        @Parameter(description = "룬 ID (계열 id 도 조회됩니다)", example = "8005")
        @PathVariable runeId: Int,
    ): CommonApiResponse<DragonRuneResponse?> =
        CommonApiResponse.success(getDragonRuneUseCase.getDragonRune(runeId)?.let(DragonRuneResponse::from))

    // ===== 변경 =====

    @PostMapping("/sync")
    @Operation(summary = "DataDragon 동기화", description = "최신 버전의 챔피언/아이템/스펠/룬 데이터를 DataDragon에서 받아 DB에 저장하고 캐시를 갱신합니다")
    fun syncDataDragon(): CommonApiResponse<DragonSyncResponse> =
        CommonApiResponse.success(DragonSyncResponse.from(syncDataDragonUseCase.syncDataDragon()))
}
