package com.example.birthdaycountdown.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.Chat
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birthdaycountdown.data.*
import com.example.birthdaycountdown.domain.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class MainTab { HOME, RECORDS, AI, PROFILE }
private enum class SettingsPage { NONE, ROOT, DISPLAY, DATA_BACKUP, APPLICATION, AI }
private enum class WatchlistPage { NONE, LIST, EDITOR, CATEGORIES }
private enum class HomeFilter(val label: String) { ALL("全部"), UPCOMING("即将到来"), STARTED("已开始") }
internal enum class AddChoice(val recordType: RecordType?) {
    BIRTHDAY(RecordType.BIRTHDAY),
    ANNIVERSARY(RecordType.ANNIVERSARY),
    WATCHLIST(null)
}

private data class AddChoiceEntry(
    val choice: AddChoice,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

internal fun watchlistSummary(count: Int): String = "正在追 $count 部"

@Composable
fun AppNav(viewModel: AppViewModel, watchlistViewModel: WatchlistViewModel, aiHistoryRepository: AiHistoryRepository, onRequestNotifications: () -> Unit) {
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var editing by remember { mutableStateOf<CountdownEntity?>(null) }
    var settingsPage by remember { mutableStateOf(SettingsPage.NONE) }
    var watchlistPage by remember { mutableStateOf(WatchlistPage.NONE) }
    var watchlistEditing by remember { mutableStateOf<WatchRecordEntity?>(null) }
    var watchlistStatus by remember { mutableStateOf(SYSTEM_WATCHING_ID) }
    var watchlistFeedback by remember { mutableStateOf<String?>(null) }
    var timeRecordType by remember { mutableStateOf<RecordType?>(null) }
    var aiLaunchTarget by remember { mutableStateOf<AiLaunchTarget?>(null) }
    var adding by remember { mutableStateOf(false) }
    var addChoice by remember { mutableStateOf<AddChoice?>(null) }

    BackHandler(enabled = editing != null || settingsPage != SettingsPage.NONE || watchlistPage != WatchlistPage.NONE || timeRecordType != null || adding || addChoice != null || tab != MainTab.HOME) {
        when {
            editing != null -> editing = null
            settingsPage != SettingsPage.NONE -> {
                settingsPage = if (settingsPage == SettingsPage.ROOT) SettingsPage.NONE else SettingsPage.ROOT
            }
            watchlistPage == WatchlistPage.CATEGORIES -> watchlistPage = WatchlistPage.LIST
            watchlistPage == WatchlistPage.EDITOR -> {
                watchlistPage = WatchlistPage.LIST
                watchlistEditing = null
            }
            watchlistPage == WatchlistPage.LIST -> {
                watchlistPage = WatchlistPage.NONE
            }
            timeRecordType != null -> timeRecordType = null
            addChoice != null -> addChoice = null
            adding -> adding = false
            tab != MainTab.HOME -> tab = MainTab.HOME
        }
    }

    GlassBackdrop {
    when {
        editing != null -> EditScreen(editing, viewModel, onRequestNotifications) { editing = null }
        settingsPage == SettingsPage.ROOT -> SettingsScreen(
            viewModel = viewModel,
            onDisplaySettings = { settingsPage = SettingsPage.DISPLAY },
            onDataBackup = { settingsPage = SettingsPage.DATA_BACKUP },
            onApplicationSettings = { settingsPage = SettingsPage.APPLICATION },
            onAiSettings = { settingsPage = SettingsPage.AI },
            onDone = { settingsPage = SettingsPage.NONE }
        )
        settingsPage == SettingsPage.DISPLAY -> DisplaySettingsScreen(viewModel) { settingsPage = SettingsPage.ROOT }
        settingsPage == SettingsPage.DATA_BACKUP -> DataBackupSettingsScreen(viewModel) { settingsPage = SettingsPage.ROOT }
        settingsPage == SettingsPage.APPLICATION -> ApplicationSettingsScreen { settingsPage = SettingsPage.ROOT }
        settingsPage == SettingsPage.AI -> AiSettingsScreen { settingsPage = SettingsPage.ROOT }
        watchlistPage == WatchlistPage.LIST -> WatchlistScreen(
            viewModel = watchlistViewModel,
            cardLayoutStyle = viewModel.displaySettings.collectAsState().value.cardLayoutStyle,
            cardColors = viewModel.displaySettings.collectAsState().value.let { CardColors(it.cardBackgroundColor, it.titleTextColor, it.solarTextColor, it.lunarTextColor, it.countdownTextColor) },
            onBack = { watchlistPage = WatchlistPage.NONE; watchlistEditing = null },
            onManageCategories = { watchlistPage = WatchlistPage.CATEGORIES },
            selectedStatus = watchlistStatus,
            onStatusSelected = { watchlistStatus = it },
            onCreate = {
                watchlistStatus = it
                watchlistEditing = null
                watchlistPage = WatchlistPage.EDITOR
            },
            onEdit = { watchlistEditing = it; watchlistPage = WatchlistPage.EDITOR },
            feedback = watchlistFeedback,
            onFeedbackShown = { watchlistFeedback = null }
        )
        watchlistPage == WatchlistPage.EDITOR -> WatchRecordEditorScreen(
            viewModel = watchlistViewModel,
            record = watchlistEditing,
            requestedStatus = watchlistStatus,
            onBack = { watchlistPage = WatchlistPage.LIST; watchlistEditing = null },
            onSaved = { message ->
                watchlistFeedback = message
                watchlistEditing = null
                watchlistPage = WatchlistPage.LIST
            }
        )
        watchlistPage == WatchlistPage.CATEGORIES -> CategoryManagerScreen(watchlistViewModel) { watchlistPage = WatchlistPage.LIST }
        adding && addChoice == null -> AddChoiceScreen {
            if (it == AddChoice.WATCHLIST) {
                watchlistStatus = SYSTEM_WATCHING_ID
                watchlistEditing = null
                watchlistPage = WatchlistPage.EDITOR
                adding = false
            } else {
                addChoice = it
            }
        }
        addChoice != null -> EditScreen(
            existing = null,
            viewModel = viewModel,
            onRequestNotifications = onRequestNotifications,
            initialType = requireNotNull(addChoice?.recordType),
            onBack = { addChoice = null },
            onDone = {
                addChoice = null
                adding = false
                if (timeRecordType == null) tab = MainTab.HOME
            }
        )
        timeRecordType != null -> HomeScreen(
            viewModel = viewModel,
            recordType = requireNotNull(timeRecordType),
            onEdit = { editing = it },
            onAdd = { addChoice = if (timeRecordType == RecordType.BIRTHDAY) AddChoice.BIRTHDAY else AddChoice.ANNIVERSARY },
            onBack = { timeRecordType = null }
        )
        else -> {
            Scaffold(containerColor = Color.Transparent, bottomBar = { MainBottomBar(tab) { tab = it } }) { padding ->
                Box(Modifier.padding(padding)) {
                    when (tab) {
                        MainTab.HOME -> DashboardScreen(
                            viewModel = viewModel,
                            watchlistViewModel = watchlistViewModel,
                            aiHistoryRepository = aiHistoryRepository,
                            onOpenRecordType = { timeRecordType = it },
                            onOpenWatchlist = { tab = MainTab.RECORDS; watchlistPage = WatchlistPage.LIST },
                            onOpenAiTask = { task ->
                                aiLaunchTarget = AiLaunchTarget(AiMode.valueOf(task.mode), task.conversationId)
                                tab = MainTab.AI
                            },
                            onAdd = { adding = true }
                        )
                        MainTab.RECORDS -> RecordsHubScreen(
                            onOpenBirthdays = { timeRecordType = RecordType.BIRTHDAY },
                            onOpenAnniversaries = { timeRecordType = RecordType.ANNIVERSARY },
                            onOpenWatchlist = { watchlistPage = WatchlistPage.LIST },
                            onAdd = { adding = true }
                        )
                        MainTab.AI -> AiHomeScreen(
                            aiHistoryRepository,
                            onSettings = { settingsPage = SettingsPage.AI },
                            launchTarget = aiLaunchTarget,
                            onLaunchConsumed = { aiLaunchTarget = null }
                        )
                        MainTab.PROFILE -> ProfileScreen(
                            viewModel = viewModel,
                            onSettings = { settingsPage = SettingsPage.ROOT }
                        )
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChoiceScreen(onSelected: (AddChoice) -> Unit) {
    val choices = listOf(
        AddChoiceEntry(AddChoice.BIRTHDAY, "添加生日", "记录每年的生日提醒", Icons.Outlined.Cake),
        AddChoiceEntry(AddChoice.ANNIVERSARY, "添加纪念日", "记录重要的纪念日期", Icons.Outlined.Event),
        AddChoiceEntry(AddChoice.WATCHLIST, "添加追剧记录", "记录正在追的剧集进度", Icons.Outlined.Movie)
    )
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("新增记录") }, colors = glassTopAppBarColors()) }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            choices.forEach { entry ->
                GradientActionCard(title = entry.title, subtitle = entry.subtitle, icon = entry.icon, onClick = { onSelected(entry.choice) })
            }
        }
    }
}

@Composable
private fun MainBottomBar(selected: MainTab, onSelected: (MainTab) -> Unit) {
    val items = listOf(
        Triple(MainTab.HOME, Pair("首页", Icons.Outlined.Home), "首页"),
        Triple(MainTab.RECORDS, Pair("记录", Icons.AutoMirrored.Outlined.EventNote), "记录"),
        Triple(MainTab.AI, Pair("AI", Icons.Outlined.AutoAwesome), "AI"),
        Triple(MainTab.PROFILE, Pair("我的", Icons.Outlined.PersonOutline), "我的")
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp
    ) {
        items.forEach { (tab, item, description) ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                icon = {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (selected == tab) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Icon(
                            item.second,
                            description,
                            Modifier.padding(8.dp),
                            tint = if (selected == tab) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                label = { Text(item.first, maxLines = 1) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

internal fun navIcon(id: BottomNavIconId): ImageVector = when (id) {
    BottomNavIconId.CLOCK -> Icons.Outlined.AccessTime
    BottomNavIconId.CALENDAR_PLUS -> Icons.Outlined.AddCircleOutline
    BottomNavIconId.USER -> Icons.Outlined.PersonOutline
    BottomNavIconId.HEART -> Icons.Outlined.FavoriteBorder
    BottomNavIconId.STAR -> Icons.Outlined.StarBorder
    BottomNavIconId.SETTINGS -> Icons.Outlined.Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(viewModel: AppViewModel, onSettings: () -> Unit) {
    val records by viewModel.records.collectAsState()
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
            SectionLabel("个人工具")
            GlassPanel(modifier = Modifier.fillMaxWidth().clickable(onClick = onSettings)) {
            ListItem(
                headlineContent = { Text("设置") },
                supportingContent = { Text("日期、文字与应用设置") },
                leadingContent = { Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier
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
private fun DashboardScreen(
    viewModel: AppViewModel,
    watchlistViewModel: WatchlistViewModel,
    aiHistoryRepository: AiHistoryRepository,
    onOpenRecordType: (RecordType) -> Unit,
    onOpenWatchlist: () -> Unit,
    onOpenAiTask: (AiDashboardTask) -> Unit,
    onAdd: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val watchRecords by watchlistViewModel.records.collectAsState()
    val dashboardAiTasks by aiHistoryRepository.dashboardTasks.collectAsState(initial = emptyList())
    val now by viewModel.now.collectAsState()
    val reminderCandidates = records.mapNotNull { record ->
        CountdownCalculator.snapshot(record, now.atZone(ZoneId.systemDefault())).countdown
            ?.let { duration -> DashboardReminderCandidate(record.id, record.type, duration) }
    }
    val reminders = selectDashboardReminders(reminderCandidates).mapNotNull { candidate ->
        records.firstOrNull { it.id == candidate.recordId }?.let { it to candidate.duration }
    }
    val aiNotices = dashboardAiTasks.filter { dashboardNoticeState(it) != null }
    val watching = watchRecords.filter { it.status == SYSTEM_WATCHING_ID }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("首页") }, colors = glassTopAppBarColors()) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "新增记录") } }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (reminders.isEmpty()) {
                item {
                    DashboardFocusCard("暂无生日或纪念日", "点击添加重要日期", onAdd)
                }
            } else {
                items(reminders, key = { it.first.id }) { (record, duration) ->
                    DashboardFocusCard(
                        record.name,
                        "${if (record.type == RecordType.BIRTHDAY) "生日" else "纪念日"} · ${dashboardDurationLabel(duration)}",
                        { onOpenRecordType(record.type) }
                    )
                }
            }
            if (watching.isNotEmpty()) {
                item { SectionLabel("正在追剧") }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenWatchlist)) {
                        ListItem(
                            headlineContent = { Text(watchlistSummary(watching.size)) },
                            supportingContent = { Text(watching.take(2).joinToString("、") { it.title }) },
                            leadingContent = { Icon(Icons.Outlined.Movie, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                        )
                    }
                }
            }
            if (aiNotices.isNotEmpty()) {
                item { SectionLabel("AI 任务") }
                items(aiNotices, key = { it.messageId }) { task ->
                    val state = requireNotNull(dashboardNoticeState(task))
                    val imageMode = task.mode == AiMode.IMAGE.name
                    GlassPanel(modifier = Modifier.fillMaxWidth().clickable { onOpenAiTask(task) }) {
                        ListItem(
                            headlineContent = {
                                Text(when {
                                    imageMode && state == AiDashboardNoticeState.ACTIVE -> "AI 生图正在生成"
                                    imageMode -> "图片已生成"
                                    state == AiDashboardNoticeState.ACTIVE -> "AI 对话正在生成"
                                    else -> "对话已生成"
                                })
                            },
                            supportingContent = { Text(if (state == AiDashboardNoticeState.ACTIVE) "${task.title} · 点击查看进度" else "${task.title} · 点击查看结果") },
                            leadingContent = { Icon(if (imageMode) Icons.Outlined.Image else Icons.AutoMirrored.Outlined.Chat, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardFocusCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        contentColor = Color.White
    ) {
        Column(Modifier.background(GlassStyle.primaryBrush).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

private fun dashboardDurationLabel(duration: Duration): String {
    val days = duration.toDays()
    if (days > 0) return "还有 $days 天"
    val hours = duration.toHours()
    if (hours > 0) return "还有 $hours 小时"
    return "今天"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordsHubScreen(onOpenBirthdays: () -> Unit, onOpenAnniversaries: () -> Unit, onOpenWatchlist: () -> Unit, onAdd: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("记录") }, colors = glassTopAppBarColors()) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "新增记录") } }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientActionCard(title = "生日记录", subtitle = "查看生日与提醒", icon = Icons.Outlined.Cake, onClick = onOpenBirthdays)
            GradientActionCard(title = "纪念日记录", subtitle = "查看纪念日与提醒", icon = Icons.Outlined.Event, onClick = onOpenAnniversaries)
            GradientActionCard(title = "追剧记录", subtitle = "更新进度、状态与归档", icon = Icons.Outlined.Movie, onClick = onOpenWatchlist)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    recordType: RecordType,
    onEdit: (CountdownEntity) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    val records by viewModel.records.collectAsState()
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
        record.type == recordType && record.name.contains(query.trim(), ignoreCase = true) && when (filter) {
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
                title = { Text(if (recordType == RecordType.BIRTHDAY) "生日记录" else "纪念日记录") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "新增记录") } }
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
            if (visibleRecords.isEmpty()) item {
                val typeRecordsEmpty = localRecords.none { it.type == recordType }
                Text(if (typeRecordsEmpty) "还没有${if (recordType == RecordType.BIRTHDAY) "生日" else "纪念日"}记录。" else "没有匹配的记录。", style = MaterialTheme.typography.bodyLarge)
            }
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
@OptIn(ExperimentalLayoutApi::class)
private fun CountdownCard(record: CountdownEntity, now: Instant, format: DateFormatPreference, settings: AppDisplaySettings, dragging: Boolean, onClick: () -> Unit, onDelete: () -> Unit, onPin: () -> Unit, modifier: Modifier = Modifier) {
    val solarDateTime = CountdownCalculator.solarDateTime(record)
    val snapshot = CountdownCalculator.snapshot(record, now.atZone(ZoneId.systemDefault()))
    val dateStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = settings.dateTextSize.sp, fontWeight = if (settings.dateBold) FontWeight.Bold else FontWeight.Normal)
    val countdownStyle = MaterialTheme.typography.titleMedium.copy(fontSize = settings.countdownTextSize.sp, fontWeight = if (settings.countdownBold) FontWeight.Bold else FontWeight.Normal)
    val isBirthday = record.type == RecordType.BIRTHDAY
    val typeLabel = if (isBirthday) "生日" else "纪念日"
    val accent = if (isBirthday) Color(0xFFE96955) else Color(0xFFC58A32)
    val colors = effectiveCardColors(record, CardColors(settings.cardBackgroundColor, settings.titleTextColor, settings.solarTextColor, settings.lunarTextColor, settings.countdownTextColor))
    val cardColor = Color(colors.background)
    val titleColor = Color(colors.title)
    val dateColor = Color(colors.solar)
    val countdownColor = Color(colors.countdown)
    val primaryValue = snapshot.countdown?.let {
        "还有\n${DisplayFormatter.countdown(it, maskOptions(record.countdownDisplayMask, settings))}"
    } ?: snapshot.elapsed?.let {
        "已经\n${DisplayFormatter.elapsed(it, snapshot.elapsedRemainder ?: Duration.ZERO, maskOptions(record.countdownDisplayMask, settings))}"
    }.orEmpty()
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        border = BorderStroke(if (dragging) 2.dp else 1.dp, if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints {
            val stacked = shouldStackInformationCard(maxWidth.value.toInt(), LocalDensity.current.fontScale)
            Column(Modifier.fillMaxWidth().background(cardColor, MaterialTheme.shapes.medium).padding(if (settings.cardLayoutStyle == CardLayoutStyle.COMPACT) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (settings.cardLayoutStyle == CardLayoutStyle.COMPACT) 8.dp else 12.dp)) {
            if (settings.cardLayoutStyle == CardLayoutStyle.SIDEBAR) {
                Box(Modifier.fillMaxWidth().height(6.dp).background(accent, MaterialTheme.shapes.small))
            }
            if (dragging) Text("正在调整顺序", style = MaterialTheme.typography.labelMedium, color = titleColor)
            if (settings.cardLayoutStyle == CardLayoutStyle.COMPACT) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    InformationCardHeader(
                        modifier = Modifier.weight(1f),
                        title = record.name,
                        subtitle = DateFormatter.format(solarDateTime, format, maskOptions(record.solarDisplayMask, settings)),
                        subtitleColor = dateColor,
                        value = primaryValue,
                        valueColor = countdownColor,
                        titleStyle = MaterialTheme.typography.titleMedium.copy(fontSize = settings.titleTextSize.sp, fontWeight = if (settings.titleBold) FontWeight.Bold else FontWeight.Normal, color = titleColor),
                        valueStyle = countdownStyle.copy(color = countdownColor),
                        stacked = false
                    )
                }
            } else {
            InformationCardHeader(
                title = record.name,
                subtitle = DateFormatter.format(solarDateTime, format, maskOptions(record.solarDisplayMask, settings)),
                subtitleColor = dateColor,
                value = primaryValue,
                valueColor = countdownColor,
                titleStyle = MaterialTheme.typography.titleMedium.copy(fontSize = settings.titleTextSize.sp, fontWeight = if (settings.titleBold) FontWeight.Bold else FontWeight.Normal, color = titleColor),
                valueStyle = countdownStyle.copy(color = countdownColor),
                stacked = stacked
            )
            }
            HorizontalDivider(color = titleColor.copy(alpha = 0.22f))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (record.calendarType == CalendarType.LUNAR && record.lunarYear != null && record.lunarMonth != null && record.lunarDay != null) {
                    val validLeap = record.lunarLeapMonth && runCatching { LunarCalendarConverter.leapMonthForYear(record.lunarYear) == record.lunarMonth }.getOrDefault(false)
                    if (record.showLunarDate && settings.showLunarDate) Text("农历 ${DateFormatter.formatLunar(LunarDate(record.lunarYear, record.lunarMonth, record.lunarDay, validLeap), solarDateTime.toLocalTime(), maskOptions(record.lunarDisplayMask, settings))}", style = dateStyle, color = Color(colors.lunar))
                    if (record.showSolarDate && settings.showSolarDate) Text("阳历 ${DateFormatter.format(solarDateTime, format, maskOptions(record.solarDisplayMask, settings))}", style = dateStyle, color = dateColor)
                } else {
                    if (record.showSolarDate && settings.showSolarDate) Text("阳历 ${DateFormatter.format(solarDateTime, format, maskOptions(record.solarDisplayMask, settings))}", style = dateStyle, color = dateColor)
                    val lunar = runCatching { LunarCalendarConverter.toLunar(solarDateTime.toLocalDate()) }.getOrNull()
                    if (record.showLunarDate && settings.showLunarDate && lunar != null) Text("农历 ${DateFormatter.formatLunar(lunar, solarDateTime.toLocalTime(), maskOptions(record.lunarDisplayMask, settings))}", style = dateStyle, color = Color(colors.lunar))
                }
                snapshot.elapsed?.let {
                    Text("下一个周年还有 ${DisplayFormatter.countdown(snapshot.nextAnniversary ?: Duration.ZERO, maskOptions(record.countdownDisplayMask, settings))}", style = dateStyle, color = countdownColor)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusLabel(typeLabel, tone = if (isBirthday) TaskTone.ERROR else TaskTone.WARNING)
                    if (record.isPinned) StatusLabel("已置顶", tone = TaskTone.INFO)
                    if (record.reminderEnabled) StatusLabel("已提醒", tone = TaskTone.SUCCESS)
                }
                var menuOpen by remember(record.id) { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "更多操作") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(if (record.isPinned) "取消置顶" else "置顶") }, onClick = { menuOpen = false; onPin() }, leadingIcon = { Icon(if (record.isPinned) Icons.Outlined.Star else Icons.Outlined.StarBorder, null) })
                        DropdownMenuItem(text = { Text("删除", color = MaterialTheme.colorScheme.error) }, onClick = { menuOpen = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun StyledText(text: String, style: androidx.compose.ui.text.TextStyle, solidColor: Int, gradientId: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    val gradient = CardGradients.find(gradientId)
    Text(text, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis, style = if (gradient.colors.size > 1) style.copy(brush = gradient.brushOrNull()) else style.copy(color = Color(solidColor)))
}

private fun maskOptions(mask: Int, settings: AppDisplaySettings) = DisplayOptions(
    mask and 1 != 0 && settings.showYears,
    mask and 2 != 0 && settings.showMonths,
    mask and 4 != 0 && settings.showDays,
    mask and 8 != 0 && settings.showHours,
    mask and 16 != 0 && settings.showMinutes,
    mask and 32 != 0 && settings.showSeconds
)
