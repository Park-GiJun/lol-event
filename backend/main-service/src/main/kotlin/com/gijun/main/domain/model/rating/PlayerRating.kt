package com.gijun.main.domain.model.rating

import com.gijun.main.domain.service.RatingMath
import java.time.LocalDateTime

/**
 * 한 사람의 두 레이팅. 둘은 끝까지 섞이지 않는다 — laneElo 계산에 teamElo 가 들어가면 안 되고,
 * 그 반대도 마찬가지다. 이유는 [RatingMath] 참고.
 *
 * `riotId` 는 [com.gijun.main.domain.service.RiotIdNormalizer] 를 통과한 **정규 이름**이다.
 * 같은 사람이 부계정으로 잡히면 여기서 이미 한 줄로 합쳐져 있다.
 *
 * `laneWins` / `teamWins` 는 명세의 엔티티 정의에는 없지만 `/api/stats/elo` 가
 * `laneWinRate` 와 `winRate` 를 내려주려면 있어야 한다. 매 조회마다 경기를 다시 훑는 대신
 * 갱신할 때 같이 센다.
 */
data class PlayerRating(
    val id: Long = 0,
    val riotId: String,
    /** 실력 레이팅. 같은 포지션 맞대결 결과로만 움직인다. */
    val laneElo: Double = RatingMath.START,
    val laneDuels: Int = 0,
    val laneWins: Int = 0,
    /** 전적 레이팅. 팀 승패로만 움직인다. */
    val teamElo: Double = RatingMath.START,
    val teamGames: Int = 0,
    val teamWins: Int = 0,
    /**
     * 연승/연패. **표시 전용이다** — 어떤 산식에도 들어가지 않는다.
     * 옛 구현은 연승을 K 배율로 썼는데, Elo 는 "현재 레이팅 = 현재 실력"을 전제하므로
     * 폼은 배율이 아니라 K 로 흡수해야 한다. 특히 연패 배율은 진 사람을 더 빨리 밀어내리는
     * 가속기였다. 숫자는 화면에 쓰려고 남겨 둔 것뿐이다.
     */
    val teamWinStreak: Int = 0,
    val teamLossStreak: Int = 0,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    /** 배치 중. 순위에서 뺀다. */
    val placement: Boolean get() = laneDuels < RatingMath.PLACEMENT

    val laneEloDisplay: Double get() = RatingMath.display(laneElo, laneDuels)
    val teamEloDisplay: Double get() = RatingMath.display(teamElo, teamGames)

    /**
     * 편성자의 평가와 라인 실적이 얼마나 어긋나 있는지. 절댓값이 크면 참고 신호다.
     * 양수면 "이기는 팀에 자주 들어갔는데 라인전 실적은 그만큼이 아니다".
     */
    val gap: Double get() = teamElo - laneElo
}
