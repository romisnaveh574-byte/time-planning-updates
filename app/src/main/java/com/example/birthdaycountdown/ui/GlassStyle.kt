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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object GlassStyle {
    const val panelAlpha = 0.72f
    const val highlightAlpha = 0.32f
    const val elevation = 6f
    const val topBarAlpha = 0.76f
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
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = null,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        content = content
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun glassTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = GlassStyle.topBarAlpha),
    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = GlassStyle.topBarAlpha)
)
