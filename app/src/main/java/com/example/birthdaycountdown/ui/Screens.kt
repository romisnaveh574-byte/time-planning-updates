package com.example.birthdaycountdown.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.birthdaycountdown.data.AiHistoryRepository
import com.example.birthdaycountdown.data.RecordType

@Composable
fun AppNav(viewModel: AppViewModel, watchlistViewModel: WatchlistViewModel, aiHistoryRepository: AiHistoryRepository, onRequestNotifications: () -> Unit) {
    val navController = rememberNavController()
    val navSettings by viewModel.bottomNavSettings.collectAsState()
    val records by viewModel.records.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = topLevelDestinationFor(currentRoute)
        .takeIf { shouldShowTopLevelNavigation(currentRoute) }
    val navigateTopLevel: (TopLevelDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    AppPage {
        RootAppScaffold(
            currentDestination = currentDestination,
            settings = navSettings,
            onDestinationSelected = navigateTopLevel
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.TIME,
                modifier = Modifier.padding(padding)
            ) {
                composable(AppRoute.TIME) {
                    HomeScreen(
                        viewModel = viewModel,
                        watchlistViewModel = watchlistViewModel,
                        onEdit = { navController.navigate(recordEditRoute(recordId = it.id)) },
                        onAdd = { navController.navigate(homeAddRoute()) },
                        onWatchlist = { navigateTopLevel(TopLevelDestination.WATCHLIST) }
                    )
                }
                composable(AppRoute.WATCHLIST) {
                    WatchlistScreen(
                        viewModel = watchlistViewModel,
                        onManageCategories = { navController.navigate(AppRoute.WATCHLIST_CATEGORIES) },
                        onAdd = { navController.navigate(AppRoute.WATCHLIST_ADD) }
                    )
                }
                composable(AppRoute.WATCHLIST_ADD) {
                    WatchlistScreen(
                        viewModel = watchlistViewModel,
                        onManageCategories = { navController.navigate(AppRoute.WATCHLIST_CATEGORIES) },
                        onAdd = { navController.navigate(AppRoute.WATCHLIST_ADD) },
                        startCreating = true,
                        onCreationFinished = { navController.popBackStack() }
                    )
                }
                composable(AppRoute.WATCHLIST_CATEGORIES) {
                    CategoryManagerScreen(watchlistViewModel) { navController.popBackStack() }
                }
                composable(AppRoute.AI) {
                    AiHomeScreen(
                        historyRepository = aiHistoryRepository,
                        onChat = { navController.navigate(aiChatRoute(it)) },
                        onImage = { navController.navigate(aiImageRoute(it)) },
                        onSettings = { navController.navigate(AppRoute.SETTINGS_AI) }
                    )
                }
                composable(
                    route = AppRoute.AI_CHAT,
                    arguments = listOf(navArgument("conversationId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    })
                ) { entry ->
                    AiDestinationScreen(
                        destination = requireNotNull(aiDestinationFor(requireNotNull(entry.destination.route))),
                        historyRepository = aiHistoryRepository,
                        conversationId = nullableConversationId(entry.arguments?.getLong("conversationId") ?: -1L),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = AppRoute.AI_IMAGE,
                    arguments = listOf(navArgument("conversationId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    })
                ) { entry ->
                    AiDestinationScreen(
                        destination = requireNotNull(aiDestinationFor(requireNotNull(entry.destination.route))),
                        historyRepository = aiHistoryRepository,
                        conversationId = nullableConversationId(entry.arguments?.getLong("conversationId") ?: -1L),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(AppRoute.PROFILE) {
                    ProfileScreen(
                        viewModel = viewModel,
                        watchlistViewModel = watchlistViewModel,
                        onSettings = { navController.navigate(AppRoute.SETTINGS) },
                        onWatchlist = { navigateTopLevel(TopLevelDestination.WATCHLIST) }
                    )
                }
                composable(AppRoute.ADD_CHOICE) {
                    AddChoiceScreen { choice ->
                        when (choice) {
                            AddChoice.BIRTHDAY, AddChoice.ANNIVERSARY -> navController.navigate(
                                recordEditRoute(recordType = requireNotNull(choice.recordType))
                            )
                            AddChoice.WATCHLIST -> navController.navigate(AppRoute.WATCHLIST_ADD)
                        }
                    }
                }
                composable(
                    route = AppRoute.RECORD_EDIT,
                    arguments = listOf(navArgument("recordId") { type = NavType.LongType })
                ) { entry ->
                    val recordId = entry.arguments?.getLong("recordId") ?: -1L
                    val existing = records.firstOrNull { it.id == recordId }
                    if (existing == null) {
                        LoadingState(title = "加载记录")
                    } else {
                        EditScreen(
                            existing = existing,
                            viewModel = viewModel,
                            onRequestNotifications = onRequestNotifications,
                            onBack = { navController.popBackStack() },
                            onDelete = {
                                viewModel.delete(it)
                                navController.popBackStack()
                            }
                        ) { navController.popBackStack() }
                    }
                }
                composable(
                    route = AppRoute.RECORD_NEW,
                    arguments = listOf(navArgument("recordType") { type = NavType.StringType })
                ) { entry ->
                    val recordType = RecordType.valueOf(requireNotNull(entry.arguments?.getString("recordType")))
                    EditScreen(
                        existing = null,
                        viewModel = viewModel,
                        onRequestNotifications = onRequestNotifications,
                        initialType = recordType,
                        onBack = { navController.popBackStack() }
                    ) {
                        if (!navController.popBackStack(AppRoute.TIME, inclusive = false)) {
                            navigateTopLevel(TopLevelDestination.TIME)
                        }
                    }
                }
                composable(AppRoute.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onDisplaySettings = { navController.navigate(AppRoute.SETTINGS_DISPLAY) },
                        onNavigationSettings = { navController.navigate(AppRoute.SETTINGS_NAVIGATION) },
                        onDataBackup = { navController.navigate(AppRoute.SETTINGS_BACKUP) },
                        onApplicationSettings = { navController.navigate(AppRoute.SETTINGS_APPLICATION) },
                        onAiSettings = { navController.navigate(AppRoute.SETTINGS_AI) },
                        onDone = { navController.popBackStack() }
                    )
                }
                composable(AppRoute.SETTINGS_DISPLAY) {
                    DisplaySettingsScreen(viewModel) { navController.popBackStack() }
                }
                composable(AppRoute.SETTINGS_NAVIGATION) {
                    BottomNavSettingsScreen(viewModel) { navController.popBackStack() }
                }
                composable(AppRoute.SETTINGS_BACKUP) {
                    DataBackupSettingsScreen(viewModel) { navController.popBackStack() }
                }
                composable(AppRoute.SETTINGS_APPLICATION) {
                    ApplicationSettingsScreen { navController.popBackStack() }
                }
                composable(AppRoute.SETTINGS_AI) {
                    AiSettingsScreen { navController.popBackStack() }
                }
            }
        }
    }
}

@Composable
private fun AiDestinationScreen(
    destination: AiDestination,
    historyRepository: AiHistoryRepository,
    conversationId: Long?,
    onBack: () -> Unit
) {
    when (destination) {
        AiDestination.CHAT -> AiChatScreen(historyRepository, conversationId, onBack)
        AiDestination.IMAGE -> AiImageScreen(historyRepository, conversationId, onBack)
    }
}

internal fun navIcon(id: BottomNavIconId): ImageVector = when (id) {
    BottomNavIconId.CLOCK -> Icons.Outlined.AccessTime
    BottomNavIconId.CALENDAR_PLUS -> Icons.Outlined.AddCircleOutline
    BottomNavIconId.MOVIE -> Icons.Outlined.Movie
    BottomNavIconId.USER -> Icons.Outlined.PersonOutline
    BottomNavIconId.HEART -> Icons.Outlined.FavoriteBorder
    BottomNavIconId.STAR -> Icons.Outlined.StarBorder
    BottomNavIconId.SETTINGS -> Icons.Outlined.Settings
}
