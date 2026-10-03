package com.plaxx.app.ui.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PerfMonitor {

    var enabled by mutableStateOf(true)

    private var lastFrameTime = 0L
    private val frameTimes = ArrayDeque<Long>()

    var currentFps by mutableFloatStateOf(0f); private set
    var avgFps by mutableFloatStateOf(0f); private set
    var worstFrameMs by mutableFloatStateOf(0f); private set

    private val physicsTimes = ArrayDeque<Float>()
    var physicsMs by mutableFloatStateOf(0f); private set
    var physicsAvgMs by mutableFloatStateOf(0f); private set

    var snapshotCommits by mutableIntStateOf(0); private set
    private var snapshotCountFrame = 0

    private val recompositionCounter = HashMap<String, Int>()
    var recompositions by mutableStateOf<Map<String, Int>>(emptyMap()); private set
    private var recompositionDirty = false

    var activeTiles by mutableIntStateOf(0); private set
    var renderedTiles by mutableIntStateOf(0); private set

    fun countRecomposition(tag: String) {
        if (!enabled) return
        recompositionCounter[tag] = (recompositionCounter[tag] ?: 0) + 1
        recompositionDirty = true
    }

    fun countSnapshotCommit() { if (enabled) snapshotCountFrame++ }
    fun updateActiveTiles(n: Int) { activeTiles = n }
    fun updateRenderedTiles(n: Int) { renderedTiles = n }

    fun beginFrame(now: Long) {
        if (!enabled) return
        if (lastFrameTime != 0L) {
            val dtMs = (now - lastFrameTime) / 1_000_000f
            frameTimes.addLast(dtMs.toLong())
            if (frameTimes.size > 120) frameTimes.removeFirst()
            if (frameTimes.size >= 2) {
                val sum = frameTimes.sum()
                avgFps = 1000f * frameTimes.size / sum
                currentFps = if (dtMs > 0f) 1000f / dtMs else 0f
                worstFrameMs = frameTimes.max().toFloat()
            }
            if (dtMs > 33f) {
                PerfLog.w("JANK frame dt=" + String.format("%.1f", dtMs) + "ms")
            }
        }
        lastFrameTime = now
    }

    fun recordPhysicsTime(ms: Float) {
        if (!enabled) return
        physicsMs = ms
        physicsTimes.addLast(ms)
        if (physicsTimes.size > 120) physicsTimes.removeFirst()
        physicsAvgMs = physicsTimes.average().toFloat()
    }

    fun endFrame() {
        if (!enabled) return
        snapshotCommits = snapshotCountFrame
        snapshotCountFrame = 0
        if (recompositionDirty) {
            recompositions = HashMap(recompositionCounter)
            recompositionDirty = false
        }
    }
}
