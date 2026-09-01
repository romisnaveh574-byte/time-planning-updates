package com.example.birthdaycountdown.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.birthdaycountdown.data.*
import com.example.birthdaycountdown.domain.*
import java.time.LocalDate
import java.time.LocalDateTime

private enum class DisplayTarget(val label: String) { SOLAR("阳历"), LUNAR("阴历"), COUNTDOWN("剩余时间") }
private enum class StyleTarget(val label: String) { BACKGROUND("卡片"), TITLE("标题"), SOLAR("阳历"), LUNAR("阴历"), COUNTDOWN("剩余") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    existing: CountdownEntity?,
    viewModel: AppViewModel,
    onRequestNotifications: () -> Unit,
    showBack: Boolean = true,
    initialType: RecordType = RecordType.ANNIVERSARY,
    onBack: (() -> Unit)? = null,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var type by remember(existing?.id, initialType) { mutableStateOf(existing?.type ?: initialType) }
    var calendarType by remember(existing?.id) { mutableStateOf(existing?.calendarType ?: CalendarType.SOLAR) }
    var dateTime by remember(existing?.id) { mutableStateOf(existing?.dateTimeIso?.let(LocalDateTime::parse) ?: LocalDateTime.now().withNano(0)) }
    var secondText by remember(existing?.id) { mutableStateOf(dateTime.second.toString()) }
    var lunarYearText by remember(existing?.id) { mutableStateOf((existing?.lunarYear ?: dateTime.year).toString()) }
    var lunarMonthText by remember(existing?.id) { mutableStateOf((existing?.lunarMonth ?: 1).toString()) }
    var lunarDayText by remember(existing?.id) { mutableStateOf((existing?.lunarDay ?: 1).toString()) }
    var lunarLeap by remember(existing?.id) { mutableStateOf(existing?.lunarLeapMonth ?: false) }
    var showSolarDate by remember(existing?.id) { mutableStateOf(existing?.showSolarDate ?: true) }
    var showLunarDate by remember(existing?.id) { mutableStateOf(existing?.showLunarDate ?: true) }
    var solarMask by remember(existing?.id) { mutableIntStateOf(existing?.solarDisplayMask ?: 63) }
    var lunarMask by remember(existing?.id) { mutableIntStateOf(existing?.lunarDisplayMask ?: 63) }
    var countdownMask by remember(existing?.id) { mutableIntStateOf(existing?.countdownDisplayMask ?: 63) }
    var backgroundColor by remember(existing?.id) { mutableIntStateOf(existing?.cardBackgroundColor ?: 0xFFE9E3EC.toInt()) }
    var titleColor by remember(existing?.id) { mutableIntStateOf(existing?.titleTextColor ?: 0xFF29232D.toInt()) }
    var solarColor by remember(existing?.id) { mutableIntStateOf(existing?.solarTextColor ?: 0xFF29232D.toInt()) }
    var lunarColor by remember(existing?.id) { mutableIntStateOf(existing?.lunarTextColor ?: 0xFF29232D.toInt()) }
    var countdownColor by remember(existing?.id) { mutableIntStateOf(existing?.countdownTextColor ?: 0xFF29232D.toInt()) }
    var cardGradientId by remember(existing?.id) { mutableStateOf(existing?.cardGradientId ?: "blue_cyan") }
    var titleGradientId by remember(existing?.id) { mutableStateOf(existing?.titleGradientId ?: "solid") }
    var solarGradientId by remember(existing?.id) { mutableStateOf(existing?.solarGradientId ?: "solid") }
    var lunarGradientId by remember(existing?.id) { mutableStateOf(existing?.lunarGradientId ?: "solid") }
    var countdownGradientId by remember(existing?.id) { mutableStateOf(existing?.countdownGradientId ?: "solid") }
    var reminder by remember(existing?.id) { mutableStateOf(existing?.reminderEnabled ?: false) }
    var lead by remember(existing?.id) { mutableIntStateOf(existing?.reminderMinutesBefore ?: 1440) }
    var basicExpanded by remember(existing?.id) { mutableStateOf(true) }
    var displayExpanded by remember(existing?.id) { mutableStateOf(false) }
    var reminderExpanded by remember(existing?.id) { mutableStateOf(false) }
    var colorExpanded by remember(existing?.id) { mutableStateOf(true) }
    var gradientExpanded by remember(existing?.id) { mutableStateOf(false) }
    var displayTarget by remember { mutableStateOf(DisplayTarget.SOLAR) }
    var colorTarget by remember { mutableStateOf(StyleTarget.BACKGROUND) }
    var gradientTarget by remember { mutableStateOf(StyleTarget.BACKGROUND) }
    var editorTab by remember { mutableIntStateOf(0) }
    var dirty by remember(existing?.id, initialType) { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val lunarYear = lunarYearText.toIntOrNull()
    val lunarMonth = lunarMonthText.toIntOrNull()
    val lunarDay = lunarDayText.toIntOrNull()
    val lunarValid = runCatching {
        require(lunarYear != null && lunarMonth != null && lunarDay != null)
        LunarCalendarConverter.toSolar(LunarDate(lunarYear, lunarMonth, lunarDay, lunarLeap))
    }.isSuccess
    val canSave = name.isNotBlank() && isValidSecondInput(secondText) && (calendarType == CalendarType.SOLAR || lunarValid) && countdownMask != 0 && (showSolarDate || showLunarDate)
    fun saveRecord() {
        viewModel.save(CountdownEntity(
            id = existing?.id ?: 0, type = type, name = name.trim(), dateTimeIso = dateTime.toString(), calendarType = calendarType,
            lunarYear = lunarYear, lunarMonth = lunarMonth, lunarDay = lunarDay, lunarLeapMonth = lunarLeap,
            reminderEnabled = reminder, reminderMinutesBefore = lead,
            showYears = countdownMask and 1 != 0, showMonths = countdownMask and 2 != 0, showDays = countdownMask and 4 != 0,
            showHours = countdownMask and 8 != 0, showMinutes = countdownMask and 16 != 0, showSeconds = countdownMask and 32 != 0,
            showSolarDate = showSolarDate, showLunarDate = showLunarDate, cardBackgroundColor = backgroundColor, cardTextColor = titleColor,
            solarDisplayMask = solarMask, lunarDisplayMask = lunarMask, countdownDisplayMask = countdownMask,
            cardGradientId = cardGradientId, titleGradientId = titleGradientId, solarGradientId = solarGradientId,
            lunarGradientId = lunarGradientId, countdownGradientId = countdownGradientId, titleTextColor = titleColor,
            solarTextColor = solarColor, lunarTextColor = lunarColor, countdownTextColor = countdownColor,
            sortOrder = existing?.sortOrder ?: Int.MAX_VALUE, isPinned = existing?.isPinned ?: false
        ))
        onDone()
    }
    fun requestExit() {
        if (dirty) confirmDiscard = true else (onBack ?: onDone)()
    }

    BackHandler(enabled = showBack) { requestExit() }

    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(
            title = { Text(if (existing == null) "添加时间" else "编辑时间") },
            navigationIcon = { if (showBack) IconButton(onClick = ::requestExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = glassTopAppBarColors()
        )
    }, bottomBar = {
        Button(
            onClick = ::saveRecord,
            enabled = canSave,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth().padding(16.dp).background(GlassStyle.primaryBrush, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        ) { Text("保存") }
    }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentedOptions(listOf("内容", "样式", "提醒"), editorTab) { editorTab = it }
            if (editorTab == 0) {
            ExpandableEditorSection(
                "基础信息",
                "${if (type == RecordType.BIRTHDAY) "生日" else "纪念日"} · ${if (calendarType == CalendarType.SOLAR) "阳历" else "阴历"}",
                basicExpanded,
                { basicExpanded = it }
            ) {
                OutlinedTextField(name, { name = it; dirty = true }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SegmentedOptions(listOf("生日", "纪念日"), if (type == RecordType.BIRTHDAY) 0 else 1) { type = if (it == 0) RecordType.BIRTHDAY else RecordType.ANNIVERSARY; dirty = true }
                SegmentedOptions(listOf("阳历", "阴历"), if (calendarType == CalendarType.SOLAR) 0 else 1) { calendarType = if (it == 0) CalendarType.SOLAR else CalendarType.LUNAR; dirty = true }
                if (calendarType == CalendarType.SOLAR) {
                    Text(DateFormatter.format(dateTime, DateFormatPreference.CHINESE))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { DatePickerDialog(context, { _, y, m, d -> dateTime = dateTime.withDate(y, m + 1, d); dirty = true }, dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth).show() }) { Text("选择日期") }
                        Button(onClick = { TimePickerDialog(context, { _, h, min -> dateTime = dateTime.withHour(h).withMinute(min); dirty = true }, dateTime.hour, dateTime.minute, true).show() }) { Text("选择时间") }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInput(lunarYearText, { lunarYearText = it; dirty = true }, "农历年", 4, Modifier.weight(1.4f))
                        NumberInput(lunarMonthText, { lunarMonthText = it; dirty = true }, "月", 2, Modifier.weight(1f))
                        NumberInput(lunarDayText, { lunarDayText = it; dirty = true }, "日", 2, Modifier.weight(1f))
                    }
                    val leapAvailable = runCatching { LunarCalendarConverter.leapMonthForYear(lunarYearText.toInt()) == lunarMonthText.toInt() }.getOrDefault(false)
                    SettingsSwitch("闰月", lunarLeap, enabled = leapAvailable || lunarLeap) { if (!it || leapAvailable) { lunarLeap = it; dirty = true } }
                    Button(onClick = { TimePickerDialog(context, { _, h, min -> dateTime = dateTime.withHour(h).withMinute(min); dirty = true }, dateTime.hour, dateTime.minute, true).show() }) { Text("选择时间 ${dateTime.toLocalTime()}") }
                }
                NumberInput(secondText, {
                    secondText = it; dirty = true
                    it.toIntOrNull()?.takeIf { second -> second in 0..59 }?.let { second -> dateTime = dateTime.withSecond(second) }
                }, "秒", 2, Modifier.width(112.dp))
            }

            ExpandableEditorSection(
                "显示内容",
                listOfNotNull("阳历".takeIf { showSolarDate }, "阴历".takeIf { showLunarDate }).joinToString("、"),
                displayExpanded,
                { displayExpanded = it }
            ) {
                SegmentedOptions(DisplayTarget.entries.map { it.label }, displayTarget.ordinal) { displayTarget = DisplayTarget.entries[it] }
                UnitMaskChips(when (displayTarget) { DisplayTarget.SOLAR -> solarMask; DisplayTarget.LUNAR -> lunarMask; DisplayTarget.COUNTDOWN -> countdownMask }) {
                    when (displayTarget) { DisplayTarget.SOLAR -> solarMask = it; DisplayTarget.LUNAR -> lunarMask = it; DisplayTarget.COUNTDOWN -> countdownMask = it }; dirty = true
                }
                Text("卡片日期")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(showSolarDate, { if (showLunarDate || !showSolarDate) { showSolarDate = !showSolarDate; dirty = true } }, label = { Text("显示阳历") })
                    FilterChip(showLunarDate, { if (showSolarDate || !showLunarDate) { showLunarDate = !showLunarDate; dirty = true } }, label = { Text("显示阴历") })
                }
            }
            }

            if (editorTab == 2) {

            ExpandableEditorSection(
                "提醒设置",
                if (reminder) "提前 ${lead / 1440} 天 ${(lead % 1440) / 60} 小时" else "未开启",
                reminderExpanded,
                { reminderExpanded = it }
            ) {
                SettingsSwitch("开启提醒", reminder) { reminder = it; dirty = true; if (it) onRequestNotifications() }
                if (reminder) {
                    Text("提前提醒：${lead / 1440} 天 ${(lead % 1440) / 60} 小时")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(60 to "1 小时", 1440 to "1 天", 10080 to "1 周").forEach { (minutes, label) -> FilterChip(lead == minutes, { lead = minutes; dirty = true }, label = { Text(label) }) } }
                }
            }
            }

            if (editorTab == 1) {

            ExpandableEditorSection("卡片颜色", "编辑 ${colorTarget.label} 颜色", colorExpanded, { colorExpanded = it }) {
                SegmentedOptions(StyleTarget.entries.map { it.label }, colorTarget.ordinal) { colorTarget = StyleTarget.entries[it] }
                val selectedColor = colorFor(colorTarget, backgroundColor, titleColor, solarColor, lunarColor, countdownColor)
                val setColor: (Int) -> Unit = { value ->
                    when (colorTarget) {
                        StyleTarget.BACKGROUND -> backgroundColor = value
                        StyleTarget.TITLE -> titleColor = value
                        StyleTarget.SOLAR -> solarColor = value
                        StyleTarget.LUNAR -> lunarColor = value
                        StyleTarget.COUNTDOWN -> countdownColor = value
                    }; dirty = true
                }
                ColorSwatches(selectedColor, listOf(0xFF29232D, 0xFFFFFFFF, 0xFF2563EB, 0xFF06B6D4, 0xFF34D399, 0xFFEC4899, 0xFFF97316, 0xFF111827).map { it.toInt() }, setColor)
                ColorEditor(colorTarget, selectedColor, setColor)
            }

            ExpandableEditorSection("渐变样式", "编辑 ${gradientTarget.label} 渐变", gradientExpanded, { gradientExpanded = it }) {
                SegmentedOptions(StyleTarget.entries.map { it.label }, gradientTarget.ordinal) { gradientTarget = StyleTarget.entries[it] }
                val selectedGradient = gradientFor(gradientTarget, cardGradientId, titleGradientId, solarGradientId, lunarGradientId, countdownGradientId)
                GradientSelector(selectedGradient) { value ->
                    when (gradientTarget) {
                        StyleTarget.BACKGROUND -> cardGradientId = value
                        StyleTarget.TITLE -> titleGradientId = value
                        StyleTarget.SOLAR -> solarGradientId = value
                        StyleTarget.LUNAR -> lunarGradientId = value
                        StyleTarget.COUNTDOWN -> countdownGradientId = value
                    }; dirty = true
                }
            }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("放弃未保存的修改？") },
            text = { Text("当前编辑内容尚未保存。") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; (onBack ?: onDone)() }) { Text("放弃修改") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("继续编辑") } }
        )
    }
}

@Composable
private fun ExpandableEditorSection(title: String, summary: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().clickable { onExpandedChange(!expanded) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "收起" else "展开")
            }
            if (expanded) Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
private fun ColorEditor(target: StyleTarget, color: Int, onColorChange: (Int) -> Unit) {
    var rgb by remember(target) { mutableStateOf(color.toRgbColor()) }
    var cmyk by remember(target) { mutableStateOf(rgbToCmyk(rgb)) }
    var r by remember(target) { mutableStateOf(rgb.red.toString()) }
    var g by remember(target) { mutableStateOf(rgb.green.toString()) }
    var b by remember(target) { mutableStateOf(rgb.blue.toString()) }
    var c by remember(target) { mutableStateOf(cmyk.cyan.toString()) }
    var m by remember(target) { mutableStateOf(cmyk.magenta.toString()) }
    var y by remember(target) { mutableStateOf(cmyk.yellow.toString()) }
    var k by remember(target) { mutableStateOf(cmyk.key.toString()) }

    LaunchedEffect(color) {
        val nextRgb = color.toRgbColor()
        if (nextRgb != rgb) {
            rgb = nextRgb
            cmyk = rgbToCmyk(nextRgb)
            r = rgb.red.toString(); g = rgb.green.toString(); b = rgb.blue.toString()
            c = cmyk.cyan.toString(); m = cmyk.magenta.toString(); y = cmyk.yellow.toString(); k = cmyk.key.toString()
        }
    }

    fun syncFromRgb() {
        val next = listOf(r, g, b).map { it.toIntOrNull() }
        if (next.all { it != null && it in 0..255 }) {
            rgb = RgbColor(next[0]!!, next[1]!!, next[2]!!)
            cmyk = rgbToCmyk(rgb)
            c = cmyk.cyan.toString(); m = cmyk.magenta.toString(); y = cmyk.yellow.toString(); k = cmyk.key.toString()
            onColorChange(rgb.toArgb())
        }
    }
    fun syncFromCmyk() {
        val next = listOf(c, m, y, k).map { it.toIntOrNull() }
        if (next.all { it != null && it in 0..100 }) {
            cmyk = CmykColor(next[0]!!, next[1]!!, next[2]!!, next[3]!!)
            rgb = cmykToRgb(cmyk)
            r = rgb.red.toString(); g = rgb.green.toString(); b = rgb.blue.toString()
            onColorChange(rgb.toArgb())
        }
    }

    Text("RGB", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorNumberField(r, { r = it; syncFromRgb() }, "R", Modifier.weight(1f))
        ColorNumberField(g, { g = it; syncFromRgb() }, "G", Modifier.weight(1f))
        ColorNumberField(b, { b = it; syncFromRgb() }, "B", Modifier.weight(1f))
    }
    Text("CMYK", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ColorNumberField(c, { c = it; syncFromCmyk() }, "C", Modifier.weight(1f))
        ColorNumberField(m, { m = it; syncFromCmyk() }, "M", Modifier.weight(1f))
        ColorNumberField(y, { y = it; syncFromCmyk() }, "Y", Modifier.weight(1f))
        ColorNumberField(k, { k = it; syncFromCmyk() }, "K", Modifier.weight(1f))
    }
    Box(Modifier.fillMaxWidth().height(40.dp).background(Color(color), MaterialTheme.shapes.small))
}

@Composable
private fun ColorNumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(value, { onValueChange(it.filter(Char::isDigit).take(3)) }, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier)
}

private fun colorFor(target: StyleTarget, background: Int, title: Int, solar: Int, lunar: Int, countdown: Int) = when (target) {
    StyleTarget.BACKGROUND -> background
    StyleTarget.TITLE -> title
    StyleTarget.SOLAR -> solar
    StyleTarget.LUNAR -> lunar
    StyleTarget.COUNTDOWN -> countdown
}

private fun gradientFor(target: StyleTarget, background: String, title: String, solar: String, lunar: String, countdown: String) = when (target) {
    StyleTarget.BACKGROUND -> background
    StyleTarget.TITLE -> title
    StyleTarget.SOLAR -> solar
    StyleTarget.LUNAR -> lunar
    StyleTarget.COUNTDOWN -> countdown
}

private fun LocalDateTime.withDate(year: Int, month: Int, day: Int): LocalDateTime = LocalDate.of(year, month, day).atTime(hour, minute, second)
