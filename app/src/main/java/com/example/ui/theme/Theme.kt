package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ConnectNpBlue,
    onPrimary = Color.White,
    primaryContainer = ConnectNpBlueDark,
    onPrimaryContainer = Color.White,
    secondary = NepalCrimson,
    onSecondary = Color.White,
    secondaryContainer = NepalCrimsonDark,
    onSecondaryContainer = Color.White,
    tertiary = ConnectNpSuccess,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondary,
    outline = DarkSecondary.copy(alpha = 0.5f),
    error = ConnectNpDanger
)

private val LightColorScheme = lightColorScheme(
    primary = ConnectNpBlue,
    onPrimary = Color.White,
    primaryContainer = ConnectNpBlueLight,
    onPrimaryContainer = ConnectNpBlueDark,
    secondary = NepalCrimson,
    onSecondary = Color.White,
    secondaryContainer = NepalCrimsonContainer,
    onSecondaryContainer = NepalCrimsonDark,
    tertiary = ConnectNpSuccess,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightSecondary,
    outline = LightSurfaceVariant,
    error = ConnectNpDanger
)

@Composable
fun ConnectNpTheme(
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ConnectNpTheme(darkTheme = darkTheme, content = content)
}
