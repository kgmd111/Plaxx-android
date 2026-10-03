package com.plaxx.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plaxx.app.data.models.EventDto
import com.plaxx.app.ui.components.EventTile
import com.plaxx.app.ui.components.TileMorphData
import com.plaxx.app.ui.debug.PerfMonitor
import com.plaxx.app.ui.debug.PerfOverlay
import com.plaxx.app.ui.theme.EventColors
import com.plaxx.app.ui.viewmodels.ScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val ROW_HEIGHT = 48.dp
private val HEADER_HEIGHT = 24.dp
private val HORIZONTAL_PADDING = 4.dp
private val DAY_LABELS = listOf("Pon", "Wt", "Sr", "Czw", "Pt", "Sob", "Ndz")
private const val COLLAPSED_ROWS = 2
private const val EXPANDED_ROWS = 4
private const val EXPAND_TRIGGER_PX = 15f

private val DAY_CELL_BOX_SIZE = 30.dp
private val DAY_CELL_SPACER = 2.dp
private val DAY_CELL_DOTS_ROW = 5.dp
private val DAY_CELL_CONTENT_HEIGHT = DAY_CELL_BOX_SIZE + DAY_CELL_SPACER + DAY_CELL_DOTS_ROW
private val DAY_CELL_TOP_PADDING = (ROW_HEIGHT - DAY_CELL_CONTENT_HEIGHT) / 2
private val DAY_CELL_BOX_CENTER_Y = DAY_CELL_TOP_PADDING + DAY_CELL_BOX_SIZE / 2

// ═══ Const for DominoPager (day view) ═══
private const val START_HOUR = 5
private const val END_HOUR = 24
private const val TOTAL_HOURS = END_HOUR - START_HOUR
private val HOUR_HEIGHT = 60.dp
private val HOUR_AXIS_WIDTH = 56.dp

private const val COMMIT_TRIGGER_FRACTION = 0.15f
private const val CLAMP_FRACTION = 0.75f
private const val FLING_VELOCITY_PX_PER_SEC = 700f
private const val FLING_MIN_DISPLACEMENT_PX = 10f

private data class ParsedEvent(
    val event: EventDto,
    val startDateEpoch: Long,
    val topMin: Int,
    val durMin: Int,
    val topPx: Float,
    val heightPx: Float,
    val centerYPx: Float,
    val columnIdx: Int,
    val totalColumns: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    viewModel: ScheduleViewModel,
    calendarExpanded: Boolean = false,
    onCalendarExpandChange: (Boolean) -> Unit = {},
    onVisibleMonthChange: (String) -> Unit = {},
    expandedEventId: String? = null,
    onTileExpand: (TileMorphData) -> Unit = {},
    homeworkViewModel: com.plaxx.app.ui.viewmodels.HomeworkViewModel? = null,
    customEventsViewModel: com.plaxx.app.ui.viewmodels.CustomEventsViewModel? = null,
    colorsViewModel: com.plaxx.app.ui.viewmodels.ColorsViewModel? = null
) {
    PerfMonitor.countRecomposition("DailyScreen")

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

    // Custom events z VM -> EventDto, dolaczone do listy kafelkow.
    val customEvtsState = customEventsViewModel?.items?.collectAsState()?.value ?: emptyList()
    val customEvents = remember(customEvtsState) {
        customEvtsState.map { it.toInternalEventDtoPublic() }
    }
    val allEvents = remember(visibleEvents, customEvents) {
        visibleEvents + customEvents
    }

    var showCustomEventSheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        MiniCalendar(
            selectedDate = state.selectedDate,
            events = visibleEvents,
            expanded = calendarExpanded,
            onExpandChange = onCalendarExpandChange,
            onDateSelected = { viewModel.selectDate(it) },
            onVisibleMonthChange = onVisibleMonthChange
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        PullToRefreshBox(
            isRefreshing = state.syncing,
            onRefresh = { viewModel.syncNow() },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                DominoPager(
                    currentDate = state.selectedDate,
                    events = allEvents,
                    onDateChange = { viewModel.selectDate(it) },
                    onPlanTouched = { if (calendarExpanded) onCalendarExpandChange(false) },
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

                // Empty state — pokaz tylko gdy brak eventow w wybranym dniu
                val hasEventsForDay = allEvents.any { ev ->
                    runCatching {
                        LocalDateTime.parse(ev.start).toLocalDate()
                    }.getOrNull() == state.selectedDate
                }
                if (!hasEventsForDay) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = 120.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            "Brak zajec tego dnia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
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

// ===== Public helper - to samo co w CustomEventsViewModel, ale
// uzywamy go tutaj bez siegania do prywatnej extension. =====
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


// ═══════════════════════════════════════════════════════════════
// DominoPager
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DominoPager(
    currentDate: LocalDate,
    events: List<EventDto>,
    onDateChange: (LocalDate) -> Unit,
    expandedEventId: String? = null,
    onPlanTouched: () -> Unit = {},
    onTileExpand: (TileMorphData) -> Unit = {},
    homeworkCounts: Map<Long, Int> = emptyMap(),
    colorsMap: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    PerfMonitor.countRecomposition("DominoPager")

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentOnPlanTouched by rememberUpdatedState(onPlanTouched)
    val currentOnDateChange by rememberUpdatedState(onDateChange)
    val localDate = remember { mutableStateOf(currentDate) }

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

    var allTiles by remember { mutableStateOf<List<ParsedEvent>>(emptyList()) }

    LaunchedEffect(events) {
        withContext(Dispatchers.Default) {
            val byDate = events.groupBy { ev ->
                runCatching { LocalDateTime.parse(ev.start).toLocalDate() }.getOrNull()
            }

            val columnInfo = mutableMapOf<String, Pair<Int, Int>>()

            byDate.forEach { (_, dayEvents) ->
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
                val startLdt = runCatching { LocalDateTime.parse(ev.start) }.getOrNull() ?: return@mapNotNull null
                val endLdt = runCatching { LocalDateTime.parse(ev.end) }.getOrNull() ?: return@mapNotNull null
                val topMin = (startLdt.hour - START_HOUR) * 60 + startLdt.minute
                val durMin = ChronoUnit.MINUTES.between(startLdt, endLdt).toInt()
                if (topMin < 0 || durMin <= 0) return@mapNotNull null

                val topPx = with(density) { topMin.dp.toPx() }
                val heightPx = with(density) { durMin.dp.toPx() }
                val centerYPx = topPx + heightPx / 2f
                val (ci, tc) = columnInfo[ev.id] ?: (0 to 1)

                ParsedEvent(
                    event = ev,
                    startDateEpoch = startLdt.toLocalDate().toEpochDay(),
                    topMin = topMin,
                    durMin = durMin,
                    topPx = topPx,
                    heightPx = heightPx,
                    centerYPx = centerYPx,
                    columnIdx = ci,
                    totalColumns = tc
                )
            }
            allTiles = tiles
            PerfMonitor.updateRenderedTiles(tiles.size)
        }
    }

    val allTilesState = rememberUpdatedState(allTiles)

    LaunchedEffect(currentDate) {
        val ld = localDate.value
        if (currentDate != ld) {
            val lw = layerWidth.floatValue
            if (lw > 0f) {
                val daysDiff = ChronoUnit.DAYS.between(ld, currentDate).toInt()
                val shift = daysDiff * lw
                Snapshot.withMutableSnapshot {
                    tileOffsets.keys.toList().forEach { k ->
                        val v = tileOffsets[k] ?: 0f
                        if (v.isFinite()) tileOffsets[k] = v + shift
                    }
                    localDate.value = currentDate
                }
            } else {
                localDate.value = currentDate
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
                val curDate = localDate.value.toEpochDay()
                val lw = layerWidth.floatValue.takeIf { it > 0f } ?: 1f
                val tiles = allTilesState.value

                val continuousOffset = target / lw

                val maxSubStep = 0.004f
                val subSteps = maxOf(1, (dt / maxSubStep).toInt() + 1)
                val subDt = dt / subSteps

                var changed = false

                tiles.forEach { pe ->
                    val dayDiff = pe.startDateEpoch - curDate
                    if (dayDiff < -2L || dayDiff > 2L) return@forEach

                    val dist = if (touchY >= 0f) abs(pe.centerYPx - touchY) else 0f
                    val t = (dist / falloffPx).coerceIn(0f, 1f)
                    val smooth = t * t * (3f - 2f * t)
                    val closeness = 1f - smooth

                    val continuousDayDistance = abs(dayDiff.toFloat() + continuousOffset)
                    val layerFactor = (1.0f - continuousDayDistance * 0.4f).coerceIn(0.6f, 1.0f)

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
                .height(HOUR_HEIGHT * TOTAL_HOURS)
                .pointerInput(Unit) {
                    var lastEventTimeMs = 0L
                    var peakVelocity = 0f
                    var hasCommittedThisGesture = false

                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            currentOnPlanTouched()
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
                                val posThreshold = lw * COMMIT_TRIGGER_FRACTION

                                val isFling = speed > FLING_VELOCITY_PX_PER_SEC &&
                                        abs(fx) > FLING_MIN_DISPLACEMENT_PX
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
                                        localDate.value = if (direction < 0)
                                            localDate.value.plusDays(1)
                                        else
                                            localDate.value.minusDays(1)
                                    }
                                    tileVelocities.clear()

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

                                    currentOnDateChange(localDate.value)
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
                                else          -> COMMIT_TRIGGER_FRACTION
                            }
                            val triggerLw = lw * dynamicTriggerFactor
                            val clampLw = lw * CLAMP_FRACTION

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
                                        localDate.value = localDate.value.plusDays(1)
                                    }
                                    tileVelocities.clear()
                                    fx += lw
                                    hasCommittedThisGesture = true
                                    currentOnDateChange(localDate.value)
                                } else if (fx > triggerLw) {
                                    Snapshot.withMutableSnapshot {
                                        val snap = tileOffsets.toMap()
                                        tileOffsets.clear()
                                        snap.forEach { (k, v) ->
                                            val nv = v - lw
                                            if (nv.isFinite()) tileOffsets[k] = nv
                                        }
                                        localDate.value = localDate.value.minusDays(1)
                                    }
                                    tileVelocities.clear()
                                    fx -= lw
                                    hasCommittedThisGesture = true
                                    currentOnDateChange(localDate.value)
                                }
                            }

                            fingerX.floatValue = fx.coerceIn(-clampLw, clampLw)
                        }
                    )
                }
        ) {
            HourAxis()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            currentOnPlanTouched()
                        }
                    }
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
                    allTiles.forEach { pe ->
                        key(pe.event.id) {
                            val columnW = lw / pe.totalColumns
                            val columnX = pe.columnIdx * columnW

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
                                        val curEpoch = localDate.value.toEpochDay()
                                        val dayDiff = pe.startDateEpoch - curEpoch

                                        if (dayDiff < -2L || dayDiff > 2L) {
                                            alpha = 0f
                                            translationX = -1_000_000f
                                            translationY = -1_000_000f
                                            return@graphicsLayer
                                        }

                                        alpha = 1f
                                        val baseX = dayDiff.toFloat() * lw + columnX
                                        val offset = tileOffsets[pe.event.id] ?: 0f
                                        val off = if (offset.isFinite()) offset else 0f
                                        translationX = baseX + off
                                        translationY = pe.topPx
                                    }
                                    .width(with(density) { columnW.toDp() })
                                    .height(with(density) { pe.heightPx.toDp() })
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourAxis() {
    Column(
        modifier = Modifier
            .width(HOUR_AXIS_WIDTH)
            .fillMaxHeight()
    ) {
        for (h in START_HOUR until END_HOUR) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HOUR_HEIGHT)
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

// ═══════════════════════════════════════════════════════════════
// MiniCalendar
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MiniCalendar(
    selectedDate: LocalDate,
    events: List<EventDto>,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onVisibleMonthChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val today = remember { LocalDate.now() }

    val dayEventColors = remember(events) {
        val map = mutableMapOf<LocalDate, List<Color>>()
        events.forEach { ev ->
            val date = runCatching {
                LocalDateTime.parse(ev.start).toLocalDate()
            }.getOrNull() ?: return@forEach
            val color = EventColors.bgFor(ev.subject)
            val existing = map[date] ?: emptyList()
            map[date] = (existing + color).distinct().take(3)
        }
        map
    }

    val rowHeightPx = with(density) { ROW_HEIGHT.toPx() }
    val headerPx = with(density) { HEADER_HEIGHT.toPx() }
    val collapsedPx = headerPx + rowHeightPx * COLLAPSED_ROWS
    val expandedPx = headerPx + rowHeightPx * EXPANDED_ROWS

    val currentHeightPx by animateFloatAsState(
        targetValue = if (expanded) expandedPx else collapsedPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "calendarHeight"
    )

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()

    val baseWeekStart = remember { LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(26) }
    val totalWeeks = 53

    val selectedWeekIdx = remember(selectedDate) {
        val selWeekStart = selectedDate.with(DayOfWeek.MONDAY)
        ChronoUnit.WEEKS.between(baseWeekStart, selWeekStart).toInt()
            .coerceIn(0, totalWeeks - 1)
    }

    LaunchedEffect(selectedWeekIdx, currentHeightPx) {
        val visibleRows = ((currentHeightPx - headerPx) / rowHeightPx).toInt().coerceAtLeast(1)
        val targetIdx = (selectedWeekIdx - (visibleRows / 2) + 1)
            .coerceIn(0, totalWeeks - 1)
        val targetPx = (targetIdx * rowHeightPx).roundToInt()
        scrollState.animateScrollTo(targetPx)
    }

    LaunchedEffect(scrollState.value, currentHeightPx) {
        val visibleRows = ((currentHeightPx - headerPx) / rowHeightPx).roundToInt().coerceAtLeast(1)
        val firstIdx = (scrollState.value / rowHeightPx).roundToInt().coerceIn(0, totalWeeks - 1)
        val lastIdx = (firstIdx + visibleRows - 1).coerceIn(0, totalWeeks - 1)
        val firstDate = baseWeekStart.plusWeeks(firstIdx.toLong())
        val lastDate = baseWeekStart.plusWeeks(lastIdx.toLong()).plusDays(6)
        val label = buildMonthLabel(YearMonth.from(firstDate), YearMonth.from(lastDate))
        onVisibleMonthChange(label)
    }

    val expandDragModifier = if (!expanded) {
        Modifier.pointerInput(Unit) {
            var totalDy = 0f
            var triggered = false
            detectVerticalDragGestures(
                onDragStart = {
                    totalDy = 0f
                    triggered = false
                },
                onVerticalDrag = { change, dy ->
                    change.consume()
                    if (triggered) return@detectVerticalDragGestures
                    totalDy += dy
                    if (abs(totalDy) > EXPAND_TRIGGER_PX) {
                        triggered = true
                        onExpandChange(true)
                    }
                },
                onDragEnd = { },
                onDragCancel = { }
            )
        }
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { currentHeightPx.toDp() })
            .background(MaterialTheme.colorScheme.surface)
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .then(expandDragModifier)
    ) {
        SelectedIndicator(
            selectedDate = selectedDate,
            baseWeekStart = baseWeekStart,
            scrollStateValue = scrollState.value,
            headerPx = headerPx,
            rowHeightPx = rowHeightPx,
            containerWidthPx = containerWidthPx,
            horizontalPaddingPx = with(density) { HORIZONTAL_PADDING.toPx() },
            density = density
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HEADER_HEIGHT)
                    .padding(horizontal = HORIZONTAL_PADDING)
            ) {
                DAY_LABELS.forEach { label ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState, enabled = expanded)
            ) {
                for (idx in 0 until totalWeeks) {
                    val weekStart = baseWeekStart.plusWeeks(idx.toLong())
                    WeekRow(
                        weekStart = weekStart,
                        selectedDate = selectedDate,
                        today = today,
                        dayEventColors = dayEventColors,
                        onDateSelected = onDateSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekRow(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    dayEventColors: Map<LocalDate, List<Color>>,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .padding(horizontal = HORIZONTAL_PADDING)
    ) {
        for (dayOffset in 0..6) {
            val date = weekStart.plusDays(dayOffset.toLong())
            DayCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                dots = dayEventColors[date] ?: emptyList(),
                modifier = Modifier.weight(1f),
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    dots: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(DAY_CELL_BOX_SIZE),
            contentAlignment = Alignment.Center
        ) {
            if (isToday && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(DAY_CELL_BOX_SIZE)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(DAY_CELL_SPACER))

        Row(
            modifier = Modifier.height(DAY_CELL_DOTS_ROW),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            dots.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun SelectedIndicator(
    selectedDate: LocalDate,
    baseWeekStart: LocalDate,
    scrollStateValue: Int,
    headerPx: Float,
    rowHeightPx: Float,
    containerWidthPx: Float,
    horizontalPaddingPx: Float,
    density: Density
) {
    if (containerWidthPx <= 0f) return

    val dayOfWeekIdx = selectedDate.dayOfWeek.value - 1
    val colWidth = (containerWidthPx - horizontalPaddingPx * 2f) / 7f
    val targetX = horizontalPaddingPx + colWidth * (dayOfWeekIdx + 0.5f)

    val weeksSinceBase = ChronoUnit.WEEKS.between(
        baseWeekStart,
        selectedDate.with(DayOfWeek.MONDAY)
    ).toInt()

    val targetLocalY = headerPx + weeksSinceBase * rowHeightPx +
            with(density) { DAY_CELL_BOX_CENTER_Y.toPx() }

    val radiusPx = with(density) { DAY_CELL_BOX_SIZE.toPx() / 2f }

    val indicatorX = remember { Animatable(targetX) }
    val indicatorLocalY = remember { Animatable(targetLocalY) }

    LaunchedEffect(targetX, targetLocalY) {
        coroutineScope {
            launch {
                indicatorX.animateTo(targetX, spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ))
            }
            launch {
                indicatorLocalY.animateTo(targetLocalY, spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ))
            }
        }
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (indicatorX.value - radiusPx).roundToInt(),
                    (indicatorLocalY.value - scrollStateValue - radiusPx).roundToInt()
                )
            }
            .size(DAY_CELL_BOX_SIZE)
            .graphicsLayer {
                val vx = indicatorX.velocity
                val vy = indicatorLocalY.velocity
                val speed = sqrt(vx * vx + vy * vy)
                val stretch = (speed / 4000f).coerceIn(0f, 0.55f)
                val angle = if (speed > 1f) atan2(vy, vx) else 0f
                rotationZ = angle * 180f / PI.toFloat()
                scaleX = 1f + stretch
                scaleY = 1f - stretch * 0.55f
                transformOrigin = TransformOrigin.Center
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}

private fun buildMonthLabel(first: YearMonth, last: YearMonth): String {
    val locale = Locale("pl")
    val m1 = first.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.uppercase() }
    val m2 = last.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.uppercase() }

    return when {
        first == last -> "$m1 ${first.year}"
        first.year == last.year -> "$m1 / $m2 ${first.year}"
        else -> "$m1 ${first.year} / $m2 ${last.year}"
    }
}
