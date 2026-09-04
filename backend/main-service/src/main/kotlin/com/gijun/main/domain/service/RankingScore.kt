package com.gijun.main.domain.service

/**
 * 표본이 작을 때의 순위 왜곡을 보정한다.
 *
 * 내전 데이터는 전체 154경기 규모라 어떤 축으로 잘라도 표본이 금방 한 자리 수로 떨어진다.
 * 실제로 보정 없이 승률로 정렬하면 이런 것들이 1위에 온다.
 *   - 챔피언 티어 1위: 1경기 1승, 승률 100%
 *   - 듀오 1위: 5경기 5승, 승률 100%
 *
 * 아무도 "다이애나가 이 판의 최강 챔피언"이라고 생각하지 않는데 표는 그렇게 말한다.
 * 그래서 관측 승률을 전체 평균(50%) 쪽으로 끌어당긴 값을 정렬 기준으로 쓴다.
 * 표본이 커질수록 보정치는 관측값에 수렴하므로, 많이 한 쪽이 정당하게 위로 올라간다.
 */
object RankingScore {

    /**
     * 정렬에 쓸 보정 승률(0~100).
     *
     * @param wins 승수
     * @param games 경기 수
     * @param prior 사전 평균 승률. 5v5 내전은 한 판에 승자와 패자가 같은 수만큼 나오므로 0.5.
     * @param weight 사전 분포의 무게. "이 정도 경기 수는 돼야 관측값을 믿는다"는 값이다.
     *               10이면 10경기에서 관측값과 평균을 반반 섞는다.
     */
    fun shrunkWinRate(wins: Int, games: Int, prior: Double = 0.5, weight: Double = 10.0): Double {
        if (games <= 0) return prior * 100
        return (wins + prior * weight) / (games + weight) * 100
    }

    /**
     * 관측값을 얼마나 믿을지. 0(전혀) ~ 1(그대로). 경기 수가 늘수록 1에 수렴한다.
     */
    fun sampleConfidence(games: Int, weight: Double = 10.0): Double =
        games / (games + weight)

    /**
     * 관측값을 전체 평균 쪽으로 신뢰도만큼 끌어당긴다.
     *
     * 승률만 보정해서는 부족하다. 티어 점수처럼 KDA·데미지를 섞어 만드는 합성 지표는
     * 그 구성 요소들도 1경기에서 똑같이 요동치기 때문이다. 실제로 승률만 보정했을 때
     * 승률 0%인 탐 켄치가 KDA 하나로 4위까지 올라왔다. 구성 요소마다 이걸 통과시켜야 한다.
     *
     * @param observed 관측값
     * @param mean 이 지표의 전체 평균 (승률이면 0.5, 정규화된 KDA·데미지면 1.0)
     */
    fun shrinkToward(observed: Double, mean: Double, games: Int, weight: Double = 10.0): Double =
        mean + (observed - mean) * sampleConfidence(games, weight)

    /**
     * 표본 신뢰 등급. UI 배지용.
     * 경기 수 자체를 항상 같이 노출하는 게 원칙이고, 이건 정렬/필터 보조용이다.
     */
    fun sampleGrade(games: Int): String = when {
        games >= 30 -> "HIGH"
        games >= 10 -> "MEDIUM"
        games >= 5  -> "LOW"
        else        -> "INSUFFICIENT"
    }
}
