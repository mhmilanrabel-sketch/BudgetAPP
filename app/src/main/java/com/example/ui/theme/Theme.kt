package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF003731),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldContainer,
    secondary = AmberGoldLight,
    onSecondary = Color(0xFF452200),
    secondaryContainer = Color(0xFF623200),
    onSecondaryContainer = AmberContainer,
    background = NavySurfaceDark,
    onBackground = Color(0xFFE2E8F0),
    surface = NavySurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = NavySurfaceContainer,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = CoralErrorDark,
    onError = Color(0xFF450A0A),
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralContainer
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Color(0xFF00201C),
    secondary = AmberGold,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Color(0xFF331800),
    background = NeutralLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = NeutralLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun MoneyManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
