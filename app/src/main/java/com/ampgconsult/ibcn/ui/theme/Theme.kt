package com.ampgconsult.ibcn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DeepBlue,
    secondary = LightBlue,
    tertiary = ElectricCyan,
    background = Charcoal,
    surface = Charcoal,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Charcoal,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DeepBlue,
    secondary = LightBlue,
    tertiary = ElectricCyan,
    background = LightGray,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Charcoal,
    onBackground = Charcoal,
    onSurface = Charcoal
)

@Composable
fun IBCNTheme(
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
