package com.example.sol.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF205C46), onPrimary = Color.White,
    secondaryContainer = Color(0xFFD9EEE2), onSecondaryContainer = Color(0xFF163D2C),
    background = Color.White, surface = Color.White,
    surfaceVariant = Color(0xFFF0ECE2), onSurfaceVariant = Color(0xFF575E55),
    surfaceContainer = Color(0xFFF3EFE5), surfaceContainerHighest = Color(0xFFF0ECE2),
    outline = Color(0xFF8C938A), outlineVariant = Color(0xFFDEDCD3),
    onBackground = Color(0xFF17251E), onSurface = Color(0xFF17251E),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AD5B5), onPrimary = Color(0xFF003827),
    background = Color(0xFF101C16), surface = Color(0xFF101C16),
)
private val SolTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 40.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp, lineHeight = 26.sp),
)

@Composable
fun SolTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = SolTypography, content = content)
}
