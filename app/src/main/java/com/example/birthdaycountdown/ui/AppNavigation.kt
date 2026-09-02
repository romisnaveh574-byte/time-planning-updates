package com.example.birthdaycountdown.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.birthdaycountdown.data.RecordType

internal object AppRoute {
    const val TIME = "time"
    const val WATCHLIST = "watchlist"
    const val WATCHLIST_CATEGORIES = "watchlist/categories"
    const val WATCHLIST_ADD = "watchlist/add"
    const val AI = "ai"
    const val AI_CHAT = "ai/chat?conversationId={conversationId}"
    const val AI_IMAGE = "ai/image?conversationId={conversationId}"
    const val PROFILE = "profile"
    const val ADD_CHOICE = "record/add"
    const val RECORD_EDIT = "record/edit/{recordId}"
    const val RECORD_NEW = "record/new/{recordType}"
    const val SETTINGS = "settings"
    const val SETTINGS_DISPLAY = "settings/display"
    const val SETTINGS_NAVIGATION = "settings/navigation"
    const val SETTINGS_BACKUP = "settings/backup"
    const val SETTINGS_APPLICATION = "settings/application"
    const val SETTINGS_AI = "settings/ai"
}

internal enum class TopLevelDestination(val route: String) {
    TIME(AppRoute.TIME),
    WATCHLIST(AppRoute.WATCHLIST),
    AI(AppRoute.AI),
    PROFILE(AppRoute.PROFILE)
}

internal enum class AiDestination { CHAT, IMAGE }

internal val TOP_LEVEL_DESTINATIONS = TopLevelDestination.entries

internal fun recordEditRoute(recordId: Long? = null, recordType: RecordType? = null): String = when {
    recordId != null -> "record/edit/$recordId"
    recordType != null -> "record/new/${recordType.name}"
    else -> error("recordId or recordType is required")
}

internal fun aiChatRoute(conversationId: Long? = null) = "ai/chat?conversationId=${conversationId ?: -1L}"

internal fun aiImageRoute(conversationId: Long? = null) = "ai/image?conversationId=${conversationId ?: -1L}"

internal fun homeAddRoute() = AppRoute.ADD_CHOICE

internal fun aiDestinationFor(route: String): AiDestination? = when (route.substringBefore('?')) {
    AppRoute.AI_CHAT.substringBefore('?') -> AiDestination.CHAT
    AppRoute.AI_IMAGE.substringBefore('?') -> AiDestination.IMAGE
    else -> null
}

internal fun nullableConversationId(conversationId: Long): Long? =
    conversationId.takeUnless { it == -1L }

internal fun topLevelDestinationFor(route: String?): TopLevelDestination? {
    val routePath = route?.substringBefore('?') ?: return null
    return when {
        routePath == AppRoute.TIME || routePath.startsWith("record/") -> TopLevelDestination.TIME
        routePath == AppRoute.WATCHLIST || routePath.startsWith("watchlist/") -> TopLevelDestination.WATCHLIST
        routePath == AppRoute.AI || routePath.startsWith("ai/") -> TopLevelDestination.AI
        routePath == AppRoute.PROFILE || routePath == AppRoute.SETTINGS || routePath.startsWith("settings/") -> TopLevelDestination.PROFILE
        else -> null
    }
}

internal fun shouldShowTopLevelNavigation(route: String?): Boolean =
    TOP_LEVEL_DESTINATIONS.any { it.route == route }

internal fun AppNavigationItems(settings: BottomNavSettings) = listOf(
    TopLevelDestination.TIME to settings.time,
    TopLevelDestination.WATCHLIST to settings.add,
    TopLevelDestination.AI to settings.ai,
    TopLevelDestination.PROFILE to settings.profile
)

@Composable
internal fun RootAppScaffold(
    currentDestination: TopLevelDestination?,
    settings: BottomNavSettings,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (currentDestination != null) {
                        AppBottomNavigation(currentDestination, settings, onDestinationSelected)
                    }
                },
                content = content
            )
        } else {
            Row(Modifier.fillMaxSize()) {
                if (currentDestination != null) {
                    AppNavigationRail(currentDestination, settings, onDestinationSelected)
                }
                Box(Modifier.weight(1f)) { content(PaddingValues()) }
            }
        }
    }
}

@Composable
private fun AppBottomNavigation(
    currentDestination: TopLevelDestination,
    settings: BottomNavSettings,
    onDestinationSelected: (TopLevelDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        AppNavigationItems(settings).forEach { (destination, item) ->
            NavigationBarItem(
                selected = currentDestination == destination,
                onClick = { onDestinationSelected(destination) },
                icon = { if (item.showIcon) Icon(navIcon(item.icon), item.label) },
                label = if (item.showLabel) ({ Text(item.label, maxLines = 1) }) else null,
                alwaysShowLabel = item.showLabel
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentDestination: TopLevelDestination,
    settings: BottomNavSettings,
    onDestinationSelected: (TopLevelDestination) -> Unit
) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        AppNavigationItems(settings).forEach { (destination, item) ->
            NavigationRailItem(
                selected = currentDestination == destination,
                onClick = { onDestinationSelected(destination) },
                icon = { if (item.showIcon) Icon(navIcon(item.icon), item.label) },
                label = if (item.showLabel) ({ Text(item.label, maxLines = 1) }) else null,
                alwaysShowLabel = item.showLabel
            )
        }
    }
}
