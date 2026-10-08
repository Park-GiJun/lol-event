package com.gijun.lolml.autograd

import kotlin.random.Random

/**
 * 뉴런 하나 = 1단계의 로지스틱 회귀에서 sigmoid 를 뗀 것: `w·x + b`.
 * [nonlinear] 면 그 결과를 tanh 로 굽힌다.
 */
class Neuron(
    inputs: Int,
    random: Random,
    private val nonlinear: Boolean,
) {
    val weights: List<Value> = List(inputs) { Value(random.nextDouble(-1.0, 1.0)) }
    val bias = Value(0.0)

    operator fun invoke(x: List<Value>): Value {
        val z = Value.sum(weights.indices.map { weights[it] * x[it] }) + bias
        return if (nonlinear) z.tanh() else z
    }

    fun parameters(): List<Value> = weights + bias
}

/** 같은 입력을 받는 뉴런 여러 개. 뉴런마다 입력의 다른 조합을 본다. */
class Layer(
    inputs: Int,
    outputs: Int,
    random: Random,
    nonlinear: Boolean,
) {
    private val neurons = List(outputs) { Neuron(inputs, random, nonlinear) }

    operator fun invoke(x: List<Value>): List<Value> = neurons.map { it(x) }

    fun parameters(): List<Value> = neurons.flatMap { it.parameters() }
}

/**
 * 층을 차례로 쌓은 것. 앞 층의 출력이 뒤 층의 입력이 된다.
 *
 * 로지스틱 회귀는 피처를 한 번 곱해 더하는 것뿐이라 "A 와 B 가 **같이** 있을 때만" 같은 관계를 못 배운다.
 * 가운데 층(굽은 함수 포함)이 그런 조합을 만든다. 마지막 층은 굽히지 않는다 — 점수를 그대로 내보내고,
 * 확률로 바꿀지는 쓰는 쪽이 정한다.
 *
 * @param sizes 각 층의 뉴런 수. `Mlp(3, listOf(4, 1))` 은 입력 3 → 가운데 4 → 출력 1.
 */
class Mlp(
    inputs: Int,
    sizes: List<Int>,
    random: Random,
) {
    private val layers =
        sizes.mapIndexed { i, size ->
            Layer(if (i == 0) inputs else sizes[i - 1], size, random, nonlinear = i != sizes.lastIndex)
        }

    operator fun invoke(x: List<Value>): List<Value> = layers.fold(x) { input, layer -> layer(input) }

    fun parameters(): List<Value> = layers.flatMap { it.parameters() }
}

/**
 * 경사하강법 한 걸음. 1단계의 `weights[j] -= learningRate * (gradient + l2 * weights[j])` 와 같은 일이다.
 * [l2] 가 있으면 값을 0 쪽으로 조금씩 당긴다.
 */
fun List<Value>.step(
    learningRate: Double,
    l2: Double = 0.0,
) = forEach { it.data -= learningRate * (it.grad + l2 * it.data) }

fun List<Value>.zeroGrad() = forEach { it.grad = 0.0 }
