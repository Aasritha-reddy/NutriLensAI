package com.nutrilens.nutrilensai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary             = NutriGreen,
    onPrimary           = Color.White,
    primaryContainer    = NutriGreenLight,
    onPrimaryContainer  = NutriGreenDark,
    secondary           = NutriGreenAccent,
    onSecondary         = Color.White,
    background          = NutriBackground,
    onBackground        = NutriTextPrimary,
    surface             = NutriSurface,
    onSurface           = NutriTextPrimary,
    surfaceVariant      = NutriGreenLight,
    onSurfaceVariant    = NutriTextSecondary,
    outline             = NutriDivider,
    outlineVariant      = Color(0xFFE5E7EB),
)

private val DarkColorScheme = darkColorScheme(
    primary             = NutriGreenAccent,
    onPrimary           = Color(0xFF003828),
    primaryContainer    = NutriGreenDark,
    onPrimaryContainer  = NutriGreenLight,
    secondary           = NutriGreen,
    background          = Color(0xFF111827),
    onBackground        = Color(0xFFF9FAFB),
    surface             = Color(0xFF1F2937),
    onSurface           = Color(0xFFF3F4F6),
    surfaceVariant      = Color(0xFF374151),
    onSurfaceVariant    = Color(0xFF9CA3AF),
)

@Composable
fun NutriLensAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = NutriTypography,
        content     = content
    )
}
