package com.example.birthdaycountdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object GlassStyle {
    const val panelAlpha = 1f
    const val highlightAlpha = 0f
    const val elevation = 0f
    const val topBarAlpha = 1f
    val surfaceCornerRadius = 8.dp
    val primaryGradient = listOf(PurplePinkStart, PurplePinkEnd)
    val primaryBrush: Brush = Brush.linearGradient(primaryGradient)
}

@Composable
internal fun GlassBackdrop(content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(colors.background)) {
        content()
    }
}

@Composable
internal fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlassStyle.surfaceCornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = GlassStyle.panelAlpha),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        shadowElevation = GlassStyle.elevation.dp,
        tonalElevation = 0.dp,
        content = content
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun glassTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background.copy(alpha = GlassStyle.topBarAlpha),
    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = GlassStyle.topBarAlpha)
)
