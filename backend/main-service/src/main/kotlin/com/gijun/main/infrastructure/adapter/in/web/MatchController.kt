package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.response.CommonApiResponse
import com.gijun.main.application.dto.*
import com.gijun.main.application.port.`in`.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/matches")
class MatchController(
    private val saveMatchesUseCase: SaveMatchesUseCase,
    private val getMatchesUseCase: GetMatchesUseCase,
    private val deleteMatchUseCase: DeleteMatchUseCase
) {
    @GetMapping
    fun getAll(@RequestParam(defaultValue = "normal") mode: String) =
        CommonApiResponse.success(getMatchesUseCase.getAll(mode))

    @PostMapping("/bulk")
    fun saveBulk(@RequestBody request: SaveMatchesRequest) =
        CommonApiResponse.success(saveMatchesUseCase.save(request))

    @DeleteMapping("/{matchId}")
    fun delete(@PathVariable matchId: String): CommonApiResponse<Unit> {
        deleteMatchUseCase.delete(matchId)
        return CommonApiResponse.success(Unit)
    }
}
