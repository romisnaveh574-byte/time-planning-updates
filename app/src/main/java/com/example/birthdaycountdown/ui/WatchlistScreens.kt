package com.example.birthdaycountdown.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.birthdaycountdown.data.WatchCategoryEntity
import com.example.birthdaycountdown.data.WatchRecordEntity
import com.example.birthdaycountdown.data.WatchStatusEntity
import com.example.birthdaycountdown.data.SYSTEM_WATCHING_ID
import com.example.birthdaycountdown.domain.matchesWatchStatus
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    cardLayoutStyle: CardLayoutStyle = CardLayoutStyle.STANDARD,
    cardColors: CardColors = CardColors(0xFF6D4BC3.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    onCreate: (String) -> Unit,
    onEdit: (WatchRecordEntity) -> Unit,
    feedback: String?,
    onFeedbackShown: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val records by viewModel.records.collectAsState()
    val statuses by viewModel.watchStatuses.collectAsState()
    val localRecords = remember { mutableStateListOf<WatchRecordEntity>() }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<WatchRecordEntity?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(records, draggingId) {
        if (draggingId == null) {
            localRecords.clear()
            localRecords.addAll(records)
        }
    }

    val visibleRecords = localRecords.filter {
        matchesWatchStatus(it.status, selectedStatus) && (selectedCategoryId == null || it.categoryId == selectedCategoryId)
    }
    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            onFeedbackShown()
        }
    }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("追剧记录") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = onManageCategories) { Icon(Icons.Outlined.Category, "管理分类") }
                },
                colors = glassTopAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { androidx.compose.material3.FloatingActionButton(onClick = { onCreate(selectedStatus) }) { Icon(Icons.Default.Add, "添加记录") } }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("追剧概览", style = MaterialTheme.typography.titleSmall)
                            Text("正在追 ${records.count { it.status == SYSTEM_WATCHING_ID }} 部", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Outlined.Movie, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                StatusFilterRow(statuses, selectedStatus, onStatusSelected)
            }
            item {
                CategoryFilterRow(categories, selectedCategoryId) { selectedCategoryId = it }
            }
            if (visibleRecords.isNotEmpty()) {
                item { Text("长按并上下拖动记录可调整顺序", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (visibleRecords.isEmpty()) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.Movie, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(if (records.isEmpty()) "还没有追剧记录" else "当前筛选暂无记录", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = { onCreate(selectedStatus) }, enabled = categories.isNotEmpty()) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(6.dp))
                                Text("添加记录")
                            }
                        }
                    }
                }
            } else {
                items(visibleRecords, key = { it.id }) { record ->
                    val currentVisibleIds by rememberUpdatedState(visibleRecords.map { it.id })
                    WatchRecordCard(
                        record = record,
                        cardLayoutStyle = cardLayoutStyle,
                        cardColors = cardColors,
                        categoryName = categories.firstOrNull { it.id == record.categoryId }?.name.orEmpty(),
                        statusName = statuses.firstOrNull { it.id == record.status }?.name ?: record.status,
                        dragging = draggingId == record.id,
                        onEdit = { onEdit(record) },
                        onDecrease = {
                            viewModel.adjustEpisode(record, -1)
                            scope.launch { snackbarHostState.showSnackbar("已更新至第 ${record.currentEpisode - 1} 集") }
                        },
                        onIncrease = {
                            viewModel.adjustEpisode(record, 1)
                            scope.launch { snackbarHostState.showSnackbar("已更新至第 ${record.currentEpisode + 1} 集") }
                        },
                        onDelete = { deleting = record },
                        modifier = Modifier.pointerInput(record.id, selectedCategoryId) {
                            var dragDistance = 0f
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingId = record.id; dragDistance = 0f },
                                onDragCancel = { draggingId = null; dragDistance = 0f },
                                onDragEnd = {
                                    viewModel.reorderRecords(localRecords.filter {
                                        matchesWatchStatus(it.status, selectedStatus) &&
                                            (selectedCategoryId == null || it.categoryId == selectedCategoryId)
                                    })
                                    draggingId = null
                                    dragDistance = 0f
                                }
                            ) { change, amount ->
                                change.consume()
                                dragDistance += amount.y
                                if (abs(dragDistance) >= 72f) {
                                    val reordered = moveVisibleItem(localRecords.toList(), currentVisibleIds, record.id, if (dragDistance > 0) 1 else -1) { it.id }
                                    localRecords.clear()
                                    localRecords.addAll(reordered)
                                    dragDistance = 0f
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除追剧记录？") },
            text = { Text(record.title) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(record) {
                        scope.launch {
                            if (snackbarHostState.showSnackbar("已删除 ${record.title}", "撤销", withDismissAction = true) == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                viewModel.saveRecord(record)
                            }
                        }
                    }
                    deleting = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun StatusFilterRow(statuses: List<WatchStatusEntity>, selectedStatus: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statuses.forEach { status ->
            FilterChip(selected = selectedStatus == status.id, onClick = { onSelect(status.id) }, label = { Text(status.name) })
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<WatchCategoryEntity>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = selectedCategoryId == null, onClick = { onSelect(null) }, label = { Text("全部") })
        categories.forEach { category ->
            FilterChip(selected = selectedCategoryId == category.id, onClick = { onSelect(category.id) }, label = { Text(category.name) })
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun WatchRecordCard(
    record: WatchRecordEntity,
    cardLayoutStyle: CardLayoutStyle,
    cardColors: CardColors,
    categoryName: String,
    statusName: String,
    dragging: Boolean,
    onEdit: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember(record.id) { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (dragging) 8.dp else 0.dp, MaterialTheme.shapes.medium)
            .alpha(if (dragging) 0.9f else 1f),
        border = BorderStroke(if (dragging) 2.dp else 1.dp, if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints {
        val stacked = shouldStackInformationCard(maxWidth.value.toInt(), LocalDensity.current.fontScale)
        Column(Modifier.fillMaxWidth().background(Color(cardColors.background), MaterialTheme.shapes.medium).padding(if (cardLayoutStyle == CardLayoutStyle.COMPACT) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (cardLayoutStyle == CardLayoutStyle.COMPACT) 8.dp else 12.dp)) {
            if (cardLayoutStyle == CardLayoutStyle.SIDEBAR) Box(Modifier.fillMaxWidth().height(6.dp).background(Color(cardColors.countdown), MaterialTheme.shapes.small))
            if (dragging) Text("正在调整顺序", style = MaterialTheme.typography.labelMedium, color = Color(cardColors.title))
            InformationCardHeader(
                title = record.title,
                subtitle = listOf(categoryName, record.platform).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "未设置分类信息" },
                subtitleColor = Color(cardColors.solar),
                value = "第 ${record.currentEpisode}${record.totalEpisodes?.let { " / $it" }.orEmpty()} 集\n观看进度",
                valueColor = Color(cardColors.countdown),
                titleStyle = MaterialTheme.typography.titleMedium.copy(color = Color(cardColors.title)),
                valueStyle = MaterialTheme.typography.titleMedium.copy(color = Color(cardColors.countdown)),
                stacked = cardLayoutStyle != CardLayoutStyle.COMPACT && stacked
            )
            record.totalEpisodes?.takeIf { it > 0 }?.let { total ->
                LinearProgressIndicator(
                    progress = { (record.currentEpisode.toFloat() / total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.22f))
            if (stacked) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusLabel(statusName, tone = if (record.status == "WATCHING") TaskTone.PROGRESS else TaskTone.INFO)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        WatchRecordActions(record, menuOpen, { menuOpen = it }, onDecrease, onIncrease, onEdit, onDelete)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusLabel(statusName, tone = if (record.status == "WATCHING") TaskTone.PROGRESS else TaskTone.INFO)
                }
                WatchRecordActions(record, menuOpen, { menuOpen = it }, onDecrease, onIncrease, onEdit, onDelete)
                }
            }
        }
        }
    }
}

@Composable
private fun WatchRecordActions(
    record: WatchRecordEntity,
    menuOpen: Boolean,
    setMenuOpen: (Boolean) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    IconButton(onClick = onDecrease, enabled = record.currentEpisode > 0) { Icon(Icons.Default.Remove, "减少集数") }
    IconButton(onClick = onIncrease, enabled = record.totalEpisodes == null || record.currentEpisode < record.totalEpisodes) { Icon(Icons.Default.Add, "增加集数") }
    Icon(Icons.Outlined.DragHandle, "长按调整顺序", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    androidx.compose.foundation.layout.Box {
        IconButton(onClick = { setMenuOpen(true) }) { Icon(Icons.Default.MoreVert, "更多操作") }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { setMenuOpen(false) }) {
            DropdownMenuItem(text = { Text("编辑") }, onClick = { setMenuOpen(false); onEdit() }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                onClick = { setMenuOpen(false); onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WatchRecordEditorScreen(
    viewModel: WatchlistViewModel,
    record: WatchRecordEntity?,
    requestedStatus: String,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    var title by remember(record?.id) { mutableStateOf(record?.title.orEmpty()) }
    var selectedCategoryId by remember(record?.id, categories) { mutableLongStateOf(record?.categoryId ?: categories.firstOrNull()?.id ?: 0L) }
    var episode by remember(record?.id) { mutableStateOf(record?.currentEpisode?.toString() ?: "0") }
    var totalEpisodes by remember(record?.id) { mutableStateOf(record?.totalEpisodes?.toString().orEmpty()) }
    var platform by remember(record?.id) { mutableStateOf(record?.platform.orEmpty()) }
    val statuses by viewModel.watchStatuses.collectAsState()
    var status by remember(record?.id, requestedStatus) { mutableStateOf(record?.status ?: requestedStatus) }
    val normalizedTitle = title.trim()
    val parsedEpisode = episode.toIntOrNull() ?: 0
    val parsedTotalEpisodes = totalEpisodes.toIntOrNull()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (record == null) "添加追剧记录" else "编辑追剧记录") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = glassTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                if (categories.isEmpty()) {
                    Text("分类正在加载，请稍候", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(title, { title = it }, label = { Text("剧名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("分类", style = MaterialTheme.typography.labelLarge)
                categories.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { category ->
                            FilterChip(
                                selected = selectedCategoryId == category.id,
                                onClick = { selectedCategoryId = category.id },
                                label = { Text(category.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                OutlinedTextField(
                    value = episode,
                    onValueChange = { episode = it.filter(Char::isDigit).take(6) },
                    label = { Text("当前集数") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { episode = (parsedEpisode - 1).coerceAtLeast(0).toString() }, enabled = parsedEpisode > 0) { Icon(Icons.Default.Remove, "减少集数") }
                            IconButton(onClick = { episode = (parsedEpisode + 1).toString() }) { Icon(Icons.Default.Add, "增加集数") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalEpisodes,
                    onValueChange = { totalEpisodes = it.filter(Char::isDigit).take(6) },
                    label = { Text("总集数（可选）") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(platform, { platform = it }, label = { Text("观看平台（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("状态", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statuses.forEach { option ->
                        FilterChip(status == option.id, { status = option.id }, label = { Text(option.name) })
                    }
                }
                if (parsedTotalEpisodes != null && parsedTotalEpisodes < parsedEpisode) {
                    Text("总集数不能小于当前集数", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                onClick = {
                    viewModel.saveRecord(WatchRecordEntity(
                        id = record?.id ?: 0L,
                        title = normalizedTitle,
                        categoryId = selectedCategoryId,
                        currentEpisode = parsedEpisode,
                        totalEpisodes = parsedTotalEpisodes,
                        platform = platform.trim(),
                        status = status,
                        lastWatchedAt = record?.lastWatchedAt ?: System.currentTimeMillis(),
                        sortOrder = record?.sortOrder ?: Int.MAX_VALUE
                    ))
                    onSaved(if (record == null) "已添加追剧记录" else "已保存追剧记录")
                },
                enabled = normalizedTitle.isNotEmpty() && selectedCategoryId > 0 && (parsedTotalEpisodes == null || parsedTotalEpisodes >= parsedEpisode),
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(viewModel: WatchlistViewModel, onBack: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val records by viewModel.records.collectAsState()
    val statuses by viewModel.watchStatuses.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WatchCategoryEntity?>(null) }
    var deleting by remember { mutableStateOf<WatchCategoryEntity?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var creatingStatus by remember { mutableStateOf(false) }
    var editingStatus by remember { mutableStateOf<WatchStatusEntity?>(null) }
    var deletingStatus by remember { mutableStateOf<WatchStatusEntity?>(null) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { if (tab == 0) creating = true else creatingStatus = true }) { Icon(Icons.Default.Add, "添加") } },
                colors = glassTopAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("内容分类") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("观看状态") })
                }
            }
            if (tab == 0) items(categories, key = { it.id }) { category ->
                val index = categories.indexOf(category)
                val recordCount = records.count { it.categoryId == category.id }
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                            Text("$recordCount 部记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { reorderCategory(categories, index, index - 1, viewModel) }, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, "上移") }
                        IconButton(onClick = { reorderCategory(categories, index, index + 1, viewModel) }, enabled = index < categories.lastIndex) { Icon(Icons.Default.ArrowDownward, "下移") }
                        IconButton(onClick = { editing = category }) { Icon(Icons.Outlined.Edit, "编辑") }
                        IconButton(onClick = { deleting = category }, enabled = categories.size > 1) { Icon(Icons.Default.Delete, "删除") }
                    }
                }
            } else items(statuses, key = { it.id }) { status ->
                val index = statuses.indexOf(status)
                val recordCount = records.count { it.status == status.id }
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(status.name, style = MaterialTheme.typography.titleMedium); Text("$recordCount 部记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { reorderStatus(statuses, index, index - 1, viewModel) }, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, "上移") }
                        IconButton(onClick = { reorderStatus(statuses, index, index + 1, viewModel) }, enabled = index < statuses.lastIndex) { Icon(Icons.Default.ArrowDownward, "下移") }
                        IconButton(onClick = { editingStatus = status }) { Icon(Icons.Outlined.Edit, "编辑") }
                        IconButton(onClick = { deletingStatus = status }, enabled = status.id != "WATCHING") { Icon(Icons.Default.Delete, "删除") }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        CategoryEditor(
            category = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { category ->
                viewModel.saveCategory(category) { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
                creating = false
                editing = null
            }
        )
    }
    deleting?.let { category ->
        CategoryDeleteDialog(
            category = category,
            categories = categories,
            recordCount = records.count { it.categoryId == category.id },
            onDismiss = { deleting = null },
            onDelete = { targetCategoryId ->
                viewModel.deleteCategory(category.id, targetCategoryId) { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
                deleting = null
            }
        )
    }
    if (creatingStatus || editingStatus != null) {
        StatusEditor(editingStatus, { creatingStatus = false; editingStatus = null }, { value ->
            if (editingStatus == null) viewModel.addWatchStatus(value) { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
            else viewModel.renameWatchStatus(editingStatus!!.id, value) { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
            creatingStatus = false; editingStatus = null
        })
    }
    deletingStatus?.let { status ->
        val targets = statuses.filter { it.id != status.id }
        StatusDeleteDialog(status, targets, records.count { it.status == status.id }, { deletingStatus = null }) { target ->
            viewModel.deleteWatchStatus(status.id, target) { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
            deletingStatus = null
        }
    }
}

private fun reorderStatus(statuses: List<WatchStatusEntity>, from: Int, to: Int, viewModel: WatchlistViewModel) {
    if (to !in statuses.indices) return
    viewModel.reorderWatchStatuses(statuses.toMutableList().also { list -> val item = list.removeAt(from); list.add(to, item) })
}

@Composable private fun StatusEditor(status: WatchStatusEntity?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(status?.id) { mutableStateOf(status?.name.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (status == null) "添加观看状态" else "编辑观看状态") }, text = { OutlinedTextField(name, { name = it }, label = { Text("状态名称") }, singleLine = true) }, confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = name.trim().isNotEmpty()) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable private fun StatusDeleteDialog(status: WatchStatusEntity, targets: List<WatchStatusEntity>, recordCount: Int, onDismiss: () -> Unit, onDelete: (String?) -> Unit) {
    var target by remember(status.id) { mutableStateOf(targets.firstOrNull()?.id) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("删除观看状态？") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(if (recordCount == 0) "删除“${status.name}”后不可恢复。" else "该状态有 $recordCount 部记录，请选择迁移目标。") ; if (recordCount > 0) targets.forEach { item -> FilterChip(selected = target == item.id, onClick = { target = item.id }, label = { Text(item.name) }) } } }, confirmButton = { TextButton(onClick = { onDelete(target) }, enabled = recordCount == 0 || target != null) { Text("删除") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

private fun reorderCategory(categories: List<WatchCategoryEntity>, from: Int, to: Int, viewModel: WatchlistViewModel) {
    if (to !in categories.indices || from == to) return
    viewModel.reorderCategories(categories.toMutableList().also { list ->
        val category = list.removeAt(from)
        list.add(to, category)
    })
}

@Composable
private fun CategoryEditor(category: WatchCategoryEntity?, onDismiss: () -> Unit, onSave: (WatchCategoryEntity) -> Unit) {
    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "添加分类" else "编辑分类") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("分类名称") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            TextButton(onClick = { onSave(WatchCategoryEntity(category?.id ?: 0L, name.trim(), category?.sortOrder ?: Int.MAX_VALUE)) }, enabled = name.trim().isNotEmpty()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun CategoryDeleteDialog(
    category: WatchCategoryEntity,
    categories: List<WatchCategoryEntity>,
    recordCount: Int,
    onDismiss: () -> Unit,
    onDelete: (Long?) -> Unit
) {
    var targetCategoryId by remember(category.id) { mutableStateOf<Long?>(categories.firstOrNull { it.id != category.id }?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除分类？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (recordCount == 0) "删除“${category.name}”后不可恢复。" else "“${category.name}”内有 $recordCount 部记录，请选择接收分类。")
                if (recordCount > 0) {
                    categories.filter { it.id != category.id }.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { target ->
                                FilterChip(
                                    selected = targetCategoryId == target.id,
                                    onClick = { targetCategoryId = target.id },
                                    label = { Text(target.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDelete(targetCategoryId) }, enabled = recordCount == 0 || targetCategoryId != null) { Text("删除") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
