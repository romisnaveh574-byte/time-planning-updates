package com.example.birthdaycountdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

internal object AppUiTokens {
    val surfaceCornerRadius = 8.dp
    val largeSurfaceCornerRadius = 8.dp
    val pageHorizontalPadding = 16.dp
    val minimumTouchTarget = 48.dp
    val contentSpacing = 12.dp
    val sectionSpacing = 20.dp
}

internal enum class StatusTone { INFO, SUCCESS, WARNING, ERROR }

internal data class StatusColors(val container: Color, val content: Color)

private val SuccessLightColors = StatusColors(
    container = Color(0xFFD6F2E1),
    content = Color(0xFF123A24)
)

private val SuccessDarkColors = StatusColors(
    container = Color(0xFF1F4D35),
    content = Color(0xFFD6F2E1)
)

private val WarningLightColors = StatusColors(
    container = Color(0xFFFFE2B8),
    content = Color(0xFF5C3B00)
)

private val WarningDarkColors = StatusColors(
    container = Color(0xFF6A4A0A),
    content = Color(0xFFFFE2B8)
)

internal fun warningStatusColors(useDarkTheme: Boolean): StatusColors =
    if (useDarkTheme) WarningDarkColors else WarningLightColors

@Composable
internal fun statusColors(tone: StatusTone): StatusColors {
    val colorScheme = MaterialTheme.colorScheme
    return when (tone) {
        StatusTone.INFO -> StatusColors(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
        StatusTone.SUCCESS -> if (colorScheme.surface.luminance() < 0.5f) SuccessDarkColors else SuccessLightColors
        StatusTone.WARNING -> warningStatusColors(useDarkTheme = colorScheme.surface.luminance() < 0.5f)
        StatusTone.ERROR -> StatusColors(colorScheme.errorContainer, colorScheme.onErrorContainer)
    }
}

@Composable
internal fun AppPage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
internal fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = AppUiTokens.minimumTouchTarget)
            .heightIn(min = AppUiTokens.minimumTouchTarget),
        enabled = enabled,
        shape = RoundedCornerShape(AppUiTokens.surfaceCornerRadius),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors()
    ) {
        Text(text)
    }
}

@Composable
internal fun AppListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    overlineText: String? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppUiTokens.surfaceCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        ListItem(
            headlineContent = { Text(headline) },
            modifier = Modifier.fillMaxWidth(),
            overlineContent = overlineText?.let { text -> { Text(text) } },
            supportingContent = supportingText?.let { text -> { Text(text) } },
            trailingContent = trailingContent,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun StatusLabel(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val colors = statusColors(tone)
    Surface(
        modifier = modifier,
        color = colors.container,
        contentColor = colors.content,
        shape = RoundedCornerShape(AppUiTokens.surfaceCornerRadius)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
internal fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    StateMessage(
        title = title,
        modifier = modifier,
        message = message,
        actionLabel = actionLabel,
        onActionClick = onActionClick
    )
}

@Composable
internal fun LoadingState(
    title: String = "加载中",
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppUiTokens.pageHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.contentSpacing)
        ) {
            CircularProgressIndicator()
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun ErrorState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String = "重试",
    onActionClick: (() -> Unit)? = null
) {
    StateMessage(
        title = title,
        modifier = modifier,
        message = message,
        actionLabel = actionLabel,
        onActionClick = onActionClick,
        tone = StatusTone.ERROR
    )
}

@Composable
private fun StateMessage(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    tone: StatusTone? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppUiTokens.pageHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.contentSpacing)
        ) {
            if (tone != null) {
                StatusLabel(text = title, tone = tone)
            } else {
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel != null && onActionClick != null) {
                PrimaryActionButton(text = actionLabel, onClick = onActionClick)
            }
        }
    }
}
