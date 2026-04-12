package net.gijun.collector.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── LoL 스타일 색상 팔레트 ──────────────────────────

object LolColors {
    // Hextech Gold
    val Primary = Color(0xFFC8AA6E)
    val PrimaryLight = Color(0xFFF0E6D2)
    val PrimaryDark = Color(0xFF785A28)
    val PrimaryHover = Color(0xFFD4B87A)

    // Obsidian Backgrounds
    val BgPrimary = Color(0xFF06080C)
    val BgSecondary = Color(0xFF0C0F16)
    val BgTertiary = Color(0xFF12151F)
    val BgCard = Color(0xFF0C0F16)
    val BgHover = Color(0xFF1A1D26)

    // Warm Text
    val TextPrimary = Color(0xFFF0E6D2)
    val TextSecondary = Color(0xFFA09B8C)
    val TextDisabled = Color(0xFF5B5A56)
    val TextInverse = Color(0xFF06080C)

    // Borders
    val Border = Color(0xFF1E2328)
    val BorderLight = Color(0xFF32281E)

    // Semantic
    val Win = Color(0xFF0AC8B9)
    val Loss = Color(0xFFE84057)
    val Info = Color(0xFF0AC8B9)
    val Warning = Color(0xFFC8AA6E)
    val Success = Color(0xFF0AC8B9)
    val Error = Color(0xFFE84057)

    // Team Colors
    val Blue = Color(0xFF0AC8B9)
    val Red = Color(0xFFE84057)
}

fun winRateColor(wr: Double): Color = when {
    wr >= 60 -> LolColors.Loss
    wr >= 55 -> LolColors.Primary
    wr >= 50 -> LolColors.Win
    else -> LolColors.TextSecondary
}

private val DarkColorScheme = darkColorScheme(
    primary = LolColors.Primary,
    onPrimary = LolColors.TextInverse,
    primaryContainer = LolColors.PrimaryDark,
    onPrimaryContainer = LolColors.PrimaryLight,
    secondary = LolColors.Info,
    onSecondary = LolColors.TextInverse,
    background = LolColors.BgPrimary,
    onBackground = LolColors.TextPrimary,
    surface = LolColors.BgCard,
    surfaceVariant = LolColors.BgSecondary,
    onSurface = LolColors.TextPrimary,
    onSurfaceVariant = LolColors.TextSecondary,
    outline = LolColors.Border,
    outlineVariant = LolColors.BorderLight,
    error = LolColors.Error,
    onError = Color.White,
)

@Composable
fun LolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(
            titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = LolColors.TextPrimary),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = LolColors.Primary),
            bodyMedium = TextStyle(fontSize = 13.sp, color = LolColors.TextPrimary, lineHeight = 20.sp),
            bodySmall = TextStyle(fontSize = 11.sp, color = LolColors.TextSecondary),
            labelSmall = TextStyle(fontSize = 10.sp, color = LolColors.TextDisabled, fontFamily = FontFamily.Monospace),
        ),
        content = content,
    )
}
