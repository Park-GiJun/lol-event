package com.gijun.main.domain.service

import kotlin.math.pow

/**
 * 내전 Elo 레이팅. 순수 계산만 담당한다.
 *
 * 설계 원칙은 두 가지다.
 *
 * 1. **제로섬.** 참가자가 70명으로 고정된 닫힌 풀이다. 한 경기에서 오간 점수의 총합이 0이 아니면
 *    전체 점수가 계속 떠오르거나 가라앉고, 그러면 "1200점"이 언제 찍은 1200점이냐에 따라 뜻이 달라진다.
 *    나중에 합류한 사람은 영원히 상위권에 못 가거나, 반대로 아무것도 안 해도 순위가 오른다.
 *    그래서 배율 같은 장식보다 총합 보존을 우선한다.
 *
 * 2. **개인 성적은 방향이 아니라 몫만 정한다.** 이겼는지 졌는지가 부호를 정하고, 팀이 주고받을
 *    총량까지 정한다. 개인 성적은 그 총량을 팀원 다섯이 어떻게 나눌지에만 관여한다.
 *    그래서 잘해도 지면 반드시 잃고, 못해도 이기면 반드시 얻는다 — 지표를 노려서 이득을 볼 여지가 없다.
 *    (예전 구현은 성적을 `36 * (perf - 0.5)` 같은 가산항으로 더했다. 져도 점수가 오를 수 있었고
 *     총합도 맞지 않았다. 같은 실수를 반복하지 않기 위한 구조다.)
 *
 * 이전 구현에는 이변 배율·연승 배율·항복 감쇠가 있었으나 전부 제거했다.
 *   - 이변 배율: Elo의 `(S - E)` 항이 이미 이변을 보상한다. 위에 1.3배를 또 곱하면 이중 계산이다.
 *   - 연승 배율: Elo는 "현재 레이팅 = 현재 실력"을 전제한다. 폼은 배율이 아니라 K로 흡수해야 한다.
 *     특히 연패 배율은 진 사람을 더 빨리 밀어내리는 가속기였다.
 *   - 항복 감쇠: 항복은 결과의 불확실성이 아니라 오히려 확실한 실력차의 증거다. 깎을 이유가 없다.
 *     단 리메이크(조기 종료)는 실력과 무관하므로 계산에서 통째로 제외한다 — 그건 호출부의 책임이다.
 */
object EloRating {

    /** 신규 플레이어 시작 점수. 프론트의 색상 임계값(900/1000/1100/1200)이 이 값을 기준으로 잡혀 있다. */
    const val INITIAL = 1000.0

    /**
     * 누적 경기 수에 따른 개인 K.
     *
     * 70명 중 상당수가 1~2경기만 뛰었다. 이들의 1000점은 실력 추정치가 아니라 그냥 기본값인데,
     * 고정 K를 쓰면 그 부정확한 값이 팀 평균에 그대로 들어가 상대 팀 계산까지 오염시킨다.
     * 초반에는 크게 움직여 빨리 제자리를 찾게 하고, 표본이 쌓이면 잠근다.
     */
    fun kFactor(games: Int): Double = when {
        games < 5  -> 64.0
        games < 15 -> 32.0
        else       -> 24.0
    }

    /** 내 팀이 이길 기대 확률. 표준 Elo 로지스틱. */
    fun expectedScore(myTeamAvg: Double, opponentTeamAvg: Double): Double =
        1.0 / (1.0 + 10.0.pow((opponentTeamAvg - myTeamAvg) / 400.0))

    /**
     * 팀 안에서 성적으로 벌어질 수 있는 배분 폭. 0.30 이면 가장 잘한 팀원의 몫이 기준의 1.3배,
     * 가장 못한 팀원이 0.7배다. 승패가 여전히 지배적이어야 하므로 보수적으로 잡는다.
     */
    private const val PERF_STRENGTH = 0.30

    /**
     * 라인전 점수가 팀 평균에서 이만큼 벌어지면 배분 폭의 끝에 닿는다.
     * 라인전 점수는 0.5가 호각인 0~1 값이라, 0.15 는 "상대를 확실히 압도했다" 수준이다.
     */
    private const val PERF_SPREAD = 0.15

    /**
     * 한 명의 계산 입력.
     *
     * @param games 이 경기를 치르기 **전까지의** 누적 경기 수
     * @param lanePerformance 같은 포지션 상대와 비교한 라인전 점수(0~1, 0.5가 호각). [LanePerformance] 참고.
     *                        기본값 0.5는 "판단 보류" — 팀 평균과 같아져 배분에 영향을 주지 않는다.
     */
    data class Rated(
        val rating: Double,
        val games: Int,
        val lanePerformance: Double = LanePerformance.NEUTRAL,
    )

    /** 입력 순서를 그대로 유지한 팀별 변동량. */
    data class Deltas(val teamA: List<Double>, val teamB: List<Double>)

    /**
     * 한 경기의 개인별 Elo 변동량. 반환된 모든 값의 합은 항상 정확히 0이다.
     *
     * 개인마다 K가 다르면 각자 `K_i * (S - E)` 를 그대로 받는 순간 총합이 어긋난다.
     * 그래서 순서를 뒤집는다 — 팀이 주고받을 **총량을 먼저 확정한 뒤**, 팀 안에서 몫으로 나눈다.
     *
     * ```
     * T = ½(ΣK_A + ΣK_B) × (S_A − E_A)     양 팀이 각자 계산했을 총량의 평균
     * A팀 i:  Δ = +T × w_i / Σw_A
     * B팀 j:  Δ = −T × w_j / Σw_B
     * ```
     *
     * 총량 T는 K와 승패로만 정해진다. 성적은 [weights] 안에만 들어가고, 몫의 비율은 정의상 합이 1이라
     * 성적이 아무리 갈려도 T는 흔들리지 않는다. 그래서 총합은 언제나 정확히 0이다.
     *
     * 전원 K가 같고 성적 차이가 없는 5대5면 `Δ = K(S − E)` 로 표준 Elo와 완전히 같아진다.
     * 인원이 어긋난 경기(4대5)에서도, 배치 중인 사람이 섞여 있어도 총합은 0으로 남는다.
     *
     * 대가는 있다. 상대 팀에 배치 중인 사람이 있으면 내 변동폭도 조금 달라진다.
     * 정확한 제로섬과 개인별 K를 동시에 원하면 피할 수 없는 값이고, 총합이 새는 것보다는 낫다.
     */
    fun deltas(teamA: List<Rated>, teamB: List<Rated>, aWon: Boolean): Deltas {
        require(teamA.isNotEmpty() && teamB.isNotEmpty()) { "빈 팀으로는 Elo를 계산할 수 없다" }

        val avgA = teamA.map { it.rating }.average()
        val avgB = teamB.map { it.rating }.average()
        val expectedA = expectedScore(avgA, avgB)
        val scoreA = if (aWon) 1.0 else 0.0

        val sumK = teamA.sumOf { kFactor(it.games) } + teamB.sumOf { kFactor(it.games) }
        val total = 0.5 * sumK * (scoreA - expectedA)

        val wA = weights(teamA, won = aWon)
        val wB = weights(teamB, won = !aWon)
        val sumWA = wA.sum()
        val sumWB = wB.sum()

        return Deltas(
            teamA = wA.map { total * it / sumWA },
            teamB = wB.map { -total * it / sumWB },
        )
    }

    /**
     * 팀 총량을 나눌 몫. `K × 성적 배율` 이다.
     *
     * 성적은 **팀 평균 대비**로만 본다. 이긴 팀은 다섯 명 모두 라인전 점수가 높게 나오기 마련인데,
     * 그 공통분은 이미 승리로 보상됐으므로 몫을 가를 근거가 못 된다. 팀 평균을 빼고 나면
     * "같은 승리 안에서 누가 더 기여했나"만 남는다.
     *
     * 부호를 승패에 맞춰 뒤집는 것이 핵심이다. 이긴 팀에서 잘한 사람은 몫이 커져 **더 얻고**,
     * 진 팀에서 잘한 사람은 몫이 작아져 **덜 잃는다**. 뒤집지 않으면 진 팀의 에이스가
     * 가장 많이 깎이는 정반대 결과가 나온다.
     */
    private fun weights(team: List<Rated>, won: Boolean): List<Double> {
        val k = team.map { kFactor(it.games) }
        if (team.size < 2) return k

        val meanPerf = team.map { it.lanePerformance }.average()
        val sign = if (won) 1.0 else -1.0

        return team.mapIndexed { i, r ->
            val edge = ((r.lanePerformance - meanPerf) / PERF_SPREAD).coerceIn(-1.0, 1.0)
            k[i] * (1.0 + PERF_STRENGTH * edge * sign)
        }
    }
}
