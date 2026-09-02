package com.example.birthdaycountdown.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0C6670),
    onPrimary = Color.White,
    secondary = Color(0xFFEF7B61),
    onSecondary = Color(0xFF21120F),
    background = Color(0xFFF5F7F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFE7ECEE),
    outlineVariant = Color(0xFFDDE3E5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF85D7DF),
    onPrimary = Color(0xFF07363B),
    secondary = Color(0xFFFFA18D),
    onSecondary = Color(0xFF471006),
    background = Color(0xFF151719),
    surface = Color(0xFF22272A),
    surfaceVariant = Color(0xFF2D3437),
    outlineVariant = Color(0xFF3E484C)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(AppUiTokens.surfaceCornerRadius),
    medium = RoundedCornerShape(AppUiTokens.surfaceCornerRadius),
    large = RoundedCornerShape(AppUiTokens.largeSurfaceCornerRadius)
)

@Composable
fun TimePlanningTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        shapes = AppShapes,
        content = content
    )
}
