package com.example.birthdaycountdown.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.birthdaycountdown.BuildConfig
import com.example.birthdaycountdown.domain.DateFormatPreference
import com.example.birthdaycountdown.update.*
import com.example.birthdaycountdown.data.backupScopeDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onDisplaySettings: () -> Unit,
    onDataBackup: () -> Unit,
    onApplicationSettings: () -> Unit,
    onAiSettings: () -> Unit,
    onDone: () -> Unit
) {
    val format by viewModel.format.collectAsState()
    val displaySettings by viewModel.displaySettings.collectAsState()
    val calendars = listOfNotNull(
        "阳历".takeIf { displaySettings.showSolarDate },
        "阴历".takeIf { displaySettings.showLunarDate }
    ).joinToString("、")
    Scaffold(containerColor = Color.Transparent, topBar = { SettingsTopBar("设置", onDone) }) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("偏好设置", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            SettingsCategoryRow("显示与格式", "${if (format == DateFormatPreference.CHINESE) "中文日期" else "数字日期"} · $calendars", Icons.Outlined.Tune, onDisplaySettings)
            Spacer(Modifier.height(8.dp))
            Text("数据与应用", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            SettingsCategoryRow("数据与备份", "本地记录可导出与合并恢复", Icons.Outlined.Settings, onDataBackup)
            SettingsCategoryRow("应用更新", "当前 ${BuildConfig.VERSION_NAME}", Icons.Outlined.Info, onApplicationSettings)
            SettingsCategoryRow("AI 中转站", "分别配置 AI 对话和 AI 生图", Icons.Outlined.AutoAwesome, onAiSettings)
            Text("卡片版式在显示与格式中统一设置，颜色和提醒在编辑具体记录时设置。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(viewModel: AppViewModel, onDone: () -> Unit) {
    val format by viewModel.format.collectAsState()
    val settings by viewModel.displaySettings.collectAsState()
    Scaffold(containerColor = Color.Transparent, topBar = { SettingsTopBar("显示与格式", onDone) }) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("日期格式", if (format == DateFormatPreference.CHINESE) "中文格式" else "数字格式", initiallyExpanded = true) {
                FilterChip(format == DateFormatPreference.CHINESE, { viewModel.setFormat(DateFormatPreference.CHINESE) }, label = { Text("2026 年 5 月 23 日 08 时 35 分 46 秒") })
                FilterChip(format == DateFormatPreference.NUMERIC, { viewModel.setFormat(DateFormatPreference.NUMERIC) }, label = { Text("2026/05/23 08:35:46") })
            }
            SettingsSection("历法显示", listOfNotNull("阳历".takeIf { settings.showSolarDate }, "阴历".takeIf { settings.showLunarDate }).joinToString("、")) {
                SettingsSwitch("显示阳历", settings.showSolarDate) { viewModel.setDisplaySettings(settings.copy(showSolarDate = it)) }
                SettingsSwitch("显示阴历", settings.showLunarDate) { viewModel.setDisplaySettings(settings.copy(showLunarDate = it)) }
            }
            SettingsSection("计时显示单位", unitSummary(settings.toMask())) {
                UnitMaskChips(settings.toMask()) { viewModel.setDisplaySettings(settings.fromMask(it)) }
            }
            SettingsSection("文字样式", "标题 ${settings.titleTextSize}sp · 日期 ${settings.dateTextSize}sp · 计时 ${settings.countdownTextSize}sp") {
                TextSizeSetting("标题文字", settings.titleTextSize, 14..32) { viewModel.setDisplaySettings(settings.copy(titleTextSize = it)) }
                SettingsSwitch("标题加粗", settings.titleBold) { viewModel.setDisplaySettings(settings.copy(titleBold = it)) }
                TextSizeSetting("日期文案", settings.dateTextSize, 10..24) { viewModel.setDisplaySettings(settings.copy(dateTextSize = it)) }
                SettingsSwitch("日期文案加粗", settings.dateBold) { viewModel.setDisplaySettings(settings.copy(dateBold = it)) }
                TextSizeSetting("倒计时文字", settings.countdownTextSize, 12..30) { viewModel.setDisplaySettings(settings.copy(countdownTextSize = it)) }
                SettingsSwitch("倒计时加粗", settings.countdownBold) { viewModel.setDisplaySettings(settings.copy(countdownBold = it)) }
            }
            SettingsSection("卡片版式", settings.cardLayoutStyle.label, initiallyExpanded = true) {
                SegmentedOptions(cardLayoutStyleLabels(), settings.cardLayoutStyle.ordinal) {
                    viewModel.setDisplaySettings(settings.copy(cardLayoutStyle = CardLayoutStyle.entries[it]))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavSettingsScreen(viewModel: AppViewModel, onDone: () -> Unit) {
    val settings by viewModel.bottomNavSettings.collectAsState()
    Scaffold(containerColor = Color.Transparent, topBar = { SettingsTopBar("底部导航", onDone) }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NavItemEditor("第一个入口", settings.time) { viewModel.setBottomNavSettings(settings.copy(time = it)) }
            NavItemEditor("第二个入口", settings.add) { viewModel.setBottomNavSettings(settings.copy(add = it)) }
            NavItemEditor("第三个入口", settings.profile) { viewModel.setBottomNavSettings(settings.copy(profile = it)) }
            NavItemEditor("第四个入口", settings.ai) { viewModel.setBottomNavSettings(settings.copy(ai = it)) }
            OutlinedButton(onClick = viewModel::resetBottomNavSettings, modifier = Modifier.fillMaxWidth()) { Text("恢复默认") }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupSettingsScreen(viewModel: AppViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            working = true
            status = runCatching {
                val content = viewModel.exportBackup()
                val written = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                        true
                    } ?: false
                }
                check(written) { "无法写入备份文件" }
                "备份已导出"
            }.getOrElse { "导出失败：${it.message ?: "未知错误"}" }
            working = false
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            working = true
            status = runCatching {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                } ?: error("无法读取备份文件")
                "已导入 ${viewModel.importBackup(content)} 条记录"
            }.getOrElse { "导入失败：${it.message ?: "备份文件无效"}" }
            working = false
        }
    }
    Scaffold(containerColor = Color.Transparent, topBar = { SettingsTopBar("数据与备份", onDone) }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSection("备份记录", "导出当前全部记录", initiallyExpanded = true) {
                Text(backupScopeDescription(), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { exportLauncher.launch("时间规划局备份.json") }, enabled = !working, modifier = Modifier.fillMaxWidth()) { Text("导出备份") }
            }
            SettingsSection("恢复记录", "只合并新增记录") {
                Text("导入会合并新增记录，不会删除当前记录。", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, enabled = !working, modifier = Modifier.fillMaxWidth()) { Text("从文件导入") }
            }
            if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
            status?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

private sealed interface UpdateUiState {
    object Idle : UpdateUiState
    object Checking : UpdateUiState
    object Latest : UpdateUiState
    data class Available(val release: ReleaseInfo) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationSettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    val releasePageUrl = latestReleasePageUrl(BuildConfig.UPDATE_REPOSITORY_OWNER, BuildConfig.UPDATE_REPOSITORY_NAME)
    fun checkForUpdate() {
        scope.launch {
            state = UpdateUiState.Checking
            state = when (val result = UpdateChecker(BuildConfig.UPDATE_REPOSITORY_OWNER, BuildConfig.UPDATE_REPOSITORY_NAME).checkResult(AppVersion(BuildConfig.VERSION_CODE.toLong(), BuildConfig.VERSION_NAME))) {
                is UpdateCheckResult.Available -> UpdateUiState.Available(result.release)
                UpdateCheckResult.Latest -> UpdateUiState.Latest
                is UpdateCheckResult.Failed -> UpdateUiState.Failed(result.message)
            }
        }
    }
    fun openReleasePage() {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releasePageUrl)))
        }.onFailure {
            state = UpdateUiState.Failed("无法打开浏览器")
        }
    }
    Scaffold(containerColor = Color.Transparent, topBar = { SettingsTopBar("应用更新", onDone) }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSection("当前版本", "${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）", initiallyExpanded = true) {
                Text("时间规划局 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                Text("版本号 ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when (val current = state) {
                UpdateUiState.Idle -> Button(onClick = ::checkForUpdate, modifier = Modifier.fillMaxWidth()) { Text("检查更新") }
                UpdateUiState.Checking -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("正在检查更新…")
                }
                UpdateUiState.Latest -> {
                    Text("当前已是最新版本。")
                    OutlinedButton(onClick = ::checkForUpdate, modifier = Modifier.fillMaxWidth()) { Text("重新检查") }
                }
                is UpdateUiState.Available -> {
                    Text("发现新版本 ${current.release.version.name}", style = MaterialTheme.typography.titleMedium)
                    if (current.release.notes.isNotBlank()) Text(current.release.notes, style = MaterialTheme.typography.bodyMedium)
                }
                is UpdateUiState.Failed -> {
                    Text("更新失败：${current.message}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = ::checkForUpdate, modifier = Modifier.fillMaxWidth()) { Text("重试") }
                }
            }
            Text(
                releasePageUrl,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().clickable(onClick = ::openReleasePage)
            )
            OutlinedButton(onClick = ::openReleasePage, modifier = Modifier.fillMaxWidth()) { Text("在浏览器中打开 Release 页面") }
        }
    }
}

@Composable
private fun SettingsSection(title: String, summary: String, initiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "收起" else "展开")
        }
        if (expanded) Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
        colors = glassTopAppBarColors()
    )
}

@Composable
private fun SettingsCategoryRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun NavItemEditor(title: String, item: BottomNavItemSettings, onChange: (BottomNavItemSettings) -> Unit) {
    val summary = buildList {
        if (item.showIcon) add("图标")
        if (item.showLabel) add(item.label.ifBlank { "小文字" })
    }.joinToString(" · ").ifBlank { "已隐藏" }
    SettingsSection(title, summary) {
        OutlinedTextField(item.label, { value -> if (value.length <= 4 && (item.showIcon || value.isNotBlank())) onChange(item.copy(label = value)) }, label = { Text("小文字（最多 4 字）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("图标")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            BottomNavIconId.entries.forEach { icon ->
                IconButton(
                    onClick = { onChange(item.copy(icon = icon)) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = if (item.icon == icon) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                ) { Icon(navIcon(icon), icon.name) }
            }
        }
        SettingsSwitch("显示图标", item.showIcon) { onChange(item.withIconVisibility(it)) }
        SettingsSwitch("显示小文字", item.showLabel) { onChange(item.withLabelVisibility(it)) }
    }
}

private fun unitSummary(mask: Int): String = listOfNotNull(
    "年".takeIf { mask and 1 != 0 },
    "月".takeIf { mask and 2 != 0 },
    "日".takeIf { mask and 4 != 0 },
    "时".takeIf { mask and 8 != 0 },
    "分".takeIf { mask and 16 != 0 },
    "秒".takeIf { mask and 32 != 0 }
).joinToString("、").ifBlank { "未选择" }

private fun AppDisplaySettings.toMask(): Int =
    (if (showYears) 1 else 0) or (if (showMonths) 2 else 0) or (if (showDays) 4 else 0) or (if (showHours) 8 else 0) or (if (showMinutes) 16 else 0) or (if (showSeconds) 32 else 0)

private fun AppDisplaySettings.fromMask(mask: Int) = copy(
    showYears = mask and 1 != 0,
    showMonths = mask and 2 != 0,
    showDays = mask and 4 != 0,
    showHours = mask and 8 != 0,
    showMinutes = mask and 16 != 0,
    showSeconds = mask and 32 != 0
)

private fun formatBytes(value: Long): String = if (value < 1024 * 1024) "${value / 1024} KB" else "%.1f MB".format(value / 1024f / 1024f)
