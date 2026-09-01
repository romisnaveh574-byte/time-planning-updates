package com.example.birthdaycountdown.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val PurplePinkStart = Color(VisualTokens.primaryGradientStart)
internal val PurplePinkEnd = Color(VisualTokens.primaryGradientEnd)
internal val ColdWhite = Color(0xFFF8F7FC)
internal val Ink = Color(0xFF1D1B2D)

private val LightColors = lightColorScheme(
    primary = PurplePinkStart,
    onPrimary = Color.White,
    secondary = Color(0xFFC83593),
    background = ColdWhite,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0ECF7),
    onSurfaceVariant = Color(0xFF686277),
    outlineVariant = Color(0xFFE2DDEA),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD6B8FF),
    onPrimary = Color(0xFF28104E),
    secondary = Color(0xFFFFA8D5),
    background = Color(0xFF161321),
    onBackground = Color(0xFFE8E1F0),
    surface = Color(0xFF201A2A),
    onSurface = Color(0xFFE8E1F0),
    surfaceVariant = Color(0xFF30283C),
    onSurfaceVariant = Color(0xFFCBC2D4),
    outlineVariant = Color(0xFF4B4256),
    primaryContainer = Color(0xFF4B287F),
    secondaryContainer = Color(0xFF683653),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun TimePlanningTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
