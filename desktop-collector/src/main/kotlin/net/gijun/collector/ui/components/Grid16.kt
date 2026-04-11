package net.gijun.collector.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.*

private data class GridChildData(val span: Int = 16)

private class GridSpanModifier(val span: Int) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = GridChildData(span)
    override fun equals(other: Any?) = other is GridSpanModifier && other.span == span
    override fun hashCode() = span.hashCode()
}

fun Modifier.colSpan(span: Int): Modifier = this.then(GridSpanModifier(span.coerceIn(1, 16)))

@Composable
fun Grid16(
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val totalWidth = constraints.maxWidth

        // Group children into rows based on spans
        data class RowItem(val measurable: Measurable, val span: Int)

        val rows = mutableListOf<List<RowItem>>()
        var currentRow = mutableListOf<RowItem>()
        var currentSpan = 0

        for (measurable in measurables) {
            val data = measurable.parentData as? GridChildData ?: GridChildData()
            val span = data.span

            if (currentSpan + span > 16 && currentRow.isNotEmpty()) {
                rows.add(currentRow.toList())
                currentRow = mutableListOf()
                currentSpan = 0
            }
            currentRow.add(RowItem(measurable, span))
            currentSpan += span
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow.toList())

        // Measure and place
        val placeables = mutableListOf<Triple<Placeable, Int, Int>>()
        var yOffset = 0

        for ((rowIndex, row) in rows.withIndex()) {
            val totalGaps = (row.size - 1).coerceAtLeast(0) * gapPx
            val availableWidth = totalWidth - totalGaps
            val colWidth = availableWidth / 16.0

            var xOffset = 0
            var maxRowHeight = 0

            for (item in row) {
                val itemWidth = (colWidth * item.span).toInt()
                val placeable = item.measurable.measure(
                    constraints.copy(minWidth = itemWidth, maxWidth = itemWidth, minHeight = 0)
                )
                placeables.add(Triple(placeable, xOffset, yOffset))
                maxRowHeight = maxOf(maxRowHeight, placeable.height)
                xOffset += itemWidth + gapPx
            }
            yOffset += maxRowHeight
            if (rowIndex < rows.size - 1) yOffset += gapPx
        }

        val totalHeight = yOffset.coerceAtLeast(0)

        layout(totalWidth, totalHeight) {
            for ((placeable, x, y) in placeables) {
                placeable.placeRelative(x, y)
            }
        }
    }
}
