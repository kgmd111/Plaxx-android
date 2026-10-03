package com.plaxx.app.ui.theme

import androidx.compose.ui.graphics.Color

object EventColors {

    // 12 kolorow z web schedule.svelte.js - NIE ZMIENIAC KOLEJNOSCI
    private val palette = listOf(
        Color(0xFF4361EE), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEC4899),
        Color(0xFF22D3EE), Color(0xFFA3E635), Color(0xFF8B5CF6), Color(0xFFF97316),
        Color(0xFFFEF08A), Color(0xFFBFDBFE), Color(0xFFFBCFE8), Color(0xFFC7D2FE),
    )

    fun bgFor(subject: String): Color =
        palette[subject.sumOf { it.code } % palette.size]

    fun textFor(bg: Color): Color {
        val lum = (0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue)
        return if (lum > 0.6) Color(0xFF0F172A) else Color.White
    }
}
