package com.example.birthdaycountdown.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.birthdaycountdown.data.WatchCategoryEntity
import com.example.birthdaycountdown.data.WatchRecordEntity
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun reorderStatusLabel(dragging: Boolean): String? =
    if (dragging) "正在排序，松开后保存顺序" else null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onManageCategories: () -> Unit,
    onAdd: () -> Unit,
    startCreating: Boolean = false,
    onCreationFinished: () -> Unit = {}
) {
    val categories by viewModel.categories.collectAsState()
    val records by viewModel.records.collectAsState()
    val localRecords = remember { mutableStateListOf<WatchRecordEntity>() }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<WatchRecordEntity?>(null) }
    var creating by remember(startCreating) { mutableStateOf(startCreating) }
    var deleting by remember { mutableStateOf<WatchRecordEntity?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(records, draggingId) {
        if (draggingId == null) {
            localRecords.clear()
            localRecords.addAll(records)
        }
    }

    val visibleRecords = localRecords.filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
    val reorderMessage = reorderStatusLabel(dragging = draggingId != null)

    fun dismissEditor() {
        val shouldFinishCreation = creating && startCreating
        creating = false
        editing = null
        if (shouldFinishCreation) {
            onCreationFinished()
        }
    }

    fun saveRecord(record: WatchRecordEntity) {
        val shouldFinishCreation = creating && startCreating
        viewModel.saveRecord(record)
        creating = false
        editing = null
        if (shouldFinishCreation) {
            onCreationFinished()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "追剧记录",
                actions = {
                    IconButton(onClick = onManageCategories) {
                        Icon(Icons.Outlined.Category, contentDescription = "管理分类")
                    }
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = "添加记录")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = AppUiTokens.pageHorizontalPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(AppUiTokens.contentSpacing)
            ) {
                CategoryFilterRow(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it }
                )
                if (reorderMessage != null) {
                    StatusLabel(text = reorderMessage, tone = StatusTone.INFO)
                }
            }

            if (visibleRecords.isEmpty()) {
                EmptyState(
                    title = if (records.isEmpty()) "还没有追剧记录" else "此分类暂无记录",
                    message = if (categories.isEmpty()) "先创建分类，再添加追剧记录。" else null,
                    actionLabel = if (categories.isNotEmpty()) "添加记录" else null,
                    onActionClick = if (categories.isNotEmpty()) onAdd else null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppUiTokens.pageHorizontalPadding,
                        end = AppUiTokens.pageHorizontalPadding,
                        top = 0.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppUiTokens.contentSpacing)
                ) {
                    items(visibleRecords, key = { it.id }) { record ->
                        val currentVisibleIds by rememberUpdatedState(visibleRecords.map { it.id })
                        WatchRecordRow(
                            record = record,
                            categoryName = categories.firstOrNull { it.id == record.categoryId }?.name.orEmpty(),
                            dragging = draggingId == record.id,
                            onEdit = { editing = record },
                            onDecrease = { viewModel.adjustEpisode(record, -1) },
                            onIncrease = { viewModel.adjustEpisode(record, 1) },
                            onDelete = { deleting = record },
                            modifier = Modifier.pointerInput(record.id, selectedCategoryId) {
                                var dragDistance = 0f
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = record.id
                                        dragDistance = 0f
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragDistance = 0f
                                    },
                                    onDragEnd = {
                                        viewModel.reorderRecords(localRecords.toList())
                                        draggingId = null
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
                                            idOf = { it.id }
                                        )
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
    }

    if (creating || editing != null) {
        WatchRecordEditor(
            record = editing,
            categories = categories,
            onDismiss = ::dismissEditor,
            onSave = ::saveRecord
        )
    }

    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除追剧记录？", color = MaterialTheme.colorScheme.error) },
            text = { Text("“${record.title}”删除后不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(record)
                        deleting = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
private fun CategoryFilterRow(
    categories: List<WatchCategoryEntity>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val labels = listOf("全部") + categories.map(WatchCategoryEntity::name)
        val selectedIndex = categories.indexOfFirst { it.id == selectedCategoryId }.let { index ->
            if (selectedCategoryId == null || index < 0) 0 else index + 1
        }
        SingleChoiceSegmentedButtonRow {
            labels.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedIndex == index,
                    onClick = {
                        onSelect(
                            if (index == 0) {
                                null
                            } else {
                                categories[index - 1].id
                            }
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                    label = { Text(label, maxLines = 1) }
                )
            }
        }
    }
}

@Composable
private fun WatchRecordRow(
    record: WatchRecordEntity,
    categoryName: String,
    dragging: Boolean,
    onEdit: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (dragging) 0.7f else 1f)
            .clickable(onClick = onEdit),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (dragging) 4.dp else 1.dp,
        shadowElevation = if (dragging) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (categoryName.isNotBlank()) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "第 ${record.currentEpisode} 集",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDecrease,
                enabled = record.currentEpisode > 0
            ) {
                Icon(Icons.Default.Remove, contentDescription = "减少集数")
            }
            IconButton(onClick = onIncrease) {
                Icon(Icons.Default.Add, contentDescription = "增加集数")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = "长按调整顺序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WatchRecordEditor(
    record: WatchRecordEntity?,
    categories: List<WatchCategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (WatchRecordEntity) -> Unit
) {
    var title by remember(record?.id) { mutableStateOf(record?.title.orEmpty()) }
    var selectedCategoryId by remember(record?.id, categories) {
        mutableLongStateOf(record?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }
    var episode by remember(record?.id) { mutableStateOf(record?.currentEpisode?.toString() ?: "0") }
    val normalizedTitle = title.trim()
    val parsedEpisode = episode.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record == null) "添加追剧记录" else "编辑追剧记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader(
                    title = if (record == null) "新建记录" else "修改记录",
                    supportingText = if (categories.isEmpty()) "请先创建分类。" else "选择分类并更新当前集数。"
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("剧名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "分类",
                    style = MaterialTheme.typography.labelLarge
                )
                CategorySelectionRow(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it }
                )
                OutlinedTextField(
                    value = episode,
                    onValueChange = { episode = it.filter(Char::isDigit).take(6) },
                    label = { Text("当前集数") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { episode = (parsedEpisode - 1).coerceAtLeast(0).toString() },
                                enabled = parsedEpisode > 0
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "减少集数")
                            }
                            IconButton(onClick = { episode = (parsedEpisode + 1).toString() }) {
                                Icon(Icons.Default.Add, contentDescription = "增加集数")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        WatchRecordEntity(
                            id = record?.id ?: 0L,
                            title = normalizedTitle,
                            categoryId = selectedCategoryId,
                            currentEpisode = parsedEpisode,
                            sortOrder = record?.sortOrder ?: Int.MAX_VALUE
                        )
                    )
                },
                enabled = normalizedTitle.isNotEmpty() && selectedCategoryId > 0
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CategorySelectionRow(
    categories: List<WatchCategoryEntity>,
    selectedCategoryId: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(viewModel: WatchlistViewModel, onBack: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val records by viewModel.records.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WatchCategoryEntity?>(null) }
    var deleting by remember { mutableStateOf<WatchCategoryEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "分类管理",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加分类")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = AppUiTokens.pageHorizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.contentSpacing)
        ) {
            items(categories, key = { it.id }) { category ->
                val index = categories.indexOf(category)
                val recordCount = records.count { it.categoryId == category.id }
                AppListItem(
                    headline = category.name,
                    supportingText = "$recordCount 部记录",
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { reorderCategory(categories, index, index - 1, viewModel) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "上移")
                            }
                            IconButton(
                                onClick = { reorderCategory(categories, index, index + 1, viewModel) },
                                enabled = index < categories.lastIndex
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "下移")
                            }
                            IconButton(onClick = { editing = category }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "编辑")
                            }
                            IconButton(
                                onClick = { deleting = category },
                                enabled = categories.size > 1
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                )
            }
        }
    }

    if (creating || editing != null) {
        CategoryEditor(
            category = editing,
            onDismiss = {
                creating = false
                editing = null
            },
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
}

private fun reorderCategory(
    categories: List<WatchCategoryEntity>,
    from: Int,
    to: Int,
    viewModel: WatchlistViewModel
) {
    if (to !in categories.indices || from == to) return
    viewModel.reorderCategories(categories.toMutableList().also { list ->
        val category = list.removeAt(from)
        list.add(to, category)
    })
}

@Composable
private fun CategoryEditor(
    category: WatchCategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (WatchCategoryEntity) -> Unit
) {
    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "添加分类" else "编辑分类") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        WatchCategoryEntity(
                            id = category?.id ?: 0L,
                            name = name.trim(),
                            sortOrder = category?.sortOrder ?: Int.MAX_VALUE
                        )
                    )
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
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
    var targetCategoryId by remember(category.id) {
        mutableStateOf<Long?>(categories.firstOrNull { it.id != category.id }?.id)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除分类？", color = MaterialTheme.colorScheme.error) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (recordCount == 0) {
                        "删除“${category.name}”后不可恢复。"
                    } else {
                        "“${category.name}”内有 $recordCount 部记录，请选择接收分类。"
                    }
                )
                if (recordCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories
                                .filter { it.id != category.id }
                                .forEach { target ->
                                    FilterChip(
                                        selected = targetCategoryId == target.id,
                                        onClick = { targetCategoryId = target.id },
                                        label = { Text(target.name) }
                                    )
                                }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDelete(targetCategoryId) },
                enabled = recordCount == 0 || targetCategoryId != null,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
