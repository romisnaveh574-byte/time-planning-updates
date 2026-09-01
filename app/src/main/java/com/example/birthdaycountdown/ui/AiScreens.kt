@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.birthdaycountdown.ui

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import com.example.birthdaycountdown.ai.AiEndpointConfig
import com.example.birthdaycountdown.ai.AiProviderProfile
import com.example.birthdaycountdown.ai.AiChatService
import com.example.birthdaycountdown.ai.AiImageGenerationService
import com.example.birthdaycountdown.ai.AiPreferences
import com.example.birthdaycountdown.ai.referenceImageExtension
import com.example.birthdaycountdown.ai.AiSettings
import com.example.birthdaycountdown.ai.OpenAiCompatibleClient
import com.example.birthdaycountdown.ai.imageSizeFor
import com.example.birthdaycountdown.ai.imageGenerationStatusLabel
import com.example.birthdaycountdown.ai.isActiveAiStatus
import com.example.birthdaycountdown.ai.aiHistoryModeLabel
import com.example.birthdaycountdown.ai.gallerySaveResultLabel
import com.example.birthdaycountdown.ai.needsAiSetup
import com.example.birthdaycountdown.ai.removeProvider
import com.example.birthdaycountdown.ai.selectedProvider
import com.example.birthdaycountdown.data.AiHistoryRepository
import com.example.birthdaycountdown.data.AiMessageEntity
import com.example.birthdaycountdown.data.AiMode
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager

internal data class AiLaunchTarget(val mode: AiMode, val conversationId: Long)

@Composable
internal fun AiHomeScreen(
    historyRepository: AiHistoryRepository,
    onSettings: () -> Unit = {},
    launchTarget: AiLaunchTarget? = null,
    onLaunchConsumed: () -> Unit = {}
) {
    var page by remember { mutableStateOf(0) }
    var selectedConversation by remember { mutableStateOf<Long?>(null) }
    val conversations by historyRepository.conversations.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val aiPreferences = remember { AiPreferences(context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)) }
    var aiSettings by remember { mutableStateOf(aiPreferences.read()) }
    LaunchedEffect(page) { if (page == 0) aiSettings = aiPreferences.read() }
    LaunchedEffect(Unit) { historyRepository.failStaleMessages() }
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<com.example.birthdaycountdown.data.AiConversationEntity?>(null) }
    LaunchedEffect(launchTarget) {
        launchTarget?.let { target ->
            selectedConversation = target.conversationId
            page = if (target.mode == AiMode.CHAT) 1 else 2
            onLaunchConsumed()
        }
    }
    BackHandler(enabled = page != 0) { page = 0 }
    when (page) {
        1 -> AiChatScreen(historyRepository, selectedConversation) { page = 0 }
        2 -> AiImageScreen(historyRepository, selectedConversation) { page = 0 }
        else -> Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("AI") }, colors = glassTopAppBarColors()) }) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { AiFeatureCard("AI 对话", "新对话", Icons.AutoMirrored.Outlined.Chat) { selectedConversation = null; page = 1 } }
                item { AiFeatureCard("AI 生图", "新生图", Icons.Default.Image) { selectedConversation = null; page = 2 } }
                if (needsAiSetup(aiSettings.chat) || needsAiSetup(aiSettings.image)) item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("部分 AI 功能尚未配置完成。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = onSettings) { Text("去设置") }
                    }
                }
                val chatConversations = conversations.filter { it.mode == AiMode.CHAT.name }
                val imageConversations = conversations.filter { it.mode == AiMode.IMAGE.name }
                if (chatConversations.isNotEmpty()) {
                    item { SectionLabel("对话历史") }
                    items(chatConversations, key = { it.id }) { conversation ->
                        AiHistoryRow(conversation, onContinue = { selectedConversation = conversation.id; page = 1 }, onDelete = { deleteTarget = conversation })
                    }
                }
                if (imageConversations.isNotEmpty()) {
                    item { SectionLabel("生图历史") }
                    items(imageConversations, key = { it.id }) { conversation ->
                        AiHistoryRow(conversation, onContinue = { selectedConversation = conversation.id; page = 2 }, onDelete = { deleteTarget = conversation })
                    }
                }
                if (conversations.isEmpty()) {
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                                Text("从 AI 对话或 AI 生图开始", style = MaterialTheme.typography.titleMedium)
                                Text("完成的对话和图片会分别保存在这里", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let { conversation ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("永久删除记录？") },
            text = { Text("该会话及其中保存的本地图片将无法恢复。") },
            confirmButton = { TextButton(onClick = { scope.launch { historyRepository.deleteConversation(conversation.id) }; deleteTarget = null }) { Text("永久删除") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun AiHistoryRow(conversation: com.example.birthdaycountdown.data.AiConversationEntity, onContinue: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember(conversation.id) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onContinue).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            StatusLabel(aiHistoryModeLabel(conversation.mode))
        }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "更多操作") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("永久删除", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuOpen = false; onDelete() },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}

@Composable
private fun AiFeatureCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    GradientActionCard(title, subtitle, icon, onClick)
}

private data class ChatMessage(val role: String, val text: String, val imagePath: String? = null, val status: String = "DONE", val errorMessage: String? = null)

@Composable
private fun AiChatScreen(historyRepository: AiHistoryRepository, conversationId: Long?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages = rememberSaveable(saver = listSaver(
        save = { list -> list.flatMap { listOf(it.role, it.text, it.imagePath.orEmpty(), it.status, it.errorMessage.orEmpty()) } },
        restore = { restored -> mutableStateListOf<ChatMessage>().apply { restored.chunked(5).forEach { row -> if (row.size == 5) add(ChatMessage(row[0], row[1], row[2].ifBlank { null }, row[3], row[4].ifBlank { null })) } } }
    )) { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var attachmentPreview by remember { mutableStateOf<Bitmap?>(null) }
    var activeConversationId by remember { mutableStateOf(conversationId) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { imageUri = it }
    val listState = rememberLazyListState()
    LaunchedEffect(imageUri) {
        attachmentPreview = imageUri?.let { uri -> withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) } }
    }
    LaunchedEffect(activeConversationId) {
        activeConversationId?.let { id ->
            historyRepository.messages(id).collect { saved ->
                messages.clear()
                messages.addAll(saved.map { ChatMessage(it.role, it.text, it.imagePath, it.status, it.errorMessage) })
                if (saved.any { it.role == "assistant" && it.status == "DONE" && !it.resultViewed }) {
                    historyRepository.markConversationViewed(id)
                }
            }
        }
    }
    val working = messages.any { it.role == "assistant" && isActiveAiStatus(it.status) }
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("AI 对话") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, colors = glassTopAppBarColors()) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LaunchedEffect(messages.size, messages.lastOrNull()?.text, messages.lastOrNull()?.status) {
                val lastIndex = messages.lastIndex
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                if (lastIndex >= 0 && lastVisible >= lastIndex - 1) listState.animateScrollToItem(lastIndex)
            }
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (messages.isEmpty()) item { Text("输入问题开始对话", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(messages) { message ->
                    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = if (message.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if (message.role == "user") "我" else "AI", style = MaterialTheme.typography.labelMedium)
                            SelectionContainer {
                                Text(message.text)
                            }
                            message.imagePath?.let { StoredAiImage(context, historyRepository, it) }
                            if (message.role == "user" && message.status == "DONE") StatusLabel("已发送", tone = TaskTone.SUCCESS)
                            if (message.role == "assistant" && isActiveAiStatus(message.status)) StatusLabel("AI 正在思考", tone = taskToneFor(message.status))
                            if (message.role == "assistant" && message.status == "FAILED") Text(message.errorMessage ?: "回复失败，请重新发送上一条消息", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (working) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom) {
                imageUri?.let {
                    attachmentPreview?.let { Image(it.asImageBitmap(), "待发送图片", Modifier.size(52.dp)) }
                    TextButton(onClick = { imageUri = null; attachmentPreview = null }, enabled = !working) { Text("移除图片") }
                }
                IconButton(onClick = { picker.launch(arrayOf("image/*")) }, enabled = !working) { Icon(Icons.Default.AddPhotoAlternate, "选择图片") }
                OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("输入消息") }, maxLines = 4)
                IconButton(onClick = {
                    val prompt = input.trim()
                    if (prompt.isBlank() && imageUri == null || working) return@IconButton
                    scope.launch {
                        val selected = imageUri
                        val conversation = activeConversationId ?: historyRepository.newConversation(AiMode.CHAT, prompt.take(30)).also { activeConversationId = it }
                        val imagePath = selected?.let { saveInputImage(context, it, historyRepository) }
                        input = ""; imageUri = null
                        val userMessageId = historyRepository.append(AiMessageEntity(conversationId = conversation, role = "user", text = prompt, imagePath = imagePath, status = "DONE"))
                        val responseMessageId = historyRepository.append(AiMessageEntity(conversationId = conversation, role = "assistant", text = "", status = "PENDING", resultViewed = false))
                        runCatching { ContextCompat.startForegroundService(context, AiChatService.intent(context, responseMessageId, userMessageId, conversation, prompt)) }
                            .onFailure { scope.launch { historyRepository.failMessage(responseMessageId, "无法启动后台 AI 对话任务，请检查系统后台权限") } }
                    }
                }, enabled = !working && (input.isNotBlank() || imageUri != null)) { Icon(Icons.AutoMirrored.Filled.Send, "发送") }
            }
        }
    }
}

@Composable
private fun AiImageScreen(historyRepository: AiHistoryRepository, conversationId: Long?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var referenceUri by remember { mutableStateOf<Uri?>(null) }
    var referencePreview by remember { mutableStateOf<Bitmap?>(null) }
    var resolution by remember { mutableStateOf("1K") }
    var aspectRatio by remember { mutableStateOf("1:1") }
    val size = imageSizeFor(resolution, aspectRatio, referencePreview?.width, referencePreview?.height)
    var quality by remember { mutableStateOf("auto") }
    var activeConversationId by remember { mutableStateOf(conversationId) }
    val records by historyRepository.messages(activeConversationId ?: -1L).collectAsState(initial = emptyList())
    val working = records.any { isActiveAiStatus(it.status) }
    val referencePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { referenceUri = it }
    LaunchedEffect(referenceUri) {
        referencePreview = referenceUri?.let { withContext(Dispatchers.IO) { context.contentResolver.openInputStream(it)?.use(BitmapFactory::decodeStream) } }
    }
    LaunchedEffect(activeConversationId, records) {
        activeConversationId?.takeIf { records.any { record -> record.status == "DONE" && !record.resultViewed } }
            ?.let { historyRepository.markConversationViewed(it) }
    }
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("AI 生图") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, colors = glassTopAppBarColors()) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(prompt, { prompt = it }, label = { Text("提示词") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { referencePicker.launch(arrayOf("image/*")) }, enabled = !working) { Text("上传参考图") }
                referencePreview?.let { Image(it.asImageBitmap(), "参考图", Modifier.size(64.dp)) }
                if (referenceUri != null) TextButton(onClick = { referenceUri = null; referencePreview = null }, enabled = !working) { Text("移除") }
            }
            SectionLabel("分辨率")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1K", "2K", "4K").forEach { FilterChip(resolution == it, { resolution = it }, label = { Text(it) }) }
            }
            SectionLabel("比例")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("原比例", "1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9").forEach { FilterChip(aspectRatio == it, { aspectRatio = it }, label = { Text(it) }) }
            }
            Text("实际尺寸：${size ?: "按参考图原比例"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SectionLabel("图片质量")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("auto" to "自适应", "high" to "高", "medium" to "中", "low" to "低").forEach { (value, label) ->
                    FilterChip(quality == value, { quality = value }, label = { Text(label) })
                }
            }
            Button(onClick = {
                if (prompt.isBlank() || working) return@Button
                scope.launch {
                    val cleanPrompt = prompt.trim()
                    val conversation = activeConversationId ?: historyRepository.newConversation(AiMode.IMAGE, prompt.take(30)).also { activeConversationId = it }
                    val referencePath = referenceUri?.let { saveInputImage(context, it, historyRepository, "reference") }
                    val messageId = historyRepository.addPendingImage(conversation, cleanPrompt, size, quality, referencePath)
                    runCatching { ContextCompat.startForegroundService(context, AiImageGenerationService.intent(context, messageId, conversation, cleanPrompt, size, quality)) }
                        .onFailure { scope.launch { historyRepository.failImage(messageId, "无法启动后台生图任务，请检查系统后台权限") } }
                    prompt = ""
                    referenceUri = null
                    referencePreview = null
                }
            }, enabled = !working, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text(if (working) "生成中…" else "生成图片") }
            if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (records.isNotEmpty()) Text("本次记录", style = MaterialTheme.typography.titleMedium)
            records.forEach { record ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectionContainer {
                            Text(record.text)
                        }
                        Text("${record.size ?: "按原比例"} · ${qualityLabel(record.quality)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        record.actualSize?.let { actual ->
                            Text("实际尺寸：$actual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        record.warning?.let { warning ->
                            StatusLabel(warning, tone = TaskTone.WARNING)
                        }
                        when (record.status) {
                            else -> if (isActiveAiStatus(record.status)) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                                StatusLabel(imageGenerationStatusLabel(record.status), tone = taskToneFor(record.status))
                                Text("离开此页面不会取消任务", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (record.status == "FAILED") {
                            Text(record.errorMessage ?: "生成失败", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                scope.launch {
                                    historyRepository.retryImage(record.id)
                                    ContextCompat.startForegroundService(context, AiImageGenerationService.intent(context, record.id, record.conversationId, record.text, record.size, record.quality.orEmpty()))
                                }
                            }, enabled = !working) { Text("一键重新生成") }
                        }
                        record.imagePath?.let { StoredAiImage(context, historyRepository, it) }
                    }
                }
            }
        }
    }
}


@Composable
private fun StoredAiImage(context: android.content.Context, repository: AiHistoryRepository, path: String) {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(path) { mutableStateOf(true) }
    var failed by remember(path) { mutableStateOf(false) }
    LaunchedEffect(path) {
        val loaded = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(repository.imageFile(path).absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val sample = generateSampleSize(bounds.outWidth, bounds.outHeight, 2048)
            BitmapFactory.decodeFile(repository.imageFile(path).absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        bitmap = loaded
        loading = false
        failed = loaded == null
    }
    when {
        loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        failed -> Text("图片文件无法读取，请一键重新生成", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        bitmap != null -> bitmap!!.let { image ->
        Image(image.asImageBitmap(), "生成结果", Modifier.fillMaxWidth())
        TextButton(onClick = {
            val saved = saveStoredBitmap(context, repository, path)
            Toast.makeText(context, gallerySaveResultLabel(saved), Toast.LENGTH_SHORT).show()
        }) { Text("保存到相册") }
        }
    }
}

private fun qualityLabel(value: String?): String = when (value) {
    "high" -> "高"
    "medium" -> "中"
    "low" -> "低"
    else -> "自适应"
}

private fun generateSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    while (maxOf(width / sample, height / sample) > maxEdge) sample *= 2
    return sample
}

private fun saveStoredBitmap(context: android.content.Context, repository: AiHistoryRepository, path: String): Boolean {
    val bitmap = BitmapFactory.decodeFile(repository.imageFile(path).absolutePath) ?: return false
    return saveBitmap(context, bitmap)
}

private fun saveInputImage(context: android.content.Context, uri: Uri, repository: AiHistoryRepository, prefix: String = "chat"): String? = runCatching {
    val name = "$prefix-${System.currentTimeMillis()}.${referenceImageExtension(context.contentResolver.getType(uri))}"
    val file = repository.imageFile(name)
    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use(input::copyTo) } ?: return null
    name
}.getOrNull()

private fun saveBitmap(context: android.content.Context, bitmap: android.graphics.Bitmap): Boolean = runCatching {
    val values = android.content.ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "time-planning-${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TimePlanning") }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    val written = context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } == true
    if (!written) context.contentResolver.delete(uri, null, null)
    written
}.getOrDefault(false)

@Composable
fun AiSettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AiPreferences(context.getSharedPreferences("settings", 0)) }
    var settings by remember { mutableStateOf(prefs.read()) }
    val client = remember { OpenAiCompatibleClient() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Pair<Boolean, AiProviderProfile>?>(null) }
    var draftConfig by remember { mutableStateOf<AiEndpointConfig?>(null) }
    var draftName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Pair<Boolean, AiProviderProfile>?>(null) }
    var confirmExit by remember { mutableStateOf(false) }
    val dirty = settings != prefs.read()
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("AI 中转站") }, navigationIcon = { IconButton(onClick = { if (dirty) confirmExit = true else onDone() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, colors = glassTopAppBarColors()) }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AiProviderSection("AI 对话", true, settings.chatProfiles, settings.selectedChatId,
                onSelect = { id -> settings = settings.copy(selectedChatId = id); prefs.write(settings.copy(selectedChatId = id)) },
                onEdit = { editing = true to it; draftConfig = it.config; draftName = it.name }, onDelete = { deleteTarget = true to it },
                onAdd = { val profile = newProvider(true, settings.chatProfiles.size); editing = true to profile; draftConfig = profile.config; draftName = profile.name })
            AiProviderSection("AI 生图", false, settings.imageProfiles, settings.selectedImageId,
                onSelect = { id -> settings = settings.copy(selectedImageId = id); prefs.write(settings.copy(selectedImageId = id)) },
                onEdit = { editing = false to it; draftConfig = it.config; draftName = it.name }, onDelete = { deleteTarget = false to it },
                onAdd = { val profile = newProvider(false, settings.imageProfiles.size); editing = false to profile; draftConfig = profile.config; draftName = profile.name })
            Text("选择会立即生效；新增或编辑配置请在弹窗中点击“确定”。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    editing?.let { (isChat, profile) ->
        AlertDialog(
            onDismissRequest = { editing = null; draftConfig = null; draftName = "" },
            title = { Text(if (profile.config.model.isBlank()) "添加${if (isChat) "AI 对话" else "AI 生图"}中转站" else "编辑${if (isChat) "AI 对话" else "AI 生图"}中转站") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(draftName, { draftName = it }, label = { Text("配置名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    draftConfig?.let { draft -> AiConfigEditor("配置详情", draft, client, scope, isImage = !isChat) { draftConfig = it } }
                }
            },
            confirmButton = { TextButton(onClick = {
                val updated = profile.copy(name = draftName.trim().ifBlank { profile.name }, config = draftConfig ?: profile.config)
                settings = if (isChat) {
                    val list = settings.chatProfiles.let { current -> if (current.any { it.id == profile.id }) current.map { if (it.id == profile.id) updated else it } else current + updated }
                    settings.copy(chatProfiles = list, selectedChatId = profile.id)
                } else {
                    val list = settings.imageProfiles.let { current -> if (current.any { it.id == profile.id }) current.map { if (it.id == profile.id) updated else it } else current + updated }
                    settings.copy(imageProfiles = list, selectedImageId = profile.id)
                }
                prefs.write(settings); editing = null; draftConfig = null; draftName = ""
            }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { editing = null; draftConfig = null; draftName = "" }) { Text("取消") } }
        )
    }
    deleteTarget?.let { (isChat, profile) ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null }, title = { Text("永久删除配置？") },
            text = { Text(if ((isChat && settings.chatProfiles.size == 1) || (!isChat && settings.imageProfiles.size == 1)) "至少保留一个配置，当前配置不能删除。" else "删除“${profile.name}”后，保存的 API Key 和模型配置无法恢复。") },
            confirmButton = { TextButton(enabled = !((isChat && settings.chatProfiles.size == 1) || (!isChat && settings.imageProfiles.size == 1)), onClick = {
                val nextSettings = if (isChat) {
                    val remaining = removeProvider(settings.chatProfiles, profile.id)
                    settings.copy(chatProfiles = remaining, selectedChatId = selectedProvider(remaining, settings.selectedChatId)?.id)
                } else {
                    val remaining = removeProvider(settings.imageProfiles, profile.id)
                    settings.copy(imageProfiles = remaining, selectedImageId = selectedProvider(remaining, settings.selectedImageId)?.id)
                }
                settings = nextSettings
                deleteTarget = null
                prefs.write(nextSettings)
            }) { Text("永久删除") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text("放弃未保存修改？") },
        text = { Text("接口地址、API Key 或模型的修改尚未保存。") },
        confirmButton = { TextButton(onClick = { confirmExit = false; onDone() }) { Text("放弃") } },
        dismissButton = { TextButton(onClick = { prefs.write(settings); confirmExit = false; onDone() }) { Text("保存并返回") } }
    )
}

private fun newProvider(isChat: Boolean, index: Int) = AiProviderProfile(
    id = "${if (isChat) "chat" else "image"}-${System.currentTimeMillis()}",
    name = "${if (isChat) "AI 对话" else "AI 生图"} ${index + 1}",
    config = AiEndpointConfig()
)

@Composable
private fun AiProviderSection(title: String, isChat: Boolean, profiles: List<AiProviderProfile>, selectedId: String?, onSelect: (String) -> Unit, onEdit: (AiProviderProfile) -> Unit, onDelete: (AiProviderProfile) -> Unit, onAdd: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        profiles.forEach { profile ->
            val selected = selectedProvider(profiles, selectedId)?.id == profile.id
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(profile.id) },
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                        Text(profile.name)
                        Text(profile.config.model.ifBlank { "未选择模型" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(profile.config.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (selected) Icon(Icons.Outlined.Check, "当前使用")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("接口地址", profile.config.baseUrl))
                            Toast.makeText(context, "接口地址已复制", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Outlined.ContentCopy, "复制接口地址") }
                        IconButton(onClick = { onEdit(profile) }) { Icon(Icons.Outlined.Edit, "编辑") }
                        IconButton(onClick = { onDelete(profile) }) { Icon(Icons.Outlined.Delete, "永久删除") }
                    }
                }
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("添加${if (isChat) "AI 对话" else "AI 生图"}中转站") }
    }
}

@Composable
private fun AiConfigEditor(title: String, initial: AiEndpointConfig, client: OpenAiCompatibleClient, scope: kotlinx.coroutines.CoroutineScope, isImage: Boolean = false, onChange: (AiEndpointConfig) -> Unit) {
    var config by remember(initial) { mutableStateOf(initial) }
    var models by remember(initial) { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember(initial) { mutableStateOf(false) }
    var loading by remember(initial) { mutableStateOf(false) }
    var error by remember(initial) { mutableStateOf<String?>(null) }
    var connectionStatus by remember(initial) { mutableStateOf<String?>(null) }
    var showAllModels by remember(initial) { mutableStateOf(false) }
    var allModels by remember(initial) { mutableStateOf<List<String>>(emptyList()) }
    GlassPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(config.baseUrl, { config = config.copy(baseUrl = it); onChange(config) }, label = { Text("接口地址（含 /v1）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(config.apiKey, { config = config.copy(apiKey = it); onChange(config) }, label = { Text("API Key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Box {
                OutlinedTextField(config.model, { config = config.copy(model = it); onChange(config) }, label = { Text("模型") }, singleLine = true, modifier = Modifier.fillMaxWidth().clickable { expanded = true })
                DropdownMenu(expanded, { expanded = false }) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, softWrap = true) },
                            onClick = { config = config.copy(model = model); onChange(config); expanded = false }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                scope.launch {
                    loading = true
                    error = null
                    runCatching { withContext(Dispatchers.IO) { client.listModels(config) } }
                        .onSuccess { fetched ->
                            allModels = fetched
                            models = if (showAllModels) fetched else com.example.birthdaycountdown.ai.filterAiModels(fetched, isImage)
                            expanded = models.isNotEmpty()
                            if (fetched.isEmpty()) error = "接口返回的模型列表为空"
                        }
                        .onFailure { models = emptyList(); error = it.message ?: "获取模型失败" }
                    loading = false
                }
            }, enabled = !loading) { Text(if (loading) "加载中…" else "获取模型") }
            TextButton(onClick = {
                scope.launch {
                    loading = true; error = null; connectionStatus = null
                    runCatching { withContext(Dispatchers.IO) { client.listModels(config) } }
                        .onSuccess { connectionStatus = "连接成功，共 ${it.size} 个模型" }
                        .onFailure { error = it.message ?: "连接失败" }
                    loading = false
                }
            }, enabled = !loading) { Text("测试连接") }
            TextButton(onClick = {
                showAllModels = !showAllModels
                models = if (showAllModels) allModels else com.example.birthdaycountdown.ai.filterAiModels(allModels, isImage)
                expanded = models.isNotEmpty()
            }) { Text(if (showAllModels) "仅显示推荐模型" else "显示全部模型") }
            }
            if (models.isEmpty() && !loading) Text("点击“获取模型”加载可用模型", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            connectionStatus?.let { StatusLabel(it, tone = TaskTone.SUCCESS) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
