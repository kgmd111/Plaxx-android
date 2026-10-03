package com.plaxx.app.ui.theme

import androidx.compose.ui.graphics.Color

val BASE_PALETTE = listOf(
    "#4361ee", "#10b981", "#f59e0b", "#ec4899",
    "#22d3ee", "#a3e635", "#8b5cf6", "#f97316",
    "#fef08a", "#bfdbfe", "#fbcfe8", "#c7d2fe",
)

fun contrastText(hex: String): Color {
    val c = hex.removePrefix("#")
    if (c.length != 6) return Color.White
    val r = c.substring(0, 2).toInt(16)
    val g = c.substring(2, 4).toInt(16)
    val b = c.substring(4, 6).toInt(16)
    val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    return if (lum > 0.6) Color(0xFF0F172A) else Color.White
}

private fun hexToColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF4361EE))

fun subjectColorHex(
    subject: String,
    customColors: Map<String, String> = emptyMap(),
): String = customColors[subject]
    ?: BASE_PALETTE[subject.sumOf { it.code } % BASE_PALETTE.size]

fun subjectColor(
    subject: String,
    customColors: Map<String, String> = emptyMap(),
): Pair<Color, Color> {
    val hex = subjectColorHex(subject, customColors)
    return hexToColor(hex) to contrastText(hex)
}
