package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.port.`in`.GetStatsUseCase
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stats")
class StatsController(private val getStatsUseCase: GetStatsUseCase) {
    @GetMapping
    fun getStats(@RequestParam(defaultValue = "normal") mode: String) =
        CommonApiResponse.success(getStatsUseCase.getStats(mode))
}
