package com.gijun.main.application.handler

import com.gijun.main.application.dto.query.GetLaneLeaderboardQuery
import com.gijun.main.application.dto.query.GetSummonerProfileQuery
import com.gijun.main.application.port.`in`.GetLaneLeaderboardUseCase
import com.gijun.main.application.port.`in`.GetSummonerProfileUseCase
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.domain.vo.RiotId
import org.springframework.stereotype.Component

/**
 * 플레이어 프로필 글을 쓴다 — 통계를 읽고, 강점·약점을 가려내고, 문장으로 옮기는 것까지.
 *
 * 색인(저장할 문서)과 에이전트 tool(그 자리에서 답할 글)이 같은 글을 쓰도록 한 곳에 둔다.
 * 둘이 따로 쓰면 챗봇이 문서에서 읽은 강점과 tool 로 읽은 강점이 달라진다.
 */
@Component
class PlayerProfileComposer(
    private val getSummonerProfileUseCase: GetSummonerProfileUseCase,
    private val getLaneLeaderboardUseCase: GetLaneLeaderboardUseCase,
) {
    internal fun compose(
        riotId: String,
        names: ChampionNames,
    ): String {
        val profile = getSummonerProfileUseCase.getSummonerProfile(GetSummonerProfileQuery(RiotId(riotId), SCOPE))
        // 비교는 그 사람이 가장 많이 간 자리에서 한다. 포지션별 통계는 캐시돼 있어 사람마다 불러도 싸다.
        val mainPosition =
            profile.positionStats
                .filter { it.games > 0 }
                .maxByOrNull { it.games }
                ?.position
        val laneBoard = mainPosition?.let { getLaneLeaderboardUseCase.getLaneLeaderboard(GetLaneLeaderboardQuery(it, SCOPE)) }
        return RagDocumentWriter.playerProfile(profile, names, PlayerInsightAnalyzer.analyze(profile, laneBoard, names))
    }

    private companion object {
        val SCOPE = GameMode.ALL
    }
}
