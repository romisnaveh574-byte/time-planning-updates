package com.example.birthdaycountdown.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.birthdaycountdown.domain.CardGradient
import com.example.birthdaycountdown.domain.CardGradients

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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { color ->
            Box(Modifier.weight(1f).aspectRatio(1f).clickable { onSelected(color) }) {
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
                        Text(gradient.name, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = if (selectedId == gradient.id) FontWeight.Bold else FontWeight.Normal)
                        if (selectedId == gradient.id) Icon(Icons.Default.Check, "已选择", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp))
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
