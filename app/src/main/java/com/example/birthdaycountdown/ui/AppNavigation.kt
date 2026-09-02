package com.example.birthdaycountdown.ui

import com.example.birthdaycountdown.data.RecordType

internal object AppRoute {
    const val TIME = "time"
    const val WATCHLIST = "watchlist"
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

internal val TOP_LEVEL_DESTINATIONS = TopLevelDestination.entries

internal fun recordEditRoute(recordId: Long? = null, recordType: RecordType? = null): String = when {
    recordId != null -> "record/edit/$recordId"
    recordType != null -> "record/new/${recordType.name}"
    else -> error("recordId or recordType is required")
}

internal fun aiChatRoute(conversationId: Long? = null) = "ai/chat?conversationId=${conversationId ?: -1L}"

internal fun aiImageRoute(conversationId: Long? = null) = "ai/image?conversationId=${conversationId ?: -1L}"
