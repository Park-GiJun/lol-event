package com.gijun.lolml.autograd

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tanh

/**
 * 숫자 하나 + "이 숫자가 어떤 계산으로 만들어졌는지" 의 기록.
 *
 * 1단계에서는 로지스틱 회귀의 기울기 공식 `(예측 − 실제) × 피처` 를 사람이 유도해서 코드에 적었다.
 * 모델이 바뀌면 공식도 다시 유도해야 한다. 여기서는 그 일을 기계에게 시킨다:
 *
 * 1. 계산할 때마다(`a * b`, `x.tanh()` …) 결과 [Value] 가 자기 재료([parents])와,
 *    "내 기울기를 재료에게 어떻게 나눠 줄지"([pushGrad])를 기억한다.
 * 2. 맨 끝 값(손실)에서 [backward] 를 부르면, 계산을 거꾸로 따라가며 모든 재료의 [grad] 를 채운다.
 *
 * [grad] 의 뜻: "이 값을 아주 조금 올리면 맨 끝 값이 그 몇 배만큼 오르는가".
 * 규칙은 하나다(연쇄 법칙) — **재료의 기울기 += 내 기울기 × (재료가 나에게 미친 영향)**.
 */
class Value private constructor(
    /** 학습되는 값(가중치)은 한 걸음마다 바뀌므로 var 다. */
    var data: Double,
    private val parents: List<Value>,
) {
    constructor(data: Double) : this(data, emptyList())

    var grad: Double = 0.0

    /** 내 [grad] 를 재료들의 grad 에 나눠 더한다. 연산마다 내용이 다르다. */
    private var pushGrad: () -> Unit = {}

    operator fun plus(other: Value): Value {
        val out = Value(data + other.data, listOf(this, other))
        // 덧셈은 양쪽을 그대로 통과시킨다: a 가 1 오르면 a + b 도 1 오른다.
        out.pushGrad = {
            grad += out.grad
            other.grad += out.grad
        }
        return out
    }

    operator fun times(other: Value): Value {
        val out = Value(data * other.data, listOf(this, other))
        // 곱셈은 상대의 값만큼 영향을 준다: a 가 1 오르면 a × b 는 b 만큼 오른다.
        out.pushGrad = {
            grad += other.data * out.grad
            other.grad += data * out.grad
        }
        return out
    }

    fun pow(exponent: Double): Value {
        val out = Value(data.pow(exponent), listOf(this))
        out.pushGrad = { grad += exponent * data.pow(exponent - 1) * out.grad }
        return out
    }

    fun exp(): Value {
        val out = Value(exp(data), listOf(this))
        out.pushGrad = { grad += out.data * out.grad }
        return out
    }

    fun ln(): Value {
        val out = Value(ln(data), listOf(this))
        out.pushGrad = { grad += out.grad / data }
        return out
    }

    /** −1 ~ 1 로 누른다. 신경망이 직선이 아닌 모양을 배울 수 있게 하는 굽은 함수다. */
    fun tanh(): Value {
        val out = Value(tanh(data), listOf(this))
        out.pushGrad = { grad += (1.0 - out.data * out.data) * out.grad }
        return out
    }

    /** 0 ~ 1 로 누른다. 1단계의 `sigmoid` 와 같은 함수다. */
    fun sigmoid(): Value {
        val s = if (data >= 0) 1.0 / (1.0 + exp(-data)) else exp(data).let { it / (1.0 + it) }
        val out = Value(s, listOf(this))
        out.pushGrad = { grad += s * (1.0 - s) * out.grad }
        return out
    }

    // 아래는 위의 것들을 조합한 것이라 기울기 규칙을 따로 적지 않아도 된다.

    operator fun unaryMinus(): Value = this * -1.0

    operator fun minus(other: Value): Value = this + -other

    operator fun div(other: Value): Value = this * other.pow(-1.0)

    operator fun plus(other: Double): Value = this + Value(other)

    operator fun minus(other: Double): Value = this + Value(-other)

    operator fun times(other: Double): Value = this * Value(other)

    operator fun div(other: Double): Value = this * Value(1.0 / other)

    /**
     * 이 값을 맨 끝으로 보고, 여기까지 오는 데 쓰인 모든 값의 [grad] 를 채운다.
     *
     * 순서가 중요하다. 어떤 값의 기울기는 **그 값을 쓴 쪽이 전부 나눠 준 뒤에야** 완성되므로,
     * 계산된 순서의 정반대로 돈다. (값 하나가 두 군데에 쓰였으면 기울기도 두 번 더해진다 — 그래서 `+=` 다.)
     *
     * 부르기 전에 학습되는 값들의 grad 는 0 이어야 한다. 직전 걸음의 기울기가 남아 있으면 섞인다.
     */
    fun backward() {
        grad = 1.0
        for (value in computationOrder().asReversed()) value.pushGrad()
    }

    /** 재료가 항상 자기를 쓴 값보다 앞에 오는 순서. 그래프가 깊어도 스택이 넘치지 않게 반복문으로 돈다. */
    private fun computationOrder(): List<Value> {
        val order = ArrayList<Value>()
        val visited = HashSet<Value>()
        // (값, 재료를 다 봤는가)
        val stack = ArrayDeque<Pair<Value, Boolean>>()
        stack.addLast(this to false)
        while (stack.isNotEmpty()) {
            val (value, parentsDone) = stack.removeLast()
            if (parentsDone) {
                order.add(value)
            } else if (visited.add(value)) {
                stack.addLast(value to true)
                value.parents.forEach { stack.addLast(it to false) }
            }
        }
        return order
    }

    override fun toString(): String = "Value(data=$data, grad=$grad)"

    companion object {
        /** 여러 값의 합을 노드 하나로 만든다. `a + b + c + …` 로 이으면 값 수만큼 그래프가 깊어진다. */
        fun sum(values: List<Value>): Value {
            val out = Value(values.sumOf { it.data }, values)
            out.pushGrad = { values.forEach { it.grad += out.grad } }
            return out
        }
    }
}

operator fun Double.plus(other: Value): Value = other + this

operator fun Double.minus(other: Value): Value = -other + this

operator fun Double.times(other: Value): Value = other * this
