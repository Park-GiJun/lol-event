package com.gijun.lolml

import com.gijun.lolml.data.Match
import com.gijun.lolml.eval.logLoss
import com.gijun.lolml.eval.printReport
import com.gijun.lolml.feature.FeatureBuilder
import com.gijun.lolml.model.LogisticRegression
import com.gijun.lolml.model.Standardizer
import com.gijun.lolml.model.sigmoid
import com.gijun.lolml.model.walkForward
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 실력을 숨겨 둔 가짜 선수들로 경기를 만들어, 파이프라인 전체가 그 실력을 찾아내는지 본다.
 * 진짜 데이터에서는 정답을 모르기 때문에, "배울 것이 분명히 있는" 데이터로 먼저 확인한다.
 */
class PipelineTest {
    @Test
    fun `숨겨 둔 실력 차이를 배워서 찍는 것보다 잘 맞춘다`() {
        val examples = FeatureBuilder().build(fakeMatches(count = 600)).drop(100)
        val minTrain = 400

        val predicted =
            walkForward(examples, minTrain) { train ->
                val standardizer = Standardizer.fit(train.map { it.features })
                val model = LogisticRegression(learningRate = 0.1, l2 = 0.01, epochs = 500)
                model.fit(train.map { standardizer.transform(it.features) }, DoubleArray(train.size) { train[it].label })
                ({ example -> model.predict(standardizer.transform(example.features)) })
            }
        val actual = DoubleArray(predicted.size) { examples[minTrain + it].label }

        printReport(examples, minTrain)
        assertTrue(logLoss(predicted, actual) < 0.67, "기준선 0.693 을 확실히 이겨야 한다")
    }

    private fun fakeMatches(count: Int): List<Match> {
        val random = Random(1)
        // 스무 명. 실력은 모델이 볼 수 없는 값이다.
        val skill = (1..20).associate { "p$it" to random.nextDouble(-1.5, 1.5) }
        return List(count) { i ->
            val ten = skill.keys.shuffled(random).take(10)
            val blue = ten.take(5)
            val red = ten.drop(5)
            val gap = blue.sumOf { skill.getValue(it) } - red.sumOf { skill.getValue(it) }
            match("m$i", blue, red, blueWin = random.nextDouble() < sigmoid(gap * 0.6), gameCreation = i.toLong())
        }
    }
}
