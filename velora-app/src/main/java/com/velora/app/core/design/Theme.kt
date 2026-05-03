package com.velora.app.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Dark scheme (default — Velora is dark-mode-first) ─────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = White,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo200,
    secondary = Gold400,
    onSecondary = NavyBlack,
    secondaryContainer = Color(0xFF3D2E00),
    onSecondaryContainer = Gold200,
    background = NavyBlack,
    onBackground = TextPrimary,
    surface = NavyDark,
    onSurface = TextPrimary,
    surfaceVariant = NavySurface,
    onSurfaceVariant = TextSecondary,
    outline = NavyCard,
    error = ErrorRed,
    onError = White,
    inverseSurface = TextPrimary,
    inverseOnSurface = NavyBlack,
)

// ── Light scheme ──────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = Indigo700,
    secondary = Gold500,
    onSecondary = White,
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = Color(0xFF3E2000),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = Color(0xFFCACDD8),
    error = ErrorRed,
    onError = White,
    inverseSurface = LightOnSurface,
    inverseOnSurface = LightSurface,
)

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun VeloraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VeloraTypography,
        shapes = VeloraShapes,
        content = content,
    )
}
