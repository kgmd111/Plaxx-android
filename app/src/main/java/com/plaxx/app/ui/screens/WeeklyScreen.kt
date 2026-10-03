package com.plaxx.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.plaxx.app.data.models.EventDto
import com.plaxx.app.ui.components.EventTile
import com.plaxx.app.ui.components.TileMorphData
import com.plaxx.app.ui.debug.PerfMonitor
import com.plaxx.app.ui.debug.PerfOverlay
import com.plaxx.app.ui.viewmodels.ScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

private const val WSTART_HOUR = 7
private const val WEND_HOUR = 23
private const val WTOTAL_HOURS = WEND_HOUR - WSTART_HOUR
private val WHOUR_HEIGHT = 60.dp
private val WHOUR_AXIS_WIDTH = 56.dp
private const val WEEK_DAYS = 5

private const val W_COMMIT_TRIGGER_FRACTION = 0.15f
private const val W_CLAMP_FRACTION = 0.75f
private const val W_FLING_VELOCITY_PX_PER_SEC = 700f
private const val W_FLING_MIN_DISPLACEMENT_PX = 10f

private val dayShortNames = mapOf(
    DayOfWeek.MONDAY to "Pon",
    DayOfWeek.TUESDAY to "Wt",
    DayOfWeek.WEDNESDAY to "Sr",
    DayOfWeek.THURSDAY to "Czw",
    DayOfWeek.FRIDAY to "Pt",
    DayOfWeek.SATURDAY to "Sob",
    DayOfWeek.SUNDAY to "Ndz"
)

private data class ParsedWeekEvent(
    val event: EventDto,
    val weekStartEpoch: Long,
    val dayIdx: Int,
    val topPx: Float,
    val heightPx: Float,
    val centerYPx: Float,
    val subColumnIdx: Int,
    val totalSubColumns: Int
)

@Composable
fun WeeklyScreen(
    viewModel: ScheduleViewModel,
    expandedEventId: String? = null,
    onTileExpand: (TileMorphData) -> Unit = {},
    homeworkViewModel: com.plaxx.app.ui.viewmodels.HomeworkViewModel? = null,
    customEventsViewModel: com.plaxx.app.ui.viewmodels.CustomEventsViewModel? = null,
    colorsViewModel: com.plaxx.app.ui.viewmodels.ColorsViewModel? = null
) {
    PerfMonitor.countRecomposition("WeeklyScreen")

    val state by viewModel.state.collectAsState()

    if (!state.isAuthenticated) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nie zalogowano")
        }
        return
    }

    val visibleEvents = remember(state.events, state.selected) {
        state.events.filter { it.logicalId in state.selected }
    }

    val customEvtsState = customEventsViewModel?.items?.collectAsState()?.value ?: emptyList()
    val customEvents = remember(customEvtsState) {
        customEvtsState.map { it.toInternalEventDtoPublic() }
    }
    val allEvents = remember(visibleEvents, customEvents) {
        visibleEvents + customEvents
    }

    var showCustomEventSheet by remember { mutableStateOf(false) }

    val weekStart = remember(state.selectedDate) {
        state.selectedDate.with(DayOfWeek.MONDAY)
    }
    val days = remember(weekStart) {
        (0 until WEEK_DAYS).map { weekStart.plusDays(it.toLong()) }
    }

    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pl"))
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectDate(state.selectedDate.minusWeeks(1)) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, "Poprzedni tydzien")
            }
            Text(
                text = "${weekStart.format(formatter)} - " +
                        weekStart.plusDays((WEEK_DAYS - 1).toLong()).format(formatter),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = { viewModel.selectDate(state.selectedDate.plusWeeks(1)) }) {
                Icon(Icons.Filled.KeyboardArrowRight, "Nastepny tydzien")
            }
        }

        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(WHOUR_AXIS_WIDTH))
            days.forEach { d ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        dayShortNames[d.dayOfWeek] ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        HorizontalDivider()

        WeekDominoPager(
            currentWeekStart = weekStart,
            events = allEvents,
            onWeekChange = { newWeekStart ->
                val dow = state.selectedDate.dayOfWeek
                viewModel.selectDate(newWeekStart.with(dow))
            },
            expandedEventId = expandedEventId,
            onTileExpand = onTileExpand,
            homeworkCounts = homeworkViewModel
                ?.items?.collectAsState()?.value
                ?.let { list ->
                    list.filter { it.planEventId != null && it.status != "done" }
                        .groupingBy { it.planEventId!! }
                        .eachCount()
                } ?: emptyMap(),
            colorsMap = colorsViewModel?.colors?.collectAsState()?.value ?: emptyMap(),
            modifier = Modifier.fillMaxSize()
        )
    }

    Box(Modifier.fillMaxSize()) {
        PerfOverlay(Modifier.align(Alignment.TopEnd))

        FloatingActionButton(
            onClick = { showCustomEventSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Dodaj wydarzenie")
        }
    }

    if (showCustomEventSheet && customEventsViewModel != null) {
        val submitting by customEventsViewModel.loading.collectAsState()
        val errorMsg by customEventsViewModel.error.collectAsState()

        com.plaxx.app.ui.components.CustomEventSheet(
            initialDate = state.selectedDate,
            initialHour = 12,
            onDismiss = { showCustomEventSheet = false },
            onCreate = { title, desc, loc, hex, start, end ->
                customEventsViewModel.create(
                    title = title,
                    description = desc,
                    location = loc,
                    colorHex = hex,
                    start = start,
                    end = end,
                    onSuccess = { showCustomEventSheet = false }
                )
            },
            submitting = submitting,
            errorMessage = errorMsg
        )
    }
}

// ===== Helper (identyczny jak w DailyScreen) =====
private fun com.plaxx.app.data.api.UserEventDto.toInternalEventDtoPublic():
        com.plaxx.app.data.models.EventDto {
    val startIso = runCatching {
        java.time.LocalDateTime.parse(startDatetime.replace(' ', 'T')).toString()
    }.getOrDefault(startDatetime)
    val endIso = runCatching {
        java.time.LocalDateTime.parse(endDatetime.replace(' ', 'T')).toString()
    }.getOrDefault(endDatetime)
    val day = runCatching {
        java.time.LocalDateTime.parse(startDatetime.replace(' ', 'T')).dayOfWeek.value
    }.getOrDefault(1)
    return com.plaxx.app.data.models.EventDto(
        subject = title,
        group = "",
        teacher = null,
        room = location,
        desc = description,
        start = startIso,
        end = endIso,
        day = day,
        id = "custom-$id",
        logicalId = "custom:$id",
        isCustom = true,
        colorHexOverride = colorHex,
        remoteId = id
    )
}


@Composable
private fun WeekDominoPager(
    currentWeekStart: LocalDate,
    events: List<EventDto>,
    onWeekChange: (LocalDate) -> Unit,
    expandedEventId: String? = null,
    onTileExpand: (TileMorphData) -> Unit = {},
    homeworkCounts: Map<Long, Int> = emptyMap(),
    colorsMap: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    PerfMonitor.countRecomposition("WeekDominoPager")

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentOnWeekChange by rememberUpdatedState(onWeekChange)
    val localWeekStart = remember { mutableStateOf(currentWeekStart) }

    val fingerX = remember { mutableFloatStateOf(0f) }
    val touchYContent = remember { mutableFloatStateOf(-1f) }
    val layerWidth = remember { mutableFloatStateOf(0f) }
    var animJob by remember { mutableStateOf<Job?>(null) }

    val scrollState = rememberScrollState()

    val tileOffsets = remember { mutableStateMapOf<String, Float>() }
    val tileVelocities = remember { mutableStateMapOf<String, Float>() }

    val falloffPx = with(density) { 320.dp.toPx() }
    val stiffnessMin = 260f
    val stiffnessMax = 3200f

    var allTiles by remember { mutableStateOf<List<ParsedWeekEvent>>(emptyList()) }

    LaunchedEffect(events) {
        withContext(Dispatchers.Default) {
            val byDate = events.groupBy { ev ->
                runCatching { LocalDateTime.parse(ev.start).toLocalDate() }.getOrNull()
            }

            val columnInfo = mutableMapOf<String, Pair<Int, Int>>()

            byDate.forEach { (date, dayEvents) ->
                if (date == null) return@forEach

                val sorted = dayEvents
                    .filter { runCatching { LocalDateTime.parse(it.start) }.isSuccess }
                    .sortedBy { it.start }

                val clusters = mutableListOf<MutableList<EventDto>>()
                for (ev in sorted) {
                    val start = LocalDateTime.parse(ev.start)
                    val last = clusters.lastOrNull()
                    val overlapsLast = last?.any { existing ->
                        runCatching { LocalDateTime.parse(existing.end) }
                            .getOrNull()?.isAfter(start) == true
                    } == true
                    if (overlapsLast) last!!.add(ev) else clusters.add(mutableListOf(ev))
                }

                clusters.forEach { cluster ->
                    val cols = mutableListOf<MutableList<EventDto>>()
                    val colMap = mutableMapOf<String, Int>()

                    for (ev in cluster) {
                        val start = LocalDateTime.parse(ev.start)
                        var placed = false
                        for ((i, col) in cols.withIndex()) {
                            val lastEnd = LocalDateTime.parse(col.last().end)
                            if (!lastEnd.isAfter(start)) {
                                col.add(ev)
                                colMap[ev.id] = i
                                placed = true
                                break
                            }
                        }
                        if (!placed) {
                            cols.add(mutableListOf(ev))
                            colMap[ev.id] = cols.size - 1
                        }
                    }

                    val total = cols.size
                    cluster.forEach { ev ->
                        columnInfo[ev.id] = (colMap[ev.id] ?: 0) to total
                    }
                }
            }

            val tiles = events.mapNotNull { ev ->
                val startLdt = runCatching { LocalDateTime.parse(ev.start) }.getOrNull()
                    ?: return@mapNotNull null
                val endLdt = runCatching { LocalDateTime.parse(ev.end) }.getOrNull()
                    ?: return@mapNotNull null

                val date = startLdt.toLocalDate()
                val dayIdx = date.dayOfWeek.value - 1
                if (dayIdx !in 0 until WEEK_DAYS) return@mapNotNull null

                val topMin = (startLdt.hour - WSTART_HOUR) * 60 + startLdt.minute
                val durMin = ChronoUnit.MINUTES.between(startLdt, endLdt).toInt()
                if (topMin < 0 || durMin <= 0) return@mapNotNull null

                val topPx = with(density) { topMin.dp.toPx() }
                val heightPx = with(density) { durMin.dp.toPx() }
                val centerYPx = topPx + heightPx / 2f
                val (ci, tc) = columnInfo[ev.id] ?: (0 to 1)

                ParsedWeekEvent(
                    event = ev,
                    weekStartEpoch = date.with(DayOfWeek.MONDAY).toEpochDay(),
                    dayIdx = dayIdx,
                    topPx = topPx,
                    heightPx = heightPx,
                    centerYPx = centerYPx,
                    subColumnIdx = ci,
                    totalSubColumns = tc
                )
            }
            allTiles = tiles
            PerfMonitor.updateRenderedTiles(tiles.size)
        }
    }

    val allTilesState = rememberUpdatedState(allTiles)

    LaunchedEffect(currentWeekStart) {
        val ld = localWeekStart.value
        if (currentWeekStart != ld) {
            val lw = layerWidth.floatValue
            if (lw > 0f) {
                val weeksDiff = ChronoUnit.WEEKS.between(ld, currentWeekStart).toInt()
                val shift = weeksDiff * lw
                Snapshot.withMutableSnapshot {
                    tileOffsets.keys.toList().forEach { k ->
                        val v = tileOffsets[k] ?: 0f
                        if (v.isFinite()) tileOffsets[k] = v + shift
                    }
                    localWeekStart.value = currentWeekStart
                }
            } else {
                localWeekStart.value = currentWeekStart
            }
        }
    }

    LaunchedEffect(events) {
        val validIds = events.map { it.id }.toSet()
        tileOffsets.keys.retainAll(validIds)
        tileVelocities.keys.retainAll(validIds)
    }

    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { now ->
                val frameStartNs = System.nanoTime()
                PerfMonitor.beginFrame(now)

                val dt = if (lastTime == 0L) 0.016f
                else ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                val target = fingerX.floatValue.takeIf { it.isFinite() } ?: 0f
                val touchY = touchYContent.floatValue
                val curWeekEpoch = localWeekStart.value.toEpochDay()
                val lw = layerWidth.floatValue.takeIf { it > 0f } ?: 1f
                val tiles = allTilesState.value

                val continuousOffset = target / lw

                val maxSubStep = 0.004f
                val subSteps = maxOf(1, (dt / maxSubStep).toInt() + 1)
                val subDt = dt / subSteps

                var changed = false

                tiles.forEach { pe ->
                    val weekDiff = ((pe.weekStartEpoch - curWeekEpoch) / 7L).toInt()
                    if (weekDiff < -1 || weekDiff > 1) return@forEach

                    val dist = if (touchY >= 0f) abs(pe.centerYPx - touchY) else 0f
                    val t = (dist / falloffPx).coerceIn(0f, 1f)
                    val smooth = t * t * (3f - 2f * t)
                    val closeness = 1f - smooth

                    val continuousWeekDistance = abs(weekDiff.toFloat() + continuousOffset)
                    val layerFactor = (1.0f - continuousWeekDistance * 0.4f).coerceIn(0.6f, 1.0f)

                    val stiffness =
                        ((stiffnessMin + (stiffnessMax - stiffnessMin) * closeness) * layerFactor)
                            .coerceAtLeast(1f)
                    val dampingRatio = 1.15f + 0.10f * closeness
                    val damping = 2f * sqrt(stiffness) * dampingRatio

                    var current = (tileOffsets[pe.event.id] ?: 0f).takeIf { it.isFinite() } ?: 0f
                    var velocity = (tileVelocities[pe.event.id] ?: 0f).takeIf { it.isFinite() } ?: 0f

                    val prevCurrent = current
                    val prevVelocity = velocity

                    repeat(subSteps) {
                        val accel = stiffness * (target - current) - damping * velocity
                        velocity += accel * subDt
                        current += velocity * subDt
                    }

                    if (!current.isFinite() || !velocity.isFinite()) {
                        current = target
                        velocity = 0f
                    }
                    if (abs(current - target) < 0.5f && abs(velocity) < 5f) {
                        current = target
                        velocity = 0f
                    }

                    if (prevCurrent != current) {
                        tileOffsets[pe.event.id] = current
                        changed = true
                    }
                    if (prevVelocity != velocity) {
                        tileVelocities[pe.event.id] = velocity
                    }
                }

                val physicsMs = (System.nanoTime() - frameStartNs) / 1_000_000f
                PerfMonitor.recordPhysicsTime(physicsMs)
                if (changed) PerfMonitor.countSnapshotCommit()

                PerfMonitor.endFrame()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHOUR_HEIGHT * WTOTAL_HOURS)
                .pointerInput(Unit) {
                    var lastEventTimeMs = 0L
                    var peakVelocity = 0f
                    var hasCommittedThisGesture = false

                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            lastEventTimeMs = 0L
                            peakVelocity = 0f
                            hasCommittedThisGesture = false
                            animJob?.cancel()
                            touchYContent.floatValue = offset.y + scrollState.value
                        },
                        onDragEnd = {
                            val lw = layerWidth.floatValue
                            if (lw > 0f && !hasCommittedThisGesture) {
                                val fx = fingerX.floatValue.takeIf { it.isFinite() } ?: 0f
                                val speed = abs(peakVelocity)
                                val posThreshold = lw * W_COMMIT_TRIGGER_FRACTION

                                val isFling = speed > W_FLING_VELOCITY_PX_PER_SEC &&
                                        abs(fx) > W_FLING_MIN_DISPLACEMENT_PX
                                val pastPosition = abs(fx) > posThreshold

                                val direction: Int = when {
                                    isFling && peakVelocity < 0f -> -1
                                    isFling && peakVelocity > 0f -> 1
                                    pastPosition && fx < 0f -> -1
                                    pastPosition && fx > 0f -> 1
                                    else -> 0
                                }

                                if (direction != 0) {
                                    val shift = if (direction < 0) lw else -lw
                                    Snapshot.withMutableSnapshot {
                                        val snap = tileOffsets.toMap()
                                        tileOffsets.clear()
                                        snap.forEach { (k, v) ->
                                            val nv = v + shift
                                            if (nv.isFinite()) tileOffsets[k] = nv
                                        }
                                        localWeekStart.value = if (direction < 0)
                                            localWeekStart.value.plusWeeks(1)
                                        else
                                            localWeekStart.value.minusWeeks(1)
                                    }
                                    tileVelocities.clear()
                                    currentOnWeekChange(localWeekStart.value)

                                    val newFx = fx + shift
                                    animJob = scope.launch {
                                        animate(
                                            initialValue = newFx,
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) { v, _ -> fingerX.floatValue = v }
                                        touchYContent.floatValue = -1f
                                    }
                                    return@detectHorizontalDragGestures
                                }
                            }

                            animJob = scope.launch {
                                animate(
                                    initialValue = fingerX.floatValue,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { v, _ -> fingerX.floatValue = v }
                                touchYContent.floatValue = -1f
                            }
                        },
                        onDragCancel = {
                            animJob = scope.launch {
                                animate(
                                    initialValue = fingerX.floatValue,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessHigh
                                    )
                                ) { v, _ -> fingerX.floatValue = v }
                                touchYContent.floatValue = -1f
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()

                            val now = change.uptimeMillis
                            if (lastEventTimeMs != 0L) {
                                val dtMs = (now - lastEventTimeMs).coerceAtLeast(1L)
                                val instantV = dragAmount / (dtMs / 1000f)
                                peakVelocity = if (abs(instantV) > abs(peakVelocity)) instantV
                                               else peakVelocity * 0.85f
                            }
                            lastEventTimeMs = now

                            val lw = layerWidth.floatValue
                            if (!lw.isFinite() || lw <= 0f) return@detectHorizontalDragGestures

                            val speed = abs(peakVelocity)
                            val dynamicTriggerFactor = when {
                                speed > 1500f -> 0.05f
                                speed > 800f  -> 0.10f
                                else          -> W_COMMIT_TRIGGER_FRACTION
                            }
                            val triggerLw = lw * dynamicTriggerFactor
                            val clampLw = lw * W_CLAMP_FRACTION

                            val current = fingerX.floatValue.takeIf { it.isFinite() } ?: 0f
                            var fx = current + dragAmount

                            if (!hasCommittedThisGesture) {
                                if (fx < -triggerLw) {
                                    Snapshot.withMutableSnapshot {
                                        val snap = tileOffsets.toMap()
                                        tileOffsets.clear()
                                        snap.forEach { (k, v) ->
                                            val nv = v + lw
                                            if (nv.isFinite()) tileOffsets[k] = nv
                                        }
                                        localWeekStart.value = localWeekStart.value.plusWeeks(1)
                                    }
                                    tileVelocities.clear()
                                    fx += lw
                                    hasCommittedThisGesture = true
                                    currentOnWeekChange(localWeekStart.value)
                                } else if (fx > triggerLw) {
                                    Snapshot.withMutableSnapshot {
                                        val snap = tileOffsets.toMap()
                                        tileOffsets.clear()
                                        snap.forEach { (k, v) ->
                                            val nv = v - lw
                                            if (nv.isFinite()) tileOffsets[k] = nv
                                        }
                                        localWeekStart.value = localWeekStart.value.minusWeeks(1)
                                    }
                                    tileVelocities.clear()
                                    fx -= lw
                                    hasCommittedThisGesture = true
                                    currentOnWeekChange(localWeekStart.value)
                                }
                            }

                            fingerX.floatValue = fx.coerceIn(-clampLw, clampLw)
                        }
                    )
                }
        ) {
            WeekHourAxis()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .clipToBounds()
                    .onSizeChanged { size ->
                        val lw = size.width.toFloat()
                        if (lw > 0f && layerWidth.floatValue != lw) {
                            layerWidth.floatValue = lw
                        }
                    }
            ) {
                val lw = layerWidth.floatValue

                if (lw > 0f) {
                    val dayW = lw / WEEK_DAYS
                    val curWeekEpoch = localWeekStart.value.toEpochDay()

                    allTiles.forEach { pe ->
                        key(pe.event.id) {
                            val subW = dayW / pe.totalSubColumns
                            val columnX = pe.dayIdx * dayW + pe.subColumnIdx * subW

                            EventTile(
                                ev = pe.event,
                                isExpanded = pe.event.id == expandedEventId,
                                homeworkCount = runCatching {
                                    homeworkCounts[pe.event.id.toLong()]
                                }.getOrNull() ?: 0,
                                subjectColorHex = colorsMap[pe.event.subject],
                                onExpandClick = onTileExpand,
                                modifier = Modifier
                                    .graphicsLayer {
                                        val weekDiff =
                                            ((pe.weekStartEpoch - curWeekEpoch) / 7L).toInt()
                                        if (weekDiff < -1 || weekDiff > 1) {
                                            alpha = 0f
                                            translationX = -1_000_000f
                                            translationY = -1_000_000f
                                            return@graphicsLayer
                                        }

                                        alpha = 1f
                                        val baseX = weekDiff.toFloat() * lw + columnX
                                        val offset = tileOffsets[pe.event.id] ?: 0f
                                        val off = if (offset.isFinite()) offset else 0f
                                        translationX = baseX + off
                                        translationY = pe.topPx
                                    }
                                    .width(with(density) { subW.toDp() })
                                    .height(with(density) { pe.heightPx.toDp() })
                                    .padding(horizontal = 1.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHourAxis() {
    Column(
        modifier = Modifier
            .width(WHOUR_AXIS_WIDTH)
            .fillMaxHeight()
    ) {
        for (h in WSTART_HOUR until WEND_HOUR) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WHOUR_HEIGHT)
            ) {
                Text(
                    text = "$h:00",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 6.dp, top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}
