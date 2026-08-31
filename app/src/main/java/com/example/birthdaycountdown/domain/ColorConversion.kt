package com.example.birthdaycountdown.domain

import kotlin.math.roundToInt

data class RgbColor(val red: Int, val green: Int, val blue: Int)
data class CmykColor(val cyan: Int, val magenta: Int, val yellow: Int, val key: Int)

fun rgbToCmyk(value: RgbColor): CmykColor {
    val red = value.red.coerceIn(0, 255) / 255.0
    val green = value.green.coerceIn(0, 255) / 255.0
    val blue = value.blue.coerceIn(0, 255) / 255.0
    val key = 1.0 - maxOf(red, green, blue)
    if (key >= 0.999999) return CmykColor(0, 0, 0, 100)
    return CmykColor(
        (((1.0 - red - key) / (1.0 - key)) * 100).roundToInt(),
        (((1.0 - green - key) / (1.0 - key)) * 100).roundToInt(),
        (((1.0 - blue - key) / (1.0 - key)) * 100).roundToInt(),
        (key * 100).roundToInt()
    )
}

fun cmykToRgb(value: CmykColor): RgbColor {
    val cyan = value.cyan.coerceIn(0, 100) / 100.0
    val magenta = value.magenta.coerceIn(0, 100) / 100.0
    val yellow = value.yellow.coerceIn(0, 100) / 100.0
    val key = value.key.coerceIn(0, 100) / 100.0
    return RgbColor(
        (255 * (1 - cyan) * (1 - key)).roundToInt(),
        (255 * (1 - magenta) * (1 - key)).roundToInt(),
        (255 * (1 - yellow) * (1 - key)).roundToInt()
    )
}

fun RgbColor.toArgb(): Int =
    (0xFF shl 24) or (red.coerceIn(0, 255) shl 16) or (green.coerceIn(0, 255) shl 8) or blue.coerceIn(0, 255)

fun Int.toRgbColor(): RgbColor = RgbColor(shr(16) and 255, shr(8) and 255, and(255))
