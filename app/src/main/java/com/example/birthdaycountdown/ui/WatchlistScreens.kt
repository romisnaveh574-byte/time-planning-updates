package com.example.birthdaycountdown.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.birthdaycountdown.data.WatchCategoryEntity
import com.example.birthdaycountdown.data.WatchRecordEntity
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onBack: () -> Unit,
    onManageCategories: () -> Unit,
    startCreating: Boolean = false
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
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("追剧记录") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = onManageCategories) { Icon(Icons.Outlined.Category, "管理分类") }
                    IconButton(onClick = { creating = true }) { Icon(Icons.Default.Add, "添加记录") }
                },
                colors = glassTopAppBarColors()
            )
        }
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
                            Text("正在追 ${records.size} 部", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Outlined.Movie, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
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
                            Text(if (records.isEmpty()) "还没有追剧记录" else "此分类暂无记录", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = { creating = true }, enabled = categories.isNotEmpty()) {
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
                        categoryName = categories.firstOrNull { it.id == record.categoryId }?.name.orEmpty(),
                        dragging = draggingId == record.id,
                        onEdit = { editing = record },
                        onDecrease = { viewModel.adjustEpisode(record, -1) },
                        onIncrease = { viewModel.adjustEpisode(record, 1) },
                        onDelete = { deleting = record },
                        modifier = Modifier.pointerInput(record.id, selectedCategoryId) {
                            var dragDistance = 0f
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingId = record.id; dragDistance = 0f },
                                onDragCancel = { draggingId = null; dragDistance = 0f },
                                onDragEnd = {
                                    viewModel.reorderRecords(localRecords.toList())
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

    if (creating || editing != null) {
        WatchRecordEditor(
            record = editing,
            categories = categories,
            onDismiss = { creating = false; editing = null },
            onSave = {
                viewModel.saveRecord(it)
                creating = false
                editing = null
            }
        )
    }
    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除追剧记录？") },
            text = { Text(record.title) },
            confirmButton = { TextButton(onClick = { viewModel.deleteRecord(record); deleting = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
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
private fun WatchRecordCard(
    record: WatchRecordEntity,
    categoryName: String,
    dragging: Boolean,
    onEdit: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (dragging) 8.dp else 0.dp, MaterialTheme.shapes.medium)
            .alpha(if (dragging) 0.9f else 1f)
            .clickable(onClick = onEdit)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (dragging) Text("正在调整顺序", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium)
                    if (categoryName.isNotBlank()) AssistChip(onClick = onEdit, label = { Text(categoryName) })
                }
                Icon(Icons.Outlined.DragHandle, "长按调整顺序", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, enabled = record.currentEpisode > 0) { Icon(Icons.Default.Remove, "减少集数") }
                Text("第 ${record.currentEpisode} 集", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 12.dp))
                IconButton(onClick = onIncrease) { Icon(Icons.Default.Add, "增加集数") }
            }
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
    var selectedCategoryId by remember(record?.id, categories) { mutableLongStateOf(record?.categoryId ?: categories.firstOrNull()?.id ?: 0L) }
    var episode by remember(record?.id) { mutableStateOf(record?.currentEpisode?.toString() ?: "0") }
    val normalizedTitle = title.trim()
    val parsedEpisode = episode.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record == null) "添加追剧记录" else "编辑追剧记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(WatchRecordEntity(record?.id ?: 0L, normalizedTitle, selectedCategoryId, parsedEpisode, record?.sortOrder ?: Int.MAX_VALUE)) },
                enabled = normalizedTitle.isNotEmpty() && selectedCategoryId > 0
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
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
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Default.Add, "添加分类") } },
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
            items(categories, key = { it.id }) { category ->
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
