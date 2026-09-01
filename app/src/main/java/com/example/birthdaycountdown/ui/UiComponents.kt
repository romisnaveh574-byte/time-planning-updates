package com.example.birthdaycountdown.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.birthdaycountdown.domain.CardGradient
import com.example.birthdaycountdown.domain.CardGradients

@Composable
internal fun GradientActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    brush: Brush = GlassStyle.primaryBrush,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { role = Role.Button }.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        contentColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            Modifier.background(brush).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
                Icon(icon, null, modifier = Modifier.padding(10.dp).size(24.dp), tint = Color.White)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
        }
    }
}

@Composable
internal fun SectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
internal fun StatusLabel(text: String, modifier: Modifier = Modifier, tone: TaskTone = TaskTone.INFO) {
    val colors = MaterialTheme.colorScheme
    val foreground = when (tone) {
        TaskTone.INFO -> colors.primary
        TaskTone.PROGRESS -> colors.secondary
        TaskTone.SUCCESS -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF83D5A2) else Color(0xFF176B3A)
        TaskTone.WARNING -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFFFC66D) else Color(0xFF8A4F00)
        TaskTone.ERROR -> colors.error
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = foreground.copy(alpha = 0.13f)) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = foreground)
    }
}

@Composable
internal fun SegmentedOptions(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                label = { Text(label, maxLines = 1) }
            )
        }
    }
}

@Composable
internal fun NumberInput(value: String, onValueChange: (String) -> Unit, label: String, maxLength: Int, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(maxLength)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
internal fun SettingsSwitch(label: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
internal fun TextSizeSetting(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text("${value}sp") }
        Slider(value.toFloat(), { onValueChange(it.toInt()) }, valueRange = range.first.toFloat()..range.last.toFloat(), steps = range.last - range.first - 1)
    }
}

@Composable
internal fun UnitMaskChips(mask: Int, onChange: (Int) -> Unit) {
    val units = listOf("年" to 1, "月" to 2, "日" to 4, "时" to 8, "分" to 16, "秒" to 32)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        units.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (label, bit) ->
                    FilterChip(
                        selected = mask and bit != 0,
                        onClick = { val next = mask xor bit; if (next != 0) onChange(next) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ColorSwatches(selected: Int, colors: List<Int>, onSelected: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { color ->
            Box(Modifier.size(44.dp).semantics { contentDescription = "颜色 #${color.toUInt().toString(16).takeLast(6).uppercase()}"; this.selected = selected == color }.clickable { onSelected(color) }) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.small,
                    color = Color(color),
                    border = if (selected == color) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
                ) {}
                if (selected == color) {
                    Icon(Icons.Default.Check, "已选择", tint = if (Color(color).luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.align(Alignment.Center).size(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun GradientSelector(selectedId: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CardGradients.all.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { gradient ->
                    Box(
                        Modifier.weight(1f).height(44.dp).background(gradient.brushOrSolid(0xFF777777.toInt()), MaterialTheme.shapes.small).clickable { onSelected(gradient.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(color = Color.Black.copy(alpha = 0.48f), shape = RoundedCornerShape(4.dp)) {
                            Text(gradient.name, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = if (selectedId == gradient.id) FontWeight.Bold else FontWeight.Normal)
                        }
                        if (selectedId == gradient.id) Icon(Icons.Default.Check, "已选择", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(50)))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

internal fun CardGradient.brushOrNull(): Brush? = colors.takeIf { it.size > 1 }?.let { Brush.linearGradient(it.map(::Color)) }
internal fun CardGradient.brushOrSolid(fallback: Int): Brush = if (colors.size > 1) Brush.linearGradient(colors.map(::Color)) else Brush.linearGradient(listOf(Color(fallback), Color(fallback)))

internal fun <T> moveVisibleItem(
    records: List<T>,
    visibleIds: List<Long>,
    movingId: Long,
    direction: Int,
    idOf: (T) -> Long
): List<T> {
    if (visibleIds.isEmpty()) return records
    val visibleIndex = visibleIds.indexOf(movingId)
    val targetVisibleIndex = (visibleIndex + direction.coerceIn(-1, 1)).coerceIn(0, visibleIds.lastIndex)
    if (visibleIndex < 0 || visibleIndex == targetVisibleIndex) return records
    val from = records.indexOfFirst { idOf(it) == movingId }
    val targetId = visibleIds[targetVisibleIndex]
    val to = records.indexOfFirst { idOf(it) == targetId }
    if (from < 0 || to < 0) return records
    return records.toMutableList().apply {
        val movedItem = this[from]
        this[from] = this[to]
        this[to] = movedItem
    }
}
