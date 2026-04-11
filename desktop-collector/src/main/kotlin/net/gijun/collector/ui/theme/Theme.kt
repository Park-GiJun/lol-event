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
    val Primary = Color(0xFFC89B3C)        // 골드
    val PrimaryLight = Color(0xFFD4AF5A)
    val PrimaryDark = Color(0xFFA07830)
    val PrimaryHover = Color(0xFFB8882C)

    val BgPrimary = Color(0xFF0A1428)
    val BgSecondary = Color(0xFF091428)
    val BgTertiary = Color(0xFF0E1C34)
    val BgCard = Color(0xFF0D1B2E)
    val BgHover = Color(0xFF152035)

    val TextPrimary = Color(0xFFF0E6D3)
    val TextSecondary = Color(0xFFA0A8B0)
    val TextDisabled = Color(0xFF5B5A56)
    val TextInverse = Color(0xFF0A1428)

    val Border = Color(0xFF1E2D40)
    val BorderLight = Color(0xFF243547)

    val Win = Color(0xFF0BC4B4)
    val Loss = Color(0xFFE84040)
    val Info = Color(0xFF3B9EFF)
    val Warning = Color(0xFFFFD166)
    val Success = Color(0xFF0BC4B4)
    val Error = Color(0xFFE84040)

    val Blue = Color(0xFF4A90D9)
    val Red = Color(0xFFD94A4A)
}

fun winRateColor(wr: Double): Color = when {
    wr >= 60 -> Color(0xFFFF4E50)
    wr >= 55 -> Color(0xFFFFB347)
    wr >= 50 -> Color(0xFF4CAF50)
    else -> LolColors.TextSecondary
}

private val DarkColorScheme = darkColorScheme(
    primary = LolColors.Primary,
    onPrimary = LolColors.TextInverse,
    primaryContainer = LolColors.PrimaryDark,
    secondary = LolColors.Info,
    background = LolColors.BgPrimary,
    surface = LolColors.BgCard,
    surfaceVariant = LolColors.BgSecondary,
    onBackground = LolColors.TextPrimary,
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
