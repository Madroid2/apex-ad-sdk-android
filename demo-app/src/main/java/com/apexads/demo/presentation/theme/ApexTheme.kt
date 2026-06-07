package com.apexads.demo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ApexPurple = Color(0xFF6200EE)
private val ApexPurpleDark = Color(0xFF3700B3)
private val ApexTeal = Color(0xFF03DAC5)

private val LightColors = lightColorScheme(
    primary = ApexPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    secondary = ApexTeal,
    onSecondary = Color.Black,
    surface = Color(0xFFFFFBFE),
    background = Color(0xFFFFFBFE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    secondary = ApexTeal,
    onSecondary = Color.Black,
)

@Composable
fun ApexTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
