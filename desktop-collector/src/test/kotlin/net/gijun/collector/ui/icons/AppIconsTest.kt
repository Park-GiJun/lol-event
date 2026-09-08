package net.gijun.collector.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 아이콘은 materialIconsExtended(36.8MB) 를 걷어내면서 SVG path 문자열로 옮겨 왔다.
 * 문자열이라 오타가 나도 컴파일은 통과하고, 화면에서는 아이콘이 그냥 안 보이거나
 * 뭉개진 모양으로 그려진다. 눈으로 잡기 어려우니 여기서 막는다.
 */
class AppIconsTest {

    /** 리플렉션으로 AppIcons 의 ImageVector 게터를 전부 끌어온다. 새 아이콘도 자동으로 포함된다. */
    private fun allIcons(): List<Pair<String, ImageVector>> =
        AppIcons::class.java.declaredMethods
            .filter {
                it.parameterCount == 0 &&
                    it.returnType == ImageVector::class.java &&
                    it.name.startsWith("get") &&
                    !it.isSynthetic
            }
            .sortedBy { it.name }
            .map {
                // by lazy 게터는 public 이지만 클래스가 열려 있지 않아 그냥 invoke 하면 막힌다.
                it.isAccessible = true
                it.name.removePrefix("get") to (it.invoke(AppIcons) as ImageVector)
            }

    private fun pathNodeCount(vector: ImageVector): Int {
        fun count(group: VectorGroup): Int = group.sumOf { node ->
            when (node) {
                is VectorPath -> node.pathData.size
                is VectorGroup -> count(node)
            }
        }
        return count(vector.root)
    }

    @Test
    fun `화면에서 쓰는 아이콘이 모두 들어 있다`() {
        val names = allIcons().map { it.first }.toSet()
        // 실제 호출부에서 쓰는 이름들. 지우면 컴파일이 깨지므로 목록만 지켜보면 된다.
        val used = setOf(
            "Analytics", "AutoAwesome", "AutoFixHigh", "Block", "Close", "Compare",
            "DoNotDisturb", "EmojiEvents", "ExpandLess", "ExpandMore", "Flag", "Groups",
            "History", "HourglassBottom", "Layers", "Leaderboard", "Minimize", "Notifications",
            "People", "RadioButtonChecked", "Refresh", "Search", "Shield", "ShowChart",
            "Star", "Stars", "Timer", "TrendingUp", "Visibility", "Whatshot",
        )
        assertEquals(used, names, "AppIcons 목록이 화면에서 쓰는 아이콘과 어긋난다")
    }

    @Test
    fun `모든 아이콘의 path 가 실제로 파싱돼 도형이 만들어진다`() {
        for ((name, vector) in allIcons()) {
            val nodes = pathNodeCount(vector)
            // path 문자열이 비었거나 앞부분이 깨지면 노드가 0~1개로 떨어진다.
            assertTrue(nodes > 1, "$name 의 path 가 도형을 못 만들었다 (노드 ${nodes}개)")
        }
    }

    @Test
    fun `모든 아이콘이 24dp 24 뷰포트로 통일돼 있다`() {
        for ((name, vector) in allIcons()) {
            // 뷰포트가 다르면 같은 size() 를 줘도 아이콘마다 크기가 달라 보인다.
            assertEquals(24f, vector.viewportWidth, "$name 뷰포트 너비")
            assertEquals(24f, vector.viewportHeight, "$name 뷰포트 높이")
            assertEquals(24f, vector.defaultWidth.value, "$name 기본 너비")
            assertEquals(24f, vector.defaultHeight.value, "$name 기본 높이")
        }
    }

    @Test
    fun `같은 아이콘을 여러 번 읽어도 같은 인스턴스를 준다`() {
        // 컴포지션마다 path 를 다시 파싱하면 안 된다. by lazy 가 유지되는지 본다.
        assertTrue(AppIcons.Refresh === AppIcons.Refresh)
        assertTrue(AppIcons.Star === AppIcons.Star)
    }
}
