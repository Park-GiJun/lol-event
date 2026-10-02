package com.gijun.main.infrastructure.adapter.`in`.web.riot.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "BulkProfileRequest", description = "프로필 일괄 조회 요청")
data class RiotProfilesRequest(
    @field:Schema(description = "Riot ID 목록. 10 명을 넘는 부분은 무시된다")
    val riotIds: List<String> = emptyList(),
)
