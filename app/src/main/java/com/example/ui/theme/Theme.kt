package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val IndustrialDarkColorScheme =
  darkColorScheme(
    primary = TacticalGreen,
    onPrimary = SteelDarkest,
    primaryContainer = TacticalGreenDark,
    onPrimaryContainer = TextPrimary,
    secondary = TacticalCyan,
    onSecondary = SteelDarkest,
    secondaryContainer = SteelLight,
    onSecondaryContainer = TextPrimary,
    tertiary = TacticalAmber,
    onTertiary = SteelDarkest,
    background = SteelDarkest,
    onBackground = TextPrimary,
    surface = SteelDark,
    onSurface = TextPrimary,
    surfaceVariant = SteelMedium,
    onSurfaceVariant = TextSecondary,
    outline = SteelBorder,
    error = TacticalRed,
    onError = TextPrimary,
  )

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = IndustrialDarkColorScheme,
    typography = Typography,
    content = content,
  )
}
