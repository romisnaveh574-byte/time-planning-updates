package com.example.birthdaycountdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birthdaycountdown.data.CalendarType
import com.example.birthdaycountdown.data.CountdownEntity
import com.example.birthdaycountdown.data.RecordType
import com.example.birthdaycountdown.domain.CardGradients
import com.example.birthdaycountdown.domain.CountdownCalculator
import com.example.birthdaycountdown.domain.DateFormatPreference
import com.example.birthdaycountdown.domain.DateFormatter
import com.example.birthdaycountdown.domain.DisplayFormatter
import com.example.birthdaycountdown.domain.DisplayOptions
import com.example.birthdaycountdown.domain.LunarCalendarConverter
import com.example.birthdaycountdown.domain.LunarDate
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlin.math.abs

internal enum class AddChoice(val label: String, val recordType: RecordType?) {
    BIRTHDAY("生日", RecordType.BIRTHDAY),
    ANNIVERSARY("纪念日", RecordType.ANNIVERSARY),
    WATCHLIST("追剧记录", null)
}

internal data class AddChoiceOption(
    val choice: AddChoice,
    val label: String,
    val supportingText: String,
    val icon: ImageVector
)

internal fun addChoiceOptions(): List<AddChoiceOption> = AddChoice.entries.map { choice ->
    AddChoiceOption(
        choice = choice,
        label = choice.label,
        supportingText = when (choice) {
            AddChoice.BIRTHDAY -> "记录下一次生日倒计时"
            AddChoice.ANNIVERSARY -> "记录纪念日和周年提醒"
            AddChoice.WATCHLIST -> "进入追剧记录"
        },
        icon = when (choice) {
            AddChoice.BIRTHDAY -> Icons.Outlined.Cake
            AddChoice.ANNIVERSARY -> Icons.Outlined.Event
            AddChoice.WATCHLIST -> Icons.Outlined.Movie
        }
    )
}

internal fun watchlistSummary(count: Int): String = "正在追 $count 部"

internal fun <T> pinnedFirstStableOrder(
    items: List<T>,
    isPinned: (T) -> Boolean
): List<T> = items.filter(isPinned) + items.filterNot(isPinned)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    viewModel: AppViewModel,
    watchlistViewModel: WatchlistViewModel,
    onEdit: (CountdownEntity) -> Unit,
    onAdd: () -> Unit,
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val current = now.atZone(ZoneId.systemDefault())

    LaunchedEffect(records, draggingId) {
        if (draggingId == null) {
            localRecords.clear()
            localRecords.addAll(records)
        }
    }

    val visibleRecords = pinnedFirstStableOrder(
        localRecords.filter { record ->
            record.name.contains(query.trim(), ignoreCase = true)
        }
    ) { it.isPinned }
    val nearestRecord = records.minByOrNull { CountdownCalculator.snapshot(it, current).target.toInstant() }
    val birthdayCount = records.count { it.type == RecordType.BIRTHDAY }
    val anniversaryCount = records.count { it.type == RecordType.ANNIVERSARY }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "时间",
                actions = {
                    IconButton(
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) query = ""
                        }
                    ) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Outlined.Close else Icons.Outlined.Search,
                            contentDescription = if (searchVisible) "关闭搜索" else "搜索"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "添加记录")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = AppUiTokens.pageHorizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.contentSpacing)
        ) {
            if (searchVisible) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("搜索记录") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item { OverviewPanel(record = nearestRecord, now = now, format = format) }
            item {
                QuickEntryRow(
                    birthdayCount = birthdayCount,
                    anniversaryCount = anniversaryCount,
                    watchlistCount = watchRecords.size,
                    onWatchlist = onWatchlist
                )
            }
            item {
                SectionHeader(
                    title = "接下来",
                    supportingText = "${visibleRecords.size} 项"
                )
            }
            if (visibleRecords.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = if (records.isEmpty()) "还没有时间记录" else "没有匹配的记录",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(visibleRecords, key = CountdownEntity::id) { record ->
                    CountdownRecordItem(
                        record = record,
                        now = now,
                        format = format,
                        settings = settings,
                        draggingId = draggingId,
                        visibleIds = visibleRecords
                            .filter { it.isPinned == record.isPinned }
                            .map(CountdownEntity::id),
                        onEdit = onEdit,
                        onDelete = { deleting = it },
                        onPin = { viewModel.setPinned(record, !record.isPinned) },
                        onReorderCommitted = viewModel::reorder,
                        localRecords = localRecords,
                        setDraggingId = { draggingId = it }
                    )
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
                TextButton(
                    onClick = {
                        viewModel.delete(record)
                        deleting = null
                        scope.launch {
                            if (
                                snackbarHostState.showSnackbar(
                                    message = "已删除 ${record.name}",
                                    actionLabel = "撤销",
                                    withDismissAction = true
                                ) == SnackbarResult.ActionPerformed
                            ) {
                                viewModel.restore(record)
                            }
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun OverviewPanel(record: CountdownEntity?, now: Instant, format: DateFormatPreference) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("概览", style = MaterialTheme.typography.titleMedium)
            if (record == null) {
                Text("还没有时间记录", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "生日和纪念日会显示在这里",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val snapshot = CountdownCalculator.snapshot(record, now.atZone(ZoneId.systemDefault()))
                StatusLabel(text = record.typeLabel(), tone = StatusTone.INFO)
                Text(record.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = DateFormatter.format(CountdownCalculator.solarDateTime(record), format),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                snapshot.countdown?.let {
                    Text(
                        text = "还有 ${DisplayFormatter.countdown(it, DisplayOptions())}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                snapshot.elapsed?.let {
                    Text(
                        text = "已经 ${DisplayFormatter.elapsed(it, snapshot.elapsedRemainder ?: Duration.ZERO, DisplayOptions())}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "下一个周年还有 ${DisplayFormatter.countdown(snapshot.nextAnniversary ?: Duration.ZERO, DisplayOptions())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickEntryRow(
    birthdayCount: Int,
    anniversaryCount: Int,
    watchlistCount: Int,
    onWatchlist: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppUiTokens.contentSpacing)
    ) {
        SummaryTile(
            modifier = Modifier.weight(1f),
            title = "生日",
            value = birthdayCount.toString()
        )
        SummaryTile(
            modifier = Modifier.weight(1f),
            title = "纪念日",
            value = anniversaryCount.toString()
        )
        SummaryTile(
            modifier = Modifier.weight(1f),
            title = "追剧",
            value = watchlistCount.toString(),
            onClick = onWatchlist,
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddChoiceScreen(onSelected: (AddChoice) -> Unit) {
    val choices = addChoiceOptions()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "添加记录") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = AppUiTokens.pageHorizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.contentSpacing)
        ) {
            items(choices, key = { it.choice.name }) { option ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(option.choice) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    ListItem(
                        headlineContent = { Text(option.label) },
                        supportingContent = { Text(option.supportingText) },
                        leadingContent = {
                            Icon(option.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileScreen(
    viewModel: AppViewModel,
    watchlistViewModel: WatchlistViewModel,
    onSettings: () -> Unit,
    onWatchlist: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val watchRecords by watchlistViewModel.records.collectAsState()
    val birthdayCount = records.count { it.type == RecordType.BIRTHDAY }
    val anniversaryCount = records.count { it.type == RecordType.ANNIVERSARY }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "我的") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = AppUiTokens.pageHorizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.contentSpacing)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader(
                            title = "记录概览",
                            supportingText = "${records.size} 条时间记录"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryMetric(modifier = Modifier.weight(1f), label = "生日", value = birthdayCount)
                            SummaryMetric(modifier = Modifier.weight(1f), label = "纪念日", value = anniversaryCount)
                            SummaryMetric(modifier = Modifier.weight(1f), label = "追剧", value = watchRecords.size)
                        }
                    }
                }
            }
            item {
                NavigationRow(
                    title = "追剧记录",
                    supportingText = watchlistSummary(watchRecords.size),
                    icon = Icons.Outlined.Movie,
                    onClick = onWatchlist
                )
            }
            item {
                NavigationRow(
                    title = "设置",
                    supportingText = "日期、显示与数据",
                    icon = Icons.Outlined.Settings,
                    onClick = onSettings
                )
            }
        }
    }
}

@Composable
private fun SummaryTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .heightIn(min = 84.dp)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                trailing?.invoke()
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NavigationRow(
    title: String,
    supportingText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(supportingText) },
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        )
    }
}

@Composable
internal fun CountdownRecordItem(
    record: CountdownEntity,
    now: Instant,
    format: DateFormatPreference,
    settings: AppDisplaySettings,
    draggingId: Long?,
    visibleIds: List<Long>,
    onEdit: (CountdownEntity) -> Unit,
    onDelete: (CountdownEntity) -> Unit,
    onPin: () -> Unit,
    onReorderCommitted: (List<CountdownEntity>) -> Unit,
    localRecords: MutableList<CountdownEntity>,
    setDraggingId: (Long?) -> Unit
) {
    var dragDistance by remember(record.id) { mutableFloatStateOf(0f) }
    val currentVisibleIds by rememberUpdatedState(visibleIds)
    val dragging = draggingId == record.id

    CountdownCard(
        record = record,
        now = now,
        format = format,
        settings = settings,
        dragging = dragging,
        onClick = { if (!dragging) onEdit(record) },
        onDelete = { onDelete(record) },
        onPin = onPin,
        modifier = Modifier
            .shadow(if (dragging) 8.dp else 0.dp, MaterialTheme.shapes.medium)
            .alpha(if (dragging) 0.92f else 1f)
            .pointerInput(record.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        setDraggingId(record.id)
                        dragDistance = 0f
                    },
                    onDragCancel = {
                        setDraggingId(null)
                        dragDistance = 0f
                    },
                    onDragEnd = {
                        onReorderCommitted(localRecords.toList())
                        setDraggingId(null)
                        dragDistance = 0f
                    }
                ) { change, amount ->
                    change.consume()
                    dragDistance += amount.y
                    if (abs(dragDistance) >= 72f) {
                        val reordered = moveVisibleItem(
                            records = localRecords.toList(),
                            visibleIds = currentVisibleIds,
                            movingId = record.id,
                            direction = if (dragDistance > 0) 1 else -1,
                            idOf = CountdownEntity::id
                        )
                        localRecords.clear()
                        localRecords.addAll(reordered)
                        dragDistance = 0f
                    }
                }
            }
    )
}

@Composable
private fun CountdownCard(
    record: CountdownEntity,
    now: Instant,
    format: DateFormatPreference,
    settings: AppDisplaySettings,
    dragging: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val solarDateTime = CountdownCalculator.solarDateTime(record)
    val snapshot = CountdownCalculator.snapshot(record, now.atZone(ZoneId.systemDefault()))
    val dateStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = settings.dateTextSize.sp,
        fontWeight = if (settings.dateBold) FontWeight.Bold else FontWeight.Normal
    )
    val countdownStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = settings.countdownTextSize.sp,
        fontWeight = if (settings.countdownBold) FontWeight.Bold else FontWeight.Normal
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    CardGradients.find(record.cardGradientId)
                        .brushOrSolid(record.cardBackgroundColor),
                    MaterialTheme.shapes.large
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (dragging) {
                Text(
                    text = "正在调整顺序",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyledText(
                    text = record.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = settings.titleTextSize.sp,
                        fontWeight = if (settings.titleBold) FontWeight.Bold else FontWeight.Normal
                    ),
                    solidColor = record.titleTextColor,
                    gradientId = record.titleGradientId,
                    modifier = Modifier.weight(1f)
                )
                if (record.isPinned) {
                    StatusLabel(
                        text = "置顶",
                        tone = StatusTone.WARNING,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                var menuOpen by remember(record.id) { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "更多操作")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (record.isPinned) "取消置顶" else "置顶") },
                        onClick = {
                            menuOpen = false
                            onPin()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (record.isPinned) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
            if (
                record.calendarType == CalendarType.LUNAR &&
                record.lunarYear != null &&
                record.lunarMonth != null &&
                record.lunarDay != null
            ) {
                val validLeap = record.lunarLeapMonth &&
                    runCatching { LunarCalendarConverter.leapMonthForYear(record.lunarYear) == record.lunarMonth }
                        .getOrDefault(false)
                if (record.showLunarDate && settings.showLunarDate) {
                    StyledText(
                        text = "农历 ${
                            DateFormatter.formatLunar(
                                LunarDate(record.lunarYear, record.lunarMonth, record.lunarDay, validLeap),
                                solarDateTime.toLocalTime(),
                                maskOptions(record.lunarDisplayMask, settings)
                            )
                        }",
                        style = dateStyle,
                        solidColor = record.lunarTextColor,
                        gradientId = record.lunarGradientId
                    )
                }
                if (record.showSolarDate && settings.showSolarDate) {
                    StyledText(
                        text = "阳历 ${DateFormatter.format(solarDateTime, format, maskOptions(record.solarDisplayMask, settings))}",
                        style = dateStyle,
                        solidColor = record.solarTextColor,
                        gradientId = record.solarGradientId
                    )
                }
            } else {
                if (record.showSolarDate && settings.showSolarDate) {
                    StyledText(
                        text = "阳历 ${DateFormatter.format(solarDateTime, format, maskOptions(record.solarDisplayMask, settings))}",
                        style = dateStyle,
                        solidColor = record.solarTextColor,
                        gradientId = record.solarGradientId
                    )
                }
                val lunar = runCatching { LunarCalendarConverter.toLunar(solarDateTime.toLocalDate()) }.getOrNull()
                if (record.showLunarDate && settings.showLunarDate && lunar != null) {
                    StyledText(
                        text = "农历 ${DateFormatter.formatLunar(lunar, solarDateTime.toLocalTime(), maskOptions(record.lunarDisplayMask, settings))}",
                        style = dateStyle,
                        solidColor = record.lunarTextColor,
                        gradientId = record.lunarGradientId
                    )
                }
            }
            snapshot.countdown?.let {
                StyledText(
                    text = "还有 ${DisplayFormatter.countdown(it, maskOptions(record.countdownDisplayMask, settings))}",
                    style = countdownStyle,
                    solidColor = record.countdownTextColor,
                    gradientId = record.countdownGradientId
                )
            }
            snapshot.elapsed?.let {
                StyledText(
                    text = "已经 ${DisplayFormatter.elapsed(it, snapshot.elapsedRemainder ?: Duration.ZERO, maskOptions(record.countdownDisplayMask, settings))}",
                    style = countdownStyle,
                    solidColor = record.countdownTextColor,
                    gradientId = record.countdownGradientId
                )
                StyledText(
                    text = "下一个周年还有 ${DisplayFormatter.countdown(snapshot.nextAnniversary ?: Duration.ZERO, maskOptions(record.countdownDisplayMask, settings))}",
                    style = dateStyle,
                    solidColor = record.countdownTextColor,
                    gradientId = record.countdownGradientId
                )
            }
        }
    }
}

@Composable
private fun StyledText(
    text: String,
    style: TextStyle,
    solidColor: Int,
    gradientId: String,
    modifier: Modifier = Modifier
) {
    val gradient = CardGradients.find(gradientId)
    Text(
        text = text,
        modifier = modifier,
        style = if (gradient.colors.size > 1) {
            style.copy(brush = gradient.brushOrNull())
        } else {
            style.copy(color = Color(solidColor))
        }
    )
}

private fun maskOptions(mask: Int, settings: AppDisplaySettings) = DisplayOptions(
    showYears = mask and 1 != 0 && settings.showYears,
    showMonths = mask and 2 != 0 && settings.showMonths,
    showDays = mask and 4 != 0 && settings.showDays,
    showHours = mask and 8 != 0 && settings.showHours,
    showMinutes = mask and 16 != 0 && settings.showMinutes,
    showSeconds = mask and 32 != 0 && settings.showSeconds
)

private fun CountdownEntity.typeLabel(): String = when (type) {
    RecordType.BIRTHDAY -> "生日"
    RecordType.ANNIVERSARY -> "纪念日"
}
