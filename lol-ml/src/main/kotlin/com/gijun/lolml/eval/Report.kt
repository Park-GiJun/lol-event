package com.gijun.lolml.eval

import com.gijun.lolml.feature.Elo
import com.gijun.lolml.feature.Example
import com.gijun.lolml.feature.FeatureBuilder
import com.gijun.lolml.model.LogisticRegression
import com.gijun.lolml.model.Standardizer
import com.gijun.lolml.model.TeamEmbeddingModel
import com.gijun.lolml.model.sigmoid
import com.gijun.lolml.model.walkForward
import kotlin.math.ln
import kotlin.math.sqrt

private const val LEARNING_RATE = 0.1
private const val L2 = 0.01
private const val EPOCHS = 2000

/** 나란히 볼 모델. 이름과, 그 모델이 쓰는 피처. */
private val MODELS =
    listOf(
        "팀 Elo" to listOf("eloDiff"),
        "라인 Elo" to listOf("laneEloDiff"),
        "자리 Elo" to listOf("seatEloDiff"),
        "승률" to listOf("winRateDiff"),
        "자리 승률" to listOf("seatWinRateDiff"),
        "라인 승률" to listOf("laneWinRateDiff"),
        "자리 라인 승률" to listOf("seatLaneWinRateDiff"),
        "전체 피처" to FeatureBuilder.INSIDE_NAMES,
        // 아래는 내전 밖의 정보(랭크 게임 티어)를 쓴다. 조회한 날의 티어라 과거 경기에는 미래 정보다.
        "티어" to listOf("tierDiff"),
        "티어 라인 Elo" to listOf("tierLaneEloDiff"),
        "자리라인승률+티어" to listOf("seatLaneWinRateDiff", "tierDiff"),
    )

/** 학습 없이 Elo 공식에 그대로 넣는 것. "자리 Elo 공식" 이 지금 팀 편성 화면이 보여 주는 기대 승률이다. */
private val FORMULAS =
    listOf(
        "라인 Elo 공식" to "laneEloDiff",
        "자리 Elo 공식" to "seatEloDiff",
        "티어 라인 Elo 공식" to "tierLaneEloDiff",
    )

/** 맞대어 볼 쌍. 앞이 티어를 넣은 것, 뒤가 그 짝이 되는 기존 모델이다. */
private val HEAD_TO_HEAD =
    listOf(
        "티어" to "자리 라인 승률",
        "자리라인승률+티어" to "자리 라인 승률",
        "자리라인승률+티어" to "자리 Elo 공식",
        "티어 라인 Elo" to "라인 Elo",
        "티어 라인 Elo 공식" to "라인 Elo 공식",
    )

/** 임베딩 모델은 학습이 느려서 경기마다 다시 학습하지 않고 이만큼 쌓일 때마다 한다. 그 사이에는 직전 모델로 맞춘다. */
private const val EMBEDDING_REFIT_EVERY = 10

/**
 * 모델의 기본값(가운데 4, 150 epoch)은 이 데이터에서 본 경기 0.62 / 안 본 경기 0.74 로 외웠다.
 * 가운데 층을 줄이고 일찍 멈추면 0.674 / 0.691 이 된다. 다만 예측이 거의 0.5 로 모인 결과다 —
 * 더 세게 누르면(l2 0.1 이상) 정확히 0.5 만 낸다.
 */
private const val EMBEDDING_HIDDEN = 2
private const val EMBEDDING_EPOCHS = 50

/** 채점 대상 하나. [fit] 은 학습 표본을 받아 "경기 → 블루 승 확률" 을 돌려준다. */
private class Contender(
    val name: String,
    private val refitEvery: Int = 1,
    val fit: (List<Example>) -> (Example) -> Double,
) {
    /** walk-forward 용. [refitEvery] 경기가 쌓일 때만 다시 학습한다 — 어느 쪽이든 맞출 경기보다 앞의 것만 본다. */
    fun walkForwardFit(): (List<Example>) -> (Example) -> Double {
        var trainedOn = 0
        var predict: ((Example) -> Double)? = null
        return { train ->
            val current = predict
            if (current != null && train.size - trainedOn < refitEvery) {
                current
            } else {
                trainedOn = train.size
                fit(train).also { predict = it }
            }
        }
    }
}

private val CONTENDERS =
    MODELS.map { (name, features) -> Contender(name) { train -> Fitted.train(train, features, symmetric = true)::predict } } +
        Contender("임베딩", EMBEDDING_REFIT_EVERY) { train ->
            TeamEmbeddingModel(hidden = EMBEDDING_HIDDEN, epochs = EMBEDDING_EPOCHS).also { it.fit(train) }::predict
        }

/**
 * 같은 경기들을 walk-forward 로 맞춰 나란히 본다.
 *
 * 피처 모델은 전부 bias 없이 학습한다 — 두 팀을 맞바꾸면 확률이 1 − p 가 되어야 하기 때문이다.
 * "블루 승률만" 은 그 반대로 bias 만 배운 것이라, 진영을 따로 배울 가치가 있는지를 보여 준다.
 */
fun printReport(
    examples: List<Example>,
    minTrain: Int,
) {
    val evaluated = examples.drop(minTrain)
    val actual = labels(evaluated)
    val coin = DoubleArray(actual.size) { 0.5 }

    println("처음 ${minTrain}경기로 시작해 한 경기씩 늘려 가며 ${evaluated.size}경기를 맞춘다 (블루 승률 %.3f)".format(actual.average()))
    println()
    println("%-12s %8s %8s   %s".format("", "logloss", "brier", "0.5 대비 logloss (95% 구간)"))
    printRow("상수 0.5", coin, actual, coin)
    printRow("블루 승률만", walkForward(examples, minTrain) { Fitted.train(it, emptyList(), symmetric = false)::predict }, actual, coin)
    val formulas =
        FORMULAS.map { (name, feature) ->
            val predicted = eloFormula(evaluated, feature)
            printRow(name, predicted, actual, coin)
            name to predicted
        }
    val predictions =
        CONTENDERS.map { contender ->
            val predicted = walkForward(examples, minTrain, contender.walkForwardFit())
            printRow(contender.name, predicted, actual, coin)
            contender.name to predicted
        }

    // 위의 구간은 전부 "찍는 것보다 나은가" 다. 여기서는 같은 경기에서 두 모델의 벌점을 경기마다 빼서
    // "티어를 넣은 쪽이 넣지 않은 쪽보다 나은가" 를 본다. 음수면 앞의 모델이 낫다.
    val byName = (formulas + predictions).toMap()
    println()
    println("티어를 넣은 것 vs 넣지 않은 것 — logloss 차이 (95% 구간)")
    for ((name, baseline) in HEAD_TO_HEAD) {
        val (mean, low, high) = lossDifference(byName.getValue(name), actual, byName.getValue(baseline))
        println("%s vs %s   %+.4f (%+.4f ~ %+.4f)".format(name, baseline, mean, low, high))
    }

    // 전체 경기로 학습한 모델에게 그 경기들을 다시 물어본다. 이미 답을 본 문제라 점수가 좋게 나온다.
    // walk-forward 점수와의 차이가 곧 "외운 만큼" 이다 — 배울 숫자가 많을수록 벌어진다.
    println()
    println("같은 ${evaluated.size}경기를, 전체 ${examples.size}경기로 학습한 모델로 채점하면 (답을 본 문제)")
    println("%-12s %8s %8s %8s".format("", "본 문제", "안 본 문제", "차이"))
    for (contender in CONTENDERS) {
        val predict = contender.fit(examples)
        val seen = logLoss(DoubleArray(evaluated.size) { predict(evaluated[it]) }, actual)
        val unseen = logLoss(predictions.first { it.first == contender.name }.second, actual)
        println("%-12s %8.4f %8.4f %+8.4f".format(contender.name, seen, unseen, unseen - seen))
    }

    val (bestName, bestPredicted) = predictions.minBy { logLoss(it.second, actual) }
    println()
    println("캘리브레이션 ($bestName — 위에서 logloss 가 가장 낮은 모델)")
    println("%-11s %6s %8s %8s".format("구간", "경기", "예측", "실제"))
    for (bin in calibrationTable(bestPredicted, actual)) {
        println("%.1f ~ %.1f   %6d %8.3f %8.3f".format(bin.from, bin.to, bin.count, bin.meanPredicted, bin.actualWinRate))
    }

    println()
    println("피처를 하나씩만 썼을 때의 가중치 (전체 ${examples.size}경기로 학습) — 이 피처가 1 표준편차만큼 블루 쪽으로 기울면")
    for (name in FeatureBuilder.NAMES) {
        val fitted = Fitted.train(examples, listOf(name), symmetric = true)
        val weight = fitted.model.weights[0]
        println(
            "%-20s w=%+.3f  블루 승률 %+.1f%%p  (1 표준편차 = %.3f)".format(
                name,
                weight,
                (sigmoid(weight) - 0.5) * 100.0,
                fitted.standardizer.std[0],
            ),
        )
    }
}

private fun printRow(
    name: String,
    predicted: DoubleArray,
    actual: DoubleArray,
    baseline: DoubleArray,
) {
    val (mean, low, high) = lossDifference(predicted, actual, baseline)
    println(
        "%-12s %8.4f %8.4f   %+.4f (%+.4f ~ %+.4f)".format(
            name,
            logLoss(predicted, actual),
            brierScore(predicted, actual),
            mean,
            low,
            high,
        ),
    )
}

/**
 * 경기마다 (이 모델의 벌점 − 기준선의 벌점). 평균이 음수면 기준선보다 낫다.
 * 구간이 0 을 걸치면 그 차이는 우연일 수 있다.
 *
 *  (평균, 95% 구간의 아래, 위)
 */
private fun lossDifference(
    predicted: DoubleArray,
    actual: DoubleArray,
    baseline: DoubleArray,
): Triple<Double, Double, Double> {
    val diffs = DoubleArray(actual.size) { gameLoss(predicted[it], actual[it]) - gameLoss(baseline[it], actual[it]) }
    val mean = diffs.average()
    val standardError = sqrt(diffs.sumOf { (it - mean) * (it - mean) } / (diffs.size - 1) / diffs.size)
    return Triple(mean, mean - 1.96 * standardError, mean + 1.96 * standardError)
}

private fun eloFormula(
    examples: List<Example>,
    featureName: String,
): DoubleArray {
    val index = FeatureBuilder.NAMES.indexOf(featureName)
    return DoubleArray(examples.size) { Elo.expected(examples[it].features[index], 0.0) }
}

private fun gameLoss(
    predicted: Double,
    actual: Double,
): Double = -ln(if (actual == 1.0) predicted else 1.0 - predicted)

private fun labels(examples: List<Example>) = DoubleArray(examples.size) { examples[it].label }

/** 표준화 통계와 모델은 한 쌍이다. 학습 표본에서 구한 통계를 맞출 경기에도 그대로 쓴다. */
internal class Fitted(
    val model: LogisticRegression,
    val standardizer: Standardizer,
    private val featureIndices: List<Int>,
) {
    fun predict(example: Example): Double = model.predict(standardizer.transform(pick(example, featureIndices)))

    companion object {
        /** @param symmetric true 면 bias 도 평균 빼기도 없다. 피처가 전부 0 이면 정확히 0.5 를 낸다. */
        fun train(
            train: List<Example>,
            featureNames: List<String>,
            symmetric: Boolean,
        ): Fitted {
            val featureIndices = featureNames.map { FeatureBuilder.NAMES.indexOf(it) }
            val raw = train.map { pick(it, featureIndices) }
            val standardizer = Standardizer.fit(raw, center = !symmetric)
            val model = LogisticRegression(LEARNING_RATE, L2, EPOCHS, fitBias = !symmetric)
            model.fit(raw.map { standardizer.transform(it) }, labels(train))
            return Fitted(model, standardizer, featureIndices)
        }

        private fun pick(
            example: Example,
            featureIndices: List<Int>,
        ) = DoubleArray(featureIndices.size) { example.features[featureIndices[it]] }
    }
}
