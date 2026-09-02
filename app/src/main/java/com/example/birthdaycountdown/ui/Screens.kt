package com.example.birthdaycountdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.birthdaycountdown.data.*
import com.example.birthdaycountdown.domain.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val WATCHLIST_CATEGORIES_ROUTE = "watchlist/categories"
private enum class HomeFilter(val label: String) { ALL("全部"), UPCOMING("即将到来"), STARTED("已开始") }
internal enum class AddChoice(val recordType: RecordType?) {
    BIRTHDAY(RecordType.BIRTHDAY),
    ANNIVERSARY(RecordType.ANNIVERSARY),
    WATCHLIST(null)
}

internal fun watchlistSummary(count: Int): String = "正在追 $count 部"

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
                        onWatchlist = { navigateTopLevel(TopLevelDestination.WATCHLIST) }
                    )
                }
                composable(AppRoute.WATCHLIST) {
                    WatchlistScreen(
                        viewModel = watchlistViewModel,
                        onBack = { navController.popBackStack() },
                        onManageCategories = { navController.navigate(WATCHLIST_CATEGORIES_ROUTE) }
                    )
                }
                composable(AppRoute.WATCHLIST_ADD) {
                    WatchlistScreen(
                        viewModel = watchlistViewModel,
                        onBack = { navController.popBackStack() },
                        onManageCategories = { navController.navigate(WATCHLIST_CATEGORIES_ROUTE) },
                        startCreating = true
                    )
                }
                composable(WATCHLIST_CATEGORIES_ROUTE) {
                    CategoryManagerScreen(watchlistViewModel) { navController.popBackStack() }
                }
                composable(AppRoute.AI) {
                    AiHomeScreen(
                        historyRepository = aiHistoryRepository,
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
                    key(entry.arguments?.getLong("conversationId")?.takeUnless { it == -1L }) {
                        AiHomeScreen(
                            historyRepository = aiHistoryRepository,
                            onSettings = { navController.navigate(AppRoute.SETTINGS_AI) }
                        )
                    }
                }
                composable(
                    route = AppRoute.AI_IMAGE,
                    arguments = listOf(navArgument("conversationId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    })
                ) { entry ->
                    key(entry.arguments?.getLong("conversationId")?.takeUnless { it == -1L }) {
                        AiHomeScreen(
                            historyRepository = aiHistoryRepository,
                            onSettings = { navController.navigate(AppRoute.SETTINGS_AI) }
                        )
                    }
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
                            onBack = { navController.popBackStack() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChoiceScreen(onSelected: (AddChoice) -> Unit) {
    val choices = listOf(
        Triple(AddChoice.BIRTHDAY, "添加生日", Icons.Outlined.Cake),
        Triple(AddChoice.ANNIVERSARY, "添加纪念日", Icons.Outlined.Event),
        Triple(AddChoice.WATCHLIST, "添加追剧记录", Icons.Outlined.Movie)
    )
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("添加时间") }, colors = glassTopAppBarColors()) }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            choices.forEach { (choice, title, icon) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelected(choice) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    ListItem(
                        headlineContent = { Text(title) },
                        leadingContent = { Icon(icon, null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                    )
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(viewModel: AppViewModel, watchlistViewModel: WatchlistViewModel, onSettings: () -> Unit, onWatchlist: () -> Unit) {
    val records by viewModel.records.collectAsState()
    val watchRecords by watchlistViewModel.records.collectAsState()
    val watchCategories by watchlistViewModel.categories.collectAsState()
    val now by viewModel.now.collectAsState()
    val current = now.atZone(ZoneId.systemDefault())
    val upcoming = records.count { CountdownCalculator.snapshot(it, current).countdown != null }
    val started = records.count { CountdownCalculator.snapshot(it, current).elapsed != null }
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("我的") }, colors = glassTopAppBarColors()) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("记录概览", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ProfileStat("全部", records.size)
                        ProfileStat("即将到来", upcoming)
                        ProfileStat("已开始", started)
                    }
                }
            }
            GlassPanel(modifier = Modifier.fillMaxWidth().clickable(onClick = onSettings)) {
            ListItem(
                headlineContent = { Text("设置") },
                supportingContent = { Text("日期、文字、导航显示") },
                leadingContent = { Icon(Icons.Outlined.Settings, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier
            )
            }
            GlassPanel(modifier = Modifier.fillMaxWidth().clickable(onClick = onWatchlist)) {
                ListItem(
                    headlineContent = { Text("追剧记录") },
                    supportingContent = { Text("${watchRecords.size} 部 · ${watchCategories.size} 个分类") },
                    leadingContent = { Icon(Icons.Outlined.Movie, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    watchlistViewModel: WatchlistViewModel,
    onEdit: (CountdownEntity) -> Unit,
    onWatchlist: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val watchRecords by watchlistViewModel.records.collectAsState()
    val now by viewModel.now.collectAsState()
    val format by viewModel.format.collectAsState()
    val settings by viewModel.displaySettings.collectAsState()
    val localRecords = remember { mutableStateListOf<CountdownEntity>() }
    var deleting by remember { mutableStateOf<CountdownEntity?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(HomeFilter.ALL) }
    var pinnedExpanded by remember { mutableStateOf(true) }
    var soonExpanded by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(records, draggingId) {
        if (draggingId == null) {
            localRecords.clear()
            localRecords.addAll(records)
        }
    }

    val visibleRecords = localRecords.filter { record ->
        record.name.contains(query.trim(), ignoreCase = true) && when (filter) {
            HomeFilter.ALL -> true
            HomeFilter.UPCOMING -> CountdownCalculator.snapshot(record, now.atZone(ZoneId.systemDefault())).countdown != null
            HomeFilter.STARTED -> CountdownCalculator.snapshot(record, now.atZone(ZoneId.systemDefault())).elapsed != null
        }
    }
    val pinnedRecords = visibleRecords.filter { it.isPinned }
    val nonPinnedRecords = visibleRecords.filterNot { it.isPinned }
    val soonRecords = nonPinnedRecords.filter {
        CountdownCalculator.snapshot(it, now.atZone(ZoneId.systemDefault())).countdown?.let { duration -> duration <= Duration.ofDays(7) } == true
    }
    val otherRecords = nonPinnedRecords - soonRecords.toSet()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("时间") },
                actions = {
                    IconButton(onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) query = ""
                    }) {
                        Icon(if (searchVisible) Icons.Outlined.Close else Icons.Outlined.Search, if (searchVisible) "关闭搜索" else "搜索")
                    }
                },
                colors = glassTopAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (searchVisible) {
                    OutlinedTextField(query, { query = it }, label = { Text("搜索记录") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Row(Modifier.padding(top = if (searchVisible) 8.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeFilter.entries.forEach { option -> FilterChip(filter == option, { filter = option }, label = { Text(option.label) }) }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onWatchlist),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    ListItem(
                        headlineContent = { Text("追剧记录") },
                        supportingContent = { Text(watchlistSummary(watchRecords.size)) },
                        leadingContent = { Icon(Icons.Outlined.Movie, null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                    )
                }
            }
            if (visibleRecords.isEmpty()) item { Text(if (localRecords.isEmpty()) "还没有时间记录，请在底部“添加时间”中创建。" else "没有匹配的记录。", style = MaterialTheme.typography.bodyLarge) }
            if (pinnedRecords.isNotEmpty()) {
                item { CollapsibleSectionHeader("置顶", pinnedRecords.size, pinnedExpanded) { pinnedExpanded = !pinnedExpanded } }
                if (pinnedExpanded) items(pinnedRecords, key = { it.id }) { record -> CountdownCardItem(record, now, format, settings, draggingId, pinnedRecords.map { it.id }, onEdit, { deleting = it }, { viewModel.setPinned(record, !record.isPinned) }, viewModel, localRecords, { draggingId = it }) }
            }
            if (soonRecords.isNotEmpty()) {
                item { CollapsibleSectionHeader("7 天内", soonRecords.size, soonExpanded) { soonExpanded = !soonExpanded } }
            if (soonExpanded) items(soonRecords, key = { it.id }) { record -> CountdownCardItem(record, now, format, settings, draggingId, soonRecords.map { it.id }, onEdit, { deleting = it }, { viewModel.setPinned(record, !record.isPinned) }, viewModel, localRecords, { draggingId = it }) }
            }
            if (otherRecords.isNotEmpty()) {
                item { Text(if (filter == HomeFilter.STARTED) "已开始" else "其他记录", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                items(otherRecords, key = { it.id }) { record ->
                    CountdownCardItem(record, now, format, settings, draggingId, otherRecords.map { it.id }, onEdit, { deleting = it }, { viewModel.setPinned(record, !record.isPinned) }, viewModel, localRecords, { draggingId = it })
                }
            }
        }
    }

    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除记录？") },
            text = { Text(record.name) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(record)
                    deleting = null
                    scope.launch {
                        if (snackbarHostState.showSnackbar("已删除 ${record.name}", "撤销", withDismissAction = true) == SnackbarResult.ActionPerformed) viewModel.restore(record)
                    }
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun CollapsibleSectionHeader(title: String, count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text("$count 项", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "收起" else "展开")
    }
}

@Composable
private fun CountdownCardItem(record: CountdownEntity, now: Instant, format: DateFormatPreference, settings: AppDisplaySettings, draggingId: Long?, visibleIds: List<Long>, onEdit: (CountdownEntity) -> Unit, onDelete: (CountdownEntity) -> Unit, onPin: () -> Unit, viewModel: AppViewModel, localRecords: MutableList<CountdownEntity>, setDraggingId: (Long?) -> Unit) {
    var dragDistance by remember(record.id) { mutableFloatStateOf(0f) }
    val currentVisibleIds by rememberUpdatedState(visibleIds)
    val dragging = draggingId == record.id
    CountdownCard(record, now, format, settings, dragging, onClick = { if (!dragging) onEdit(record) }, onDelete = { onDelete(record) }, onPin = onPin,
        modifier = Modifier.shadow(if (dragging) 8.dp else 0.dp, MaterialTheme.shapes.medium).alpha(if (dragging) 0.92f else 1f).pointerInput(record.id) {
            detectDragGesturesAfterLongPress(onDragStart = { setDraggingId(record.id); dragDistance = 0f }, onDragCancel = { setDraggingId(null); dragDistance = 0f }, onDragEnd = { viewModel.reorder(localRecords.toList()); setDraggingId(null); dragDistance = 0f }) { change, amount ->
                change.consume(); dragDistance += amount.y
                if (abs(dragDistance) >= 72f) {
                    val reordered = moveVisibleItem(localRecords.toList(), currentVisibleIds, record.id, if (dragDistance > 0) 1 else -1) { it.id }
                    localRecords.clear(); localRecords.addAll(reordered)
                    dragDistance = 0f
                }
            }
        })
}

@Composable
private fun CountdownCard(record: CountdownEntity, now: Instant, format: DateFormatPreference, settings: AppDisplaySettings, dragging: Boolean, onClick: () -> Unit, onDelete: () -> Unit, onPin: () -> Unit, modifier: Modifier = Modifier) {
    val solarDateTime = CountdownCalculator.solarDateTime(record)
    val snapshot = CountdownCalculator.snapshot(record, now.atZone(ZoneId.systemDefault()))
    val dateStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = settings.dateTextSize.sp, fontWeight = if (settings.dateBold) FontWeight.Bold else FontWeight.Normal)
    val countdownStyle = MaterialTheme.typography.titleMedium.copy(fontSize = settings.countdownTextSize.sp, fontWeight = if (settings.countdownBold) FontWeight.Bold else FontWeight.Normal)
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), border = if (dragging) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null, colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(Modifier.fillMaxWidth().background(CardGradients.find(record.cardGradientId).brushOrSolid(record.cardBackgroundColor), MaterialTheme.shapes.medium)) {
            Column(Modifier.padding(16.dp)) {
                if (dragging) Text("正在调整顺序", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    StyledText(record.name, MaterialTheme.typography.titleLarge.copy(fontSize = settings.titleTextSize.sp, fontWeight = if (settings.titleBold) FontWeight.Bold else FontWeight.Normal), record.titleTextColor, record.titleGradientId, Modifier.weight(1f))
                    var menuOpen by remember(record.id) { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "更多操作") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text(if (record.isPinned) "取消置顶" else "置顶") }, onClick = { menuOpen = false; onPin() }, leadingIcon = { Icon(if (record.isPinned) Icons.Outlined.Star else Icons.Outlined.StarBorder, null) })
                            DropdownMenuItem(text = { Text("删除") }, onClick = { menuOpen = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                        }
                    }
                }
                if (record.calendarType == CalendarType.LUNAR && record.lunarYear != null && record.lunarMonth != null && record.lunarDay != null) {
                    val validLeap = record.lunarLeapMonth && runCatching { LunarCalendarConverter.leapMonthForYear(record.lunarYear) == record.lunarMonth }.getOrDefault(false)
                    if (record.showLunarDate && settings.showLunarDate) StyledText("农历 ${DateFormatter.formatLunar(LunarDate(record.lunarYear, record.lunarMonth, record.lunarDay, validLeap), solarDateTime.toLocalTime(), maskOptions(record.lunarDisplayMask, settings))}", dateStyle, record.lunarTextColor, record.lunarGradientId)
                    if (record.showSolarDate && settings.showSolarDate) StyledText("阳历 ${DateFormatter.format(solarDateTime, format, maskOptions(record.solarDisplayMask, settings))}", dateStyle, record.solarTextColor, record.solarGradientId)
                } else {
                    if (record.showSolarDate && settings.showSolarDate) StyledText("阳历 ${DateFormatter.format(solarDateTime, format, maskOptions(record.solarDisplayMask, settings))}", dateStyle, record.solarTextColor, record.solarGradientId)
                    val lunar = runCatching { LunarCalendarConverter.toLunar(solarDateTime.toLocalDate()) }.getOrNull()
                    if (record.showLunarDate && settings.showLunarDate && lunar != null) StyledText("农历 ${DateFormatter.formatLunar(lunar, solarDateTime.toLocalTime(), maskOptions(record.lunarDisplayMask, settings))}", dateStyle, record.lunarTextColor, record.lunarGradientId)
                }
                snapshot.countdown?.let { StyledText("还有 ${DisplayFormatter.countdown(it, maskOptions(record.countdownDisplayMask, settings))}", countdownStyle, record.countdownTextColor, record.countdownGradientId) }
                snapshot.elapsed?.let {
                    StyledText("已经 ${DisplayFormatter.elapsed(it, snapshot.elapsedRemainder ?: Duration.ZERO, maskOptions(record.countdownDisplayMask, settings))}", countdownStyle, record.countdownTextColor, record.countdownGradientId)
                    StyledText("下一个周年还有 ${DisplayFormatter.countdown(snapshot.nextAnniversary ?: Duration.ZERO, maskOptions(record.countdownDisplayMask, settings))}", dateStyle, record.countdownTextColor, record.countdownGradientId)
                }
            }
        }
    }
}

@Composable
private fun StyledText(text: String, style: androidx.compose.ui.text.TextStyle, solidColor: Int, gradientId: String, modifier: Modifier = Modifier) {
    val gradient = CardGradients.find(gradientId)
    Text(text, modifier = modifier, style = if (gradient.colors.size > 1) style.copy(brush = gradient.brushOrNull()) else style.copy(color = Color(solidColor)))
}

private fun maskOptions(mask: Int, settings: AppDisplaySettings) = DisplayOptions(
    mask and 1 != 0 && settings.showYears,
    mask and 2 != 0 && settings.showMonths,
    mask and 4 != 0 && settings.showDays,
    mask and 8 != 0 && settings.showHours,
    mask and 16 != 0 && settings.showMinutes,
    mask and 32 != 0 && settings.showSeconds
)
