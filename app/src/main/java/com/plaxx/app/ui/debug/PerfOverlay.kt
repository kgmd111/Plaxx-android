package com.plaxx.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PerfOverlay(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    val bg = Color(0xCC000000)
    val fg = Color(0xFF00FF88)
    val warn = Color(0xFFFFAA00)
    val bad = Color(0xFFFF4444)
    val gray = Color(0xFF888888)

    val avg = PerfMonitor.avgFps
    val fpsColor = when {
        avg >= 55f -> fg
        avg >= 40f -> warn
        else -> bad
    }

    Box(modifier = modifier.padding(8.dp)) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .clickable { expanded = !expanded }
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "FPS ${fmt0(PerfMonitor.currentFps)}  avg ${fmt0(avg)}",
                color = fpsColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "worst ${fmt1(PerfMonitor.worstFrameMs)}ms",
                color = if (PerfMonitor.worstFrameMs > 32f) bad else fg,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )

            if (expanded) {
                Text("─── physics ───", color = gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text(
                    "loop ${fmt2(PerfMonitor.physicsMs)}ms  avg ${fmt2(PerfMonitor.physicsAvgMs)}ms",
                    color = if (PerfMonitor.physicsAvgMs > 4f) warn else fg,
                    fontFamily = FontFamily.Monospace, fontSize = 10.sp
                )
                Text("─── snapshot ───", color = gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text(
                    "commits/frame ${PerfMonitor.snapshotCommits}",
                    color = if (PerfMonitor.snapshotCommits > 1) warn else fg,
                    fontFamily = FontFamily.Monospace, fontSize = 10.sp
                )
                Text("─── tiles ───", color = gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                Text(
                    "active ${PerfMonitor.activeTiles}  rendered ${PerfMonitor.renderedTiles}",
                    color = fg, fontFamily = FontFamily.Monospace, fontSize = 10.sp
                )
                Text("─── recompositions ───", color = gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                PerfMonitor.recompositions.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            entry.key, color = fg,
                            fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                            modifier = Modifier.width(160.dp)
                        )
                        Text(
                            entry.value.toString(),
                            color = if (entry.value > 100) warn else fg,
                            fontFamily = FontFamily.Monospace, fontSize = 9.sp
                        )
                    }
                }
            } else {
                Text("▸ tap for details", color = gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
        }
    }
}

private fun fmt0(v: Float): String = ((v * 10f).toInt() / 10f).toString()
private fun fmt1(v: Float): String = ((v * 10f).toInt() / 10f).toString()
private fun fmt2(v: Float): String = ((v * 100f).toInt() / 100f).toString()
