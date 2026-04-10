package net.gijun.collector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.gijun.collector.api.PlayerStats
import net.gijun.collector.ui.theme.LolColors

@Composable
fun PlayerCard(
    riotId: String,
    data: PlayerStats?,
    loading: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)

    // Skeleton
    if (loading || data == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(LolColors.BgCard)
                .padding(8.dp)
        ) {
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(3.dp)).background(LolColors.Border))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(3.dp)).background(LolColors.Border))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) {
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(4.dp)).background(LolColors.Border))
                }
            }
        }
        return
    }

    val displayName = riotId.split("#").firstOrNull() ?: riotId
    val eloVal = data.elo?.takeIf { it.isFinite() }?.toInt()
    val eloColor = when {
        eloVal == null -> LolColors.TextSecondary
        eloVal >= 1200 -> LolColors.Win
        eloVal >= 1000 -> LolColors.Primary
        else -> LolColors.Loss
    }
    val hasMore = data.championStats.size > 3
    val showCount = if (expanded) data.championStats.size else 3

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LolColors.BgCard)
            .then(if (hasMore) Modifier.clickable { expanded = !expanded } else Modifier)
            .padding(8.dp)
    ) {
        Text(displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LolColors.TextPrimary, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            if (eloVal != null) "Elo $eloVal" else "Elo —",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = eloColor,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            data.championStats.take(showCount).forEach { c ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ChampionIcon(c.championId, size = 26.dp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${c.winRate.toInt()}%",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            c.winRate >= 60 -> LolColors.Loss
                            c.winRate >= 50 -> LolColors.Win
                            else -> LolColors.TextSecondary
                        },
                    )
                }
            }
            if (hasMore) {
                Text(
                    if (expanded) "▲" else "+${data.championStats.size - 3}",
                    fontSize = 10.sp,
                    color = LolColors.TextSecondary,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
    }
}
