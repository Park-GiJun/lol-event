package com.gijun.main.infrastructure.adapter.`in`.web.page

import com.gijun.main.application.dto.query.GetChampionPageQuery
import com.gijun.main.application.dto.query.GetSummonerProfileQuery
import com.gijun.main.application.port.`in`.GetChampionPageUseCase
import com.gijun.main.application.port.`in`.GetHomeUseCase
import com.gijun.main.application.port.`in`.GetSummonerProfileUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.infrastructure.adapter.`in`.web.page.dto.ChampionPageResponse
import com.gijun.main.infrastructure.adapter.`in`.web.page.dto.HomeResponse
import com.gijun.main.infrastructure.adapter.`in`.web.page.dto.SummonerProfileResponse
import com.gijun.main.shared.infrastructure.web.common.CommonApiResponse
import com.gijun.main.shared.infrastructure.web.common.riotIdOf
import com.gijun.main.shared.infrastructure.web.common.urlDecoded
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 화면 한 장 단위로 묶어 내려보내는 집계 엔드포인트.
 * ```
 * | 메서드 | 경로 | 설명 |
 * |---|---|---|
 * | GET | `/api/home` | 홈 화면 |
 * | GET | `/api/summoner/{riotId}` | 소환사 화면 |
 * | GET | `/api/champions/{champion}` | 챔피언 화면 |
 * ```
 * 기존 stats 엔드포인트들은 지표 단위라 화면 하나를 그리려면 5~6번 왕복해야 했고,
 * 듀오·라이벌·티어처럼 전체 목록만 주는 엔드포인트는 한 명(또는 한 챔피언) 것을
 * 보려고 수십 KB를 받아 화면에서 걸러 썼다. 그 필터링을 서버로 옮긴다.
 * 지표 단위 엔드포인트는 그대로 둔다 — 분석 탭들이 계속 쓴다.
 */
@RestController
@RequestMapping("/api", version = "1.0")
@Tag(name = "Page Aggregate", description = "화면 단위 집계 API")
class PageWebAdapter(
    private val getHomeUseCase: GetHomeUseCase,
    private val getSummonerProfileUseCase: GetSummonerProfileUseCase,
    private val getChampionPageUseCase: GetChampionPageUseCase,
) {
    @GetMapping("/home")
    @Operation(
        summary = "홈 화면 집계",
        description = "전체 요약, Elo 상위, 챔피언 티어 상위, 시상, 최근 경기, 기록 기간을 한 번에 반환합니다",
    )
    fun getHome(
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "all")
        @RequestParam(defaultValue = "all") mode: GameMode,
    ): CommonApiResponse<HomeResponse> = CommonApiResponse.success(HomeResponse.from(getHomeUseCase.getHome(mode)))

    @GetMapping("/summoner/{riotId}")
    @Operation(
        summary = "소환사 화면 집계",
        description = "프로필, 연승/연패, 챔피언별·포지션별 통계, 최근 경기, 함께 뛴 사람, 맞붙은 사람을 한 번에 반환합니다",
    )
    fun getSummonerProfile(
        @Parameter(description = "소환사 이름#태그", example = "Dokyon#두 웅")
        @PathVariable riotId: String,
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "all")
        @RequestParam(defaultValue = "all") mode: GameMode,
    ): CommonApiResponse<SummonerProfileResponse> =
        CommonApiResponse.success(
            SummonerProfileResponse.from(
                getSummonerProfileUseCase.getSummonerProfile(GetSummonerProfileQuery(riotIdOf(urlDecoded(riotId)), mode)),
            ),
        )

    @GetMapping("/champions/{champion}")
    @Operation(
        summary = "챔피언 화면 집계",
        description = "챔피언 상세, 티어 항목, 라인전 지표, 상대 전적을 한 번에 반환합니다",
    )
    fun getChampionPage(
        @Parameter(description = "챔피언 영문명", example = "Ahri")
        @PathVariable champion: String,
        @Parameter(description = "경기 모드 (normal=5v5내전, aram=칼바람, all=전체)", example = "all")
        @RequestParam(defaultValue = "all") mode: GameMode,
    ): CommonApiResponse<ChampionPageResponse> =
        CommonApiResponse.success(
            ChampionPageResponse.from(getChampionPageUseCase.getChampionPage(GetChampionPageQuery(urlDecoded(champion), mode))),
        )
}
