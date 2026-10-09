package com.gijun.main.application.handler

import com.gijun.main.application.port.out.external.PlayerTier
import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.prediction.model.WinModel
import com.gijun.main.domain.prediction.service.TierScores
import kotlin.math.abs
import kotlin.math.roundToInt

/** 예측에 들어간 한 사람. 전부 코드가 낸 값이다. */
data class PredictionSeat(
    val riotId: String,
    val position: Position,
    /** 0~1. 표본이 적으면 평소 승률 쪽으로 당긴 값. */
    val seatLaneWinRate: Double,
    val seatDuels: Int,
    /** 확인하지 못했으면 null. 그때는 모델의 평균 점수로 계산한다. */
    val tier: PlayerTier?,
)

/**
 * 승률 예측을 모델(LLM)이 읽을 글로 쓴다. **확률과 판정까지 여기서 끝낸다** — 모델은 옮기기만 한다.
 */
object MatchPredictionWriter {
    private const val PERCENT = 100
    private const val HALF = 0.5

    /** 50% 에서 이만큼 안쪽이면 비슷하다고 본다. 편성 해설과 같은 기준(5%p)이다. */
    private const val CLOSE = 0.05
    private const val CLEAR = 0.15

    /** 블루 평균 − 레드 평균. */
    fun seatLaneWinRateDiff(
        blue: List<PredictionSeat>,
        red: List<PredictionSeat>,
    ): Double = blue.map { it.seatLaneWinRate }.average() - red.map { it.seatLaneWinRate }.average()

    /** 블루 평균 − 레드 평균. 티어를 모르는 사람은 모델의 평균 점수로 친다. */
    fun tierDiff(
        blue: List<PredictionSeat>,
        red: List<PredictionSeat>,
        model: WinModel,
    ): Double = meanTier(blue, model) - meanTier(red, model)

    fun write(
        model: WinModel,
        blue: List<PredictionSeat>,
        red: List<PredictionSeat>,
    ): String {
        val laneDiff = seatLaneWinRateDiff(blue, red)
        val tierDiff = tierDiff(blue, red, model)
        val blueWin = model.blueWinProbability(laneDiff, tierDiff)
        val bluePercent = (blueWin * PERCENT).roundToInt()

        val lines = mutableListOf<String>()
        lines += "[승률 예측] 블루 $bluePercent% · 레드 ${PERCENT - bluePercent}% — ${verdict(blueWin)}"
        lines += "픽 전 기준이다. 챔피언, 조합, 상성, 같은 팀끼리의 호흡은 보지 않았다."
        lines += "계산에 들어간 것은 아래 두 가지뿐이다."
        lines +=
            "- 자리 라인 승률(그 자리에서 맞상대보다 더 크게 성장한 비율. 표본이 적으면 평소 승률 쪽으로 당겼다): " +
            "블루 평균 ${percent(blue.map { it.seatLaneWinRate }.average())}, " +
            "레드 평균 ${percent(red.map { it.seatLaneWinRate }.average())} " +
            "→ ${lead(laneDiff)} ${"%.1f".format(abs(laneDiff) * PERCENT)}%p 높다."
        lines +=
            "- 랭크 게임 티어: 블루 평균 ${TierScores.label(meanTier(blue, model))}, " +
            "레드 평균 ${TierScores.label(meanTier(red, model))} " +
            "→ ${lead(tierDiff)} ${"%.1f".format(abs(tierDiff))}티어 높다."

        lines += "라인별:"
        blue.zip(red).forEach { (b, r) ->
            lines += "- ${RagDocumentWriter.positionLabel(b.position.name)}: 블루 ${seatText(b)} / 레드 ${seatText(r)}"
        }

        val unknownTier = (blue + red).filter { it.tier == null }.map { it.riotId }
        if (unknownTier.isNotEmpty()) lines += "티어를 확인하지 못해 평균으로 계산한 사람: ${unknownTier.joinToString(", ")}."

        lines +=
            "주의: 내전 ${model.trainedMatches}경기로 학습한 모델이다. 표본이 적어서 검증에서 동전 던지기보다 낫다고 확정하지 못했다. " +
            "참고용이라는 말을 꼭 붙인다."
        return lines.joinToString("\n")
    }

    private fun meanTier(
        team: List<PredictionSeat>,
        model: WinModel,
    ): Double = team.map { seat -> seat.tier?.let { TierScores.score(it.tier, it.division, it.lp) } ?: model.tierMean }.average()

    private fun seatText(seat: PredictionSeat): String {
        val tier = seat.tier?.let { "${it.tier} ${it.division} ${it.lp}LP" } ?: "티어 모름"
        val record = if (seat.seatDuels == 0) "이 자리 기록 없음" else "이 자리 맞대결 ${seat.seatDuels}번"
        return "${seat.riotId} (자리 라인 승률 ${percent(seat.seatLaneWinRate)}, $record, $tier)"
    }

    /** 판정을 글로. 모델에게 맡기면 51% 를 "유리하다" 고 부풀린다. */
    private fun verdict(blueWin: Double): String {
        val side = if (blueWin >= HALF) "블루" else "레드"
        val gap = abs(blueWin - HALF)
        return when {
            gap < CLOSE -> "거의 비슷하다"
            gap < CLEAR -> "${side}가 조금 유리하다"
            else -> "${side}가 유리하다"
        }
    }

    private fun lead(diff: Double) = if (diff >= 0) "블루가" else "레드가"

    private fun percent(rate: Double) = "${(rate * PERCENT).roundToInt()}%"
}
