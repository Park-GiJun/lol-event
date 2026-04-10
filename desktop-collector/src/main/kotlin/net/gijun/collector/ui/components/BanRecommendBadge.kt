package net.gijun.collector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.gijun.collector.ui.theme.LolColors

@Composable
fun BanRecommendBadge(
    champion: String,
    championId: Int,
    isHighThreat: Boolean,
    winRate: Double? = null,
    games: Int? = null,
) {
    val bg = if (isHighThreat) LolColors.Error.copy(alpha = 0.1f) else LolColors.BgHover
    val borderColor = if (isHighThreat) LolColors.Error.copy(alpha = 0.3f) else LolColors.Border
    val wrColor = when {
        winRate == null -> LolColors.TextSecondary
        winRate >= 60 -> LolColors.Error
        winRate >= 50 -> LolColors.Win
        else -> LolColors.TextSecondary
    }
    val shape = RoundedCornerShape(4.dp)

    Row(
        modifier = Modifier
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ChampionIcon(championId, size = 22.dp)
        Column {
            Text(champion, fontSize = 11.sp, color = LolColors.TextPrimary)
            if (winRate != null || games != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    winRate?.let {
                        Text("${it.toInt()}%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = wrColor)
                    }
                    games?.let {
                        Text("(${it}판)", fontSize = 10.sp, color = LolColors.TextSecondary)
                    }
                }
            }
        }
        if (isHighThreat) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(10.dp), tint = LolColors.Warning)
        }
    }
}
