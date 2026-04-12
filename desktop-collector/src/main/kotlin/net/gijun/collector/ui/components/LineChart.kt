package net.gijun.collector.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.gijun.collector.ui.theme.LolColors
import kotlin.math.abs
import kotlin.math.max

/**
 * Time-series line chart for signed values (e.g. team gold difference).
 * Positive area is filled with [positiveColor], negative area with [negativeColor].
 * The y=0 baseline is always shown.
 */
@Composable
fun SignedLineChart(
    points: List<Pair<Float, Float>>, // (x = time, y = value)
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    positiveColor: Color = LolColors.Info,
    negativeColor: Color = LolColors.Error,
    axisColor: Color = LolColors.Border,
    fillAlpha: Float = 0.18f,
) {
    Box(modifier = modifier.height(height)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.size < 2) return@Canvas

            val w = size.width
            val h = size.height
            val padL = 4f
            val padR = 4f
            val padT = 6f
            val padB = 6f
            val plotW = w - padL - padR
            val plotH = h - padT - padB

            val xMin = points.first().first
            val xMax = points.last().first
            val xRange = max(1f, xMax - xMin)

            val absMax = max(1f, points.maxOf { abs(it.second) })
            val yRange = absMax * 2f
            val centerY = padT + plotH / 2f

            fun mapX(t: Float) = padL + ((t - xMin) / xRange) * plotW
            fun mapY(v: Float) = centerY - (v / absMax) * (plotH / 2f)

            // 0선 (점선)
            drawLine(
                color = axisColor,
                start = Offset(padL, centerY),
                end = Offset(padL + plotW, centerY),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
            )

            // 라인을 양수 구간과 음수 구간으로 나눠 면 채우기 + 라인
            val mapped = points.map { Offset(mapX(it.first), mapY(it.second)) }

            // 면 (양수)
            val posPath = Path().apply {
                var inPositive = false
                points.forEachIndexed { idx, (t, v) ->
                    val x = mapX(t)
                    val y = mapY(v)
                    if (v >= 0f) {
                        if (!inPositive) {
                            moveTo(x, centerY)
                            lineTo(x, y)
                            inPositive = true
                        } else {
                            lineTo(x, y)
                        }
                        if (idx == points.lastIndex) {
                            lineTo(x, centerY)
                            close()
                        }
                    } else if (inPositive) {
                        // 이전 양수 구간 닫기 (centerY로 떨어뜨림)
                        lineTo(x, centerY)
                        close()
                        inPositive = false
                    }
                }
            }
            drawPath(posPath, color = positiveColor.copy(alpha = fillAlpha))

            // 면 (음수)
            val negPath = Path().apply {
                var inNegative = false
                points.forEachIndexed { idx, (t, v) ->
                    val x = mapX(t)
                    val y = mapY(v)
                    if (v < 0f) {
                        if (!inNegative) {
                            moveTo(x, centerY)
                            lineTo(x, y)
                            inNegative = true
                        } else {
                            lineTo(x, y)
                        }
                        if (idx == points.lastIndex) {
                            lineTo(x, centerY)
                            close()
                        }
                    } else if (inNegative) {
                        lineTo(x, centerY)
                        close()
                        inNegative = false
                    }
                }
            }
            drawPath(negPath, color = negativeColor.copy(alpha = fillAlpha))

            // 라인 stroke (구간별 색)
            for (i in 0 until mapped.lastIndex) {
                val a = mapped[i]
                val b = mapped[i + 1]
                val v = (points[i].second + points[i + 1].second) / 2f
                drawLine(
                    color = if (v >= 0f) positiveColor else negativeColor,
                    start = a,
                    end = b,
                    strokeWidth = 2f,
                )
            }

            // 마지막 점 강조
            val last = mapped.last()
            val lastV = points.last().second
            drawCircle(
                color = if (lastV >= 0f) positiveColor else negativeColor,
                radius = 3f,
                center = last,
            )
            drawCircle(
                color = LolColors.BgPrimary,
                radius = 3f,
                center = last,
                style = Stroke(width = 1f),
            )
        }
    }
}
