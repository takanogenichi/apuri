package com.example.frenchquiz.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = FrBlue,
    onPrimary = Color.White,
    secondary = FrRed,
    onSecondary = Color.White,
    background = LightBackground,
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = FrBlueLight,
    onPrimary = Color.Black,
    secondary = FrRedLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    surface = Color(0xFF1E2227),
)

@Composable
fun FrenchQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
