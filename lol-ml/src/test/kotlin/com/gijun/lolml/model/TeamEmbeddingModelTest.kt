package com.gijun.lolml.model

import com.gijun.lolml.eval.logLoss
import com.gijun.lolml.feature.Example
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeamEmbeddingModelTest {
    private val players = (1..12).map { "p$it" }

    /** 실력을 숨겨 둔 열두 명. 모델은 이름과 승패만 본다. */
    private fun examples(
        count: Int,
        random: Random,
    ): List<Example> {
        val skill = players.withIndex().associate { (i, id) -> id to (i - 5.5) / 3.0 }
        return List(count) { i ->
            val ten = players.shuffled(random).take(10)
            val blue = ten.take(5)
            val red = ten.drop(5)
            val gap = blue.sumOf { skill.getValue(it) } - red.sumOf { skill.getValue(it) }
            Example("m$i", DoubleArray(0), if (random.nextDouble() < sigmoid(gap)) 1.0 else 0.0, blue, red)
        }
    }

    private fun score(
        model: TeamEmbeddingModel,
        examples: List<Example>,
    ) = logLoss(DoubleArray(examples.size) { model.predict(examples[it]) }, DoubleArray(examples.size) { examples[it].label })

    @Test
    fun `이름과 승패만 보고도 처음 보는 경기를 찍는 것보다 잘 맞춘다`() {
        val random = Random(11)
        val train = examples(300, random)
        val unseen = examples(200, random)

        val model = TeamEmbeddingModel(seed = 1).also { it.fit(train) }

        assertTrue(score(model, unseen) < 0.66, "안 본 경기: ${score(model, unseen)}")
        // 본 경기는 언제나 더 잘 맞춘다.
        assertTrue(score(model, train) < score(model, unseen))
    }

    @Test
    fun `두 팀을 맞바꾸면 확률이 뒤집히고 모르는 사람끼리는 반반이다`() {
        val model = TeamEmbeddingModel(epochs = 30, seed = 1).also { it.fit(examples(50, Random(2))) }
        val blue = players.take(5)
        val red = players.drop(5).take(5)

        val p = model.predict(Example("a", DoubleArray(0), 0.0, blue, red))
        val swapped = model.predict(Example("b", DoubleArray(0), 0.0, red, blue))
        val strangers = Example("c", DoubleArray(0), 0.0, List(5) { "x$it" }, List(5) { "y$it" })

        assertEquals(1.0, p + swapped, 1e-12)
        assertEquals(0.5, model.predict(strangers), 1e-12)
        assertNotNull(model.embedding("p1"))
        assertNull(model.embedding("x0"))
        // 사람 12 × 4 + 가운데 층 (4 × 4 + 4) + 출력 (4 + 1)
        assertEquals(12 * 4 + 20 + 5, model.parameterCount)
    }
}
