package com.gijun.main.infrastructure.adapter.`in`.web.riot

import com.gijun.main.application.port.`in`.GetRiotProfileUseCase
import com.gijun.main.application.port.`in`.GetRiotProfilesUseCase
import com.gijun.main.infrastructure.adapter.`in`.web.riot.dto.RiotProfileResponse
import com.gijun.main.infrastructure.adapter.`in`.web.riot.dto.RiotProfilesRequest
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import com.gijun.main.shared.infrastructure.web.common.riotIdOf
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Riot 공식 API 연동 — 랭크와 숙련도.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/riot/profile/{riotId}` | 한 명 |
 * | POST | `/api/riot/profiles/bulk` | 여러 명(최대 10) |
 * ```
 */
@RestController
@RequestMapping("/api/riot", version = "1.0")
@Tag(name = "Riot API", description = "라이엇 공식 API 연동 (랭크, 숙련도)")
class RiotProfileWebAdapter(
    private val getRiotProfileUseCase: GetRiotProfileUseCase,
    private val getRiotProfilesUseCase: GetRiotProfilesUseCase,
) {
    @GetMapping("/profile/{riotId}")
    @Operation(
        summary = "플레이어 라이엇 프로필 조회 (랭크 + 숙련도)",
        description = "내전 멤버로 등록되지 않은 사람은 puuid 가 null 인 빈 프로필을 돌려줍니다.",
    )
    fun getRiotProfile(
        @PathVariable riotId: String,
    ): CommonApiResponse<RiotProfileResponse> =
        CommonApiResponse.success(RiotProfileResponse.from(getRiotProfileUseCase.getRiotProfile(riotIdOf(riotId))))

    @PostMapping("/profiles/bulk")
    @Operation(
        summary = "복수 플레이어 라이엇 프로필 일괄 조회",
        description = "앞에서부터 10 명까지만 조회합니다. 조회에 실패한 사람은 결과에서 빠집니다.",
    )
    fun getRiotProfiles(
        @Valid @RequestBody request: RiotProfilesRequest,
    ): CommonApiResponse<Map<String, RiotProfileResponse>> =
        CommonApiResponse.success(
            getRiotProfilesUseCase.getRiotProfiles(request.riotIds).mapValues { RiotProfileResponse.from(it.value) },
        )
}
