package net.gijun.collector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import net.gijun.collector.lcu.LcuStatus
import net.gijun.collector.ui.theme.LolColors

@Composable
fun WindowScope.Titlebar(
    version: String,
    lcuStatus: LcuStatus,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(LolColors.BgSecondary),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 드래그 가능 영역 (타이틀)
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("LoL 수집기", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LolColors.TextPrimary)
            Text("v$version", fontSize = 10.sp, color = LolColors.TextSecondary)
        }

        // 상태
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 상태 점
            Box(
                Modifier
                    .size(6.dp)
                    .background(
                        if (lcuStatus.connected) LolColors.Win else LolColors.Error,
                        shape = androidx.compose.foundation.shape.CircleShape,
                    )
            )
            if (lcuStatus.connected) {
                Text(
                    "${lcuStatus.gameName}#${lcuStatus.tagLine}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = LolColors.Primary,
                    maxLines = 1,
                )
            } else {
                Text(
                    lcuStatus.reason ?: "미연결",
                    fontSize = 11.sp,
                    color = LolColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }

        // 윈도우 컨트롤
        WinButton(onClick = onMinimize) {
            Icon(Icons.Default.Minimize, contentDescription = "최소화", modifier = Modifier.size(14.dp), tint = LolColors.TextSecondary)
        }
        WinButton(onClick = onClose, isClose = true) {
            Icon(Icons.Default.Close, contentDescription = "닫기", modifier = Modifier.size(14.dp), tint = LolColors.TextSecondary)
        }
    }
}

@Composable
private fun WinButton(
    onClick: () -> Unit,
    isClose: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        isClose && hovered -> LolColors.Error
        hovered -> Color.White.copy(alpha = 0.06f)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
