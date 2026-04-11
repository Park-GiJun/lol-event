package net.gijun.collector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.gijun.collector.ui.theme.LolColors

enum class Page(val label: String, val icon: ImageVector) {
    DASHBOARD("내전 대시보드", Icons.Default.Layers),
    MATCHES("경기 기록", Icons.Default.History),
    COLLECT("매치 수집", Icons.Default.Layers),
    CUSTOM("내전 분석", Icons.Default.Layers),       // Swords 아이콘 대체
    SUMMONER("소환사 검색", Icons.Default.Search),
    DAMAGE_ANALYSIS("데미지 분석", Icons.Default.Whatshot),
    VISION("시야 분석", Icons.Default.Visibility),
    SURRENDER("서렌더 분석", Icons.Default.Flag),
}

@Composable
fun Sidebar(
    currentPage: Page,
    onPageChange: (Page) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(LolColors.BgSecondary),
    ) {
        Spacer(Modifier.height(8.dp))
        Page.entries.forEach { page ->
            SidebarItem(
                label = page.label,
                icon = page.icon,
                selected = currentPage == page,
                onClick = { onPageChange(page) },
            )
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val bgColor = when {
        selected -> LolColors.Primary.copy(alpha = 0.08f)
        hovered -> LolColors.BgHover
        else -> Color.Transparent
    }
    val textColor = if (selected) LolColors.Primary else if (hovered) LolColors.TextPrimary else LolColors.TextSecondary
    val borderColor = if (selected) LolColors.Primary else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(start = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 좌측 강조선
        Box(
            Modifier
                .width(3.dp)
                .height(40.dp)
                .background(borderColor)
        )
        Spacer(Modifier.width(13.dp))
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = textColor)
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}
