package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HighDensityPrimaryDark,
    onPrimary = HighDensityPrimaryNavy,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    background = HighDensityBgDark,
    onBackground = HighDensityTextLight,
    surface = HighDensitySurfaceDark,
    onSurface = HighDensityTextLight,
    surfaceVariant = HighDensitySurfaceVariantDark,
    onSurfaceVariant = HighDensityTextMutedDark,
    outline = HighDensityBorderCardDark,
    outlineVariant = HighDensityBorderChipDark
)

private val LightColorScheme = lightColorScheme(
    primary = HighDensityPrimaryNavy,
    onPrimary = Color.White,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    background = HighDensityBgLight,
    onBackground = HighDensityTextDark,
    surface = HighDensitySurfaceLight,
    onSurface = HighDensityTextDark,
    surfaceVariant = HighDensitySurfaceVariantLight,
    onSurfaceVariant = HighDensityTextMuted,
    outline = HighDensityBorderCard,
    outlineVariant = HighDensityBorderChip
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to faithfully render the High Density theme
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
