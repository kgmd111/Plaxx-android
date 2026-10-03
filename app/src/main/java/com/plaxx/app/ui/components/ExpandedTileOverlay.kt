package com.plaxx.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plaxx.app.data.api.HomeworkDto
import com.plaxx.app.ui.viewmodels.CustomEventsViewModel
import com.plaxx.app.ui.viewmodels.HomeworkViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.graphicsLayer
private val EASING = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private const val DURATION_MS = 340
private const val DURATION_CLOSE_MS = 280

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pl"))
private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl"))
private val hwDueFmt = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.forLanguageTag("pl"))

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun lerpOff(a: Offset, b: Offset, t: Float) = Offset(lerp(a.x, b.x, t), lerp(a.y, b.y, t))
private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    a.red + (b.red - a.red) * t,
    a.green + (b.green - a.green) * t,
    a.blue + (b.blue - a.blue) * t,
    a.alpha + (b.alpha - a.alpha) * t
)

@Composable
fun ExpandedTileOverlay(
    data: TileMorphData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    homeworkViewModel: HomeworkViewModel? = null,
    customEventsViewModel: CustomEventsViewModel? = null,
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenW = with(density) { config.screenWidthDp.dp.toPx() }
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }

    val padH = with(density) { 20.dp.toPx() }
    val headerH = with(density) { 64.dp.toPx() }
    val bodyPadTop = with(density) { 8.dp.toPx() }
    val rowH = with(density) { 44.dp.toPx() }
    val labelColW = with(density) { 130.dp.toPx() }
    val sidePad = with(density) { 16.dp.toPx() }
    val descTopPad = with(density) { 4.dp.toPx() }

    val targetW = minOf(with(density) { 480.dp.toPx() }, screenW - sidePad * 2)
    val targetH = minOf(with(density) { 520.dp.toPx() }, screenH * 0.85f)
    val targetL = (screenW - targetW) / 2f
    val targetT = (screenH - targetH) / 2f

    val subjectTargetAnchor = Offset(targetL + padH, targetT + headerH / 2f)
    val bodyTop = targetT + headerH + bodyPadTop
    fun rowCenterY(i: Int) = bodyTop + i * rowH + rowH / 2f
    val labelX = targetL + padH
    val valueX = targetL + padH + labelColW

    val groupLabelAnchor   = Offset(labelX, rowCenterY(0))
    val teacherLabelAnchor = Offset(labelX, rowCenterY(1))
    val roomLabelAnchor    = Offset(labelX, rowCenterY(2))
    val timeLabelAnchor    = Offset(labelX, rowCenterY(3))
    val dateLabelAnchor    = Offset(labelX, rowCenterY(4))
    val groupValueAnchor   = Offset(valueX, rowCenterY(0))
    val teacherValueAnchor = Offset(valueX, rowCenterY(1))
    val roomValueAnchor    = Offset(valueX, rowCenterY(2))
    val timeValueAnchor    = Offset(valueX, rowCenterY(3))
    val dateValueAnchor    = Offset(valueX, rowCenterY(4))

    val descRowTop = bodyTop + 5 * rowH
    val descLabelAnchor = Offset(labelX, descRowTop + rowH / 2f)
    val descValueAnchor = Offset(valueX, descRowTop + descTopPad)

    val hwSectionTop = descRowTop + rowH + with(density) { 12.dp.toPx() }

    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ==== Homework ====
    val allHw = homeworkViewModel?.items?.collectAsState()?.value ?: emptyList()
    val hwForThisEvent: List<HomeworkDto> = remember(allHw, data.event) {
        val remote = data.event.remoteId
        if (data.event.isCustom) {
            if (remote != null) allHw.filter { it.userEventId == remote } else emptyList()
        } else {
            val idLong = data.event.id.toLongOrNull()
            if (idLong != null) allHw.filter { it.planEventId == idLong } else emptyList()
        }
    }

    var showQuickAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(DURATION_MS, easing = EASING))
    }

    val handleDismiss: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                progress.animateTo(0f, tween(DURATION_CLOSE_MS, easing = EASING))
                onDismiss()
            }
        }
    }
    BackHandler(enabled = !closing) { handleDismiss() }

    val p = progress.value

    val bgLeft   = lerp(data.tileRect.left,   targetL, p)
    val bgTop    = lerp(data.tileRect.top,    targetT, p)
    val bgWidth  = lerp(data.tileRect.width,  targetW, p)
    val bgHeight = lerp(data.tileRect.height, targetH, p)

    val headerLeft   = lerp(data.tileRect.left,   targetL, p)
    val headerTop    = lerp(data.tileRect.top,    targetT, p)
    val headerWidth  = lerp(data.tileRect.width,  targetW, p)
    val headerHeight = lerp(data.tileRect.height, headerH, p)

    val startCorner = with(density) { 14.dp.toPx() }
    val endCorner   = with(density) { 24.dp.toPx() }
    val cornerPx = lerp(startCorner, endCorner, p)

    val headerTopCorner    = lerp(startCorner, endCorner, p)
    val headerBottomCorner = lerp(startCorner, 0f, p)
    val headerShape = RoundedCornerShape(
        topStart    = with(density) { headerTopCorner.toDp() },
        topEnd      = with(density) { headerTopCorner.toDp() },
        bottomEnd   = with(density) { headerBottomCorner.toDp() },
        bottomStart = with(density) { headerBottomCorner.toDp() }
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val bgColor = lerpColor(data.backgroundColor, surfaceColor, p)
    val borderWidthPx = with(density) { 2.dp.toPx() }
    val curBorderWidthPx = lerp(0f, borderWidthPx, p)
    val valueColor = lerpColor(data.textColor, MaterialTheme.colorScheme.onSurface, p)

    val detailsAlpha = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val detailsSlide = (1f - detailsAlpha) * with(density) { 12.dp.toPx() }

    val tileCenterY = data.tileRect.center.y

    fun fallback(text: String, fs: Float, fw: FontWeight) = ElementSnapshot(
        text = text,
        rect = Rect(data.tileRect.left, tileCenterY - 8f, data.tileRect.left + 1f, tileCenterY + 8f),
        fontSizeSp = fs, fontWeight = fw
    )

    val subjectSnap = data.subject ?: fallback(data.event.subject, 12f, FontWeight.Bold)
    val roomSnap = data.room ?: data.event.room?.takeIf { it.isNotBlank() }?.let { fallback(it, 12f, FontWeight.SemiBold) }
    val groupSnap = data.group ?: data.event.group.takeIf { it.isNotBlank() }?.let { fallback(it, 11f, FontWeight.Normal) }

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * p))
                .clickable(
                    enabled = !closing,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { handleDismiss() }
        )

        // Karta tla
        Box(
            Modifier
                .offsetPx(bgLeft, bgTop)
                .size(with(density) { bgWidth.toDp() }, with(density) { bgHeight.toDp() })
                .shadow((24f * p).dp, RoundedCornerShape(with(density) { cornerPx.toDp() }), clip = false)
                .clip(RoundedCornerShape(with(density) { cornerPx.toDp() }))
                .background(bgColor)
                .then(
                    if (curBorderWidthPx > 0.5f) {
                        Modifier.border(
                            width = with(density) { curBorderWidthPx.toDp() },
                            color = data.backgroundColor,
                            shape = RoundedCornerShape(with(density) { cornerPx.toDp() })
                        )
                    } else Modifier
                )
        )

        // Pasek nagłówka
        Box(
            Modifier
                .offsetPx(headerLeft, headerTop)
                .size(with(density) { headerWidth.toDp() }, with(density) { headerHeight.toDp() })
                .clip(headerShape)
                .background(data.backgroundColor)
        )

        // Subject
        MorphText(
            text = subjectSnap.text,
            startAnchor = Offset(subjectSnap.rect.left, subjectSnap.rect.center.y),
            endAnchor = subjectTargetAnchor,
            startFontSp = subjectSnap.fontSizeSp,
            endFontSp = 20f,
            fontWeight = FontWeight.Bold,
            color = data.textColor,
            progress = p
        )

        // Grupa + sala
        groupSnap?.let { s ->
            MorphText(s.text, Offset(s.rect.left, s.rect.center.y), groupValueAnchor,
                s.fontSizeSp, 14f, s.fontWeight, valueColor, p)
        }
        roomSnap?.let { s ->
            MorphText(s.text, Offset(s.rect.left, s.rect.center.y), roomValueAnchor,
                s.fontSizeSp, 14f, s.fontWeight, valueColor, p)
        }

        // Detale (fade-in)
        if (detailsAlpha > 0.001f) {
            // Labelki
            FadeText("👥 Grupa", groupLabelAnchor, 14f,
                MaterialTheme.colorScheme.onSurfaceVariant, FontWeight.Normal, detailsAlpha, detailsSlide)
            FadeText("👤 Prowadzący", teacherLabelAnchor, 14f,
                MaterialTheme.colorScheme.onSurfaceVariant, FontWeight.Normal, detailsAlpha, detailsSlide)
            FadeText("🚪 Sala", roomLabelAnchor, 14f,
                MaterialTheme.colorScheme.onSurfaceVariant, FontWeight.Normal, detailsAlpha, detailsSlide)
            FadeText("🕒 Godziny", timeLabelAnchor, 14f,
                MaterialTheme.colorScheme.onSurfaceVariant, FontWeight.Normal, detailsAlpha, detailsSlide)
            FadeText("📅 Data", dateLabelAnchor, 14f,
                MaterialTheme.colorScheme.onSurfaceVariant, FontWeight.Normal, detailsAlpha, detailsSlide)

            // Wartosci
            FadeText(data.event.teacher ?: "—", teacherValueAnchor, 14f,
                MaterialTheme.colorScheme.onSurface, FontWeight.SemiBold, detailsAlpha, detailsSlide)
            FadeText("${fmtTime(data.event.start)} – ${fmtTime(data.event.end)}", timeValueAnchor, 14f,
                MaterialTheme.colorScheme.onSurface, FontWeight.SemiBold, detailsAlpha, detailsSlide)
            FadeText(fmtDate(data.event.start), dateValueAnchor, 14f,
                MaterialTheme.colorScheme.onSurface, FontWeight.SemiBold, detailsAlpha, detailsSlide)

            // Opis
            data.event.desc?.takeIf { it.isNotBlank() }?.let { desc ->
                FadeText("📝 Opis", descLabelAnchor, 14f,
                    MaterialTheme.colorScheme.onSurfaceVariant, FontWeight.Normal, detailsAlpha, detailsSlide)
                FadeText(desc, descValueAnchor, 13f,
                    MaterialTheme.colorScheme.onSurface, FontWeight.Normal, detailsAlpha, detailsSlide,
                    maxLines = 4,
                    maxWidthPx = (targetW - padH - labelColW - padH).roundToInt(),
                    alignTop = true)
            }

            // Close button
            val closeSizePx = with(density) { 32.dp.toPx() }
            Box(
                Modifier
                    .offsetPx(
                        targetL + targetW - padH - closeSizePx,
                        targetT + headerH / 2f - closeSizePx / 2f
                    )
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(data.textColor.copy(alpha = 0.22f * detailsAlpha))
                    .clickable(enabled = !closing, indication = null,
                        interactionSource = remember { MutableInteractionSource() }) { handleDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = data.textColor.copy(alpha = detailsAlpha), fontSize = 14.sp)
            }
        }

        // ═══════════════════════════════════════════════════════
        // Sekcja HW (od 70% postepu)
        // ═══════════════════════════════════════════════════════
        val hwAlpha = ((p - 0.70f) / 0.30f).coerceIn(0f, 1f)
        if (hwAlpha > 0.001f && homeworkViewModel != null) {
            Box(
                Modifier
                    .offsetPx(targetL + padH, hwSectionTop)
                    .size(
                        with(density) { (targetW - padH * 2).toDp() },
                        with(density) { (targetH - (hwSectionTop - targetT) - padH).toDp() }
                    )
                    .graphicsLayerAlpha(hwAlpha)
            ) {
                Column(Modifier.fillMaxSize()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "📚 Zadania (${hwForThisEvent.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showQuickAdd = true }) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dodaj", fontSize = 13.sp)
                        }
                    }

                    if (hwForThisEvent.isEmpty()) {
                        Text(
                            "Brak zadan. Kliknij Dodaj, aby utworzyc.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        hwForThisEvent.take(4).forEach { hw ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = hw.status == "done",
                                    onCheckedChange = { homeworkViewModel.toggleStatus(hw) },
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        hw.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    hw.dueDate?.let {
                                        Text(
                                            "⏰ ${fmtHwDue(it)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { homeworkViewModel.remove(hw) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete, "Usun",
                                        Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (hwForThisEvent.size > 4) {
                            Text(
                                "+ ${hwForThisEvent.size - 4} wiecej…",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                            )
                        }
                    }

                    // Akcje custom eventu (Edytuj / Usun)
                    if (data.event.isCustom && customEventsViewModel != null) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    // Edycja custom eventu: mozna rozbudowac pozniej.
                                    // Na razie tylko info - uzytkownik otwiera FAB by dodac nowe.
                                }
                            ) {
                                Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edytuj", fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(6.dp))
                            TextButton(
                                onClick = {
                                    val rid = data.event.remoteId
                                    if (rid != null) {
                                        customEventsViewModel.remove(rid)
                                        handleDismiss()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Delete, null, Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Usun",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HomeworkQuickAddSheet
    // ═══════════════════════════════════════════════════════════
    if (showQuickAdd && homeworkViewModel != null) {
        val submitting by homeworkViewModel.loading.collectAsState()
        val err by homeworkViewModel.error.collectAsState()

        val planEventId = if (!data.event.isCustom) data.event.id.toLongOrNull() else null
        val userEventId = if (data.event.isCustom) data.event.remoteId else null

        HomeworkQuickAddSheet(
            planEventId = planEventId,
            userEventId = userEventId,
            defaultTitle = "",
            onDismiss = { showQuickAdd = false },
            onCreate = { title, desc, due ->
                homeworkViewModel.create(
                    title = title,
                    description = desc,
                    dueDate = due,
                    planEventId = planEventId,
                    userEventId = userEventId,
                    onSuccess = { showQuickAdd = false }
                )
            },
            submitting = submitting,
            errorMessage = err
        )
    }
}

// ─── Utils ─────────────────────────────────────────────────────

private fun Modifier.graphicsLayerAlpha(a: Float): Modifier =
    this.graphicsLayer { alpha = a }
private fun Modifier.offsetPx(x: Float, y: Float) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(x.roundToInt(), y.roundToInt())
        }
    }
)

@Composable
private fun MorphText(
    text: String, startAnchor: Offset, endAnchor: Offset,
    startFontSp: Float, endFontSp: Float,
    fontWeight: FontWeight, color: Color, progress: Float
) {
    val anchor = lerpOff(startAnchor, endAnchor, progress)
    val fontSp = lerp(startFontSp, endFontSp, progress)
    Layout(
        content = {
            Text(text, color = color, fontSize = fontSp.sp, fontWeight = fontWeight,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        },
        modifier = Modifier.fillMaxSize()
    ) { measurables, constraints ->
        val placeable = measurables[0].measure(Constraints())
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(anchor.x.roundToInt(), (anchor.y - placeable.height / 2f).roundToInt())
        }
    }
}

@Composable
private fun FadeText(
    text: String, anchor: Offset, fontSizeSp: Float, color: Color,
    fontWeight: FontWeight, alpha: Float, slideY: Float,
    maxLines: Int = 1, maxWidthPx: Int = Int.MAX_VALUE, alignTop: Boolean = false
) {
    Layout(
        content = {
            Text(text, color = color.copy(alpha = color.alpha * alpha),
                fontSize = fontSizeSp.sp, fontWeight = fontWeight,
                maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        },
        modifier = Modifier.fillMaxSize()
    ) { measurables, constraints ->
        val c = if (maxWidthPx == Int.MAX_VALUE) Constraints() else Constraints(maxWidth = maxWidthPx)
        val placeable = measurables[0].measure(c)
        layout(constraints.maxWidth, constraints.maxHeight) {
            val y = if (alignTop) (anchor.y + slideY).roundToInt()
                    else (anchor.y + slideY - placeable.height / 2f).roundToInt()
            placeable.place(anchor.x.roundToInt(), y)
        }
    }
}

private fun fmtTime(iso: String): String = runCatching {
    LocalDateTime.parse(iso).format(timeFormatter)
}.getOrDefault("--:--")

private fun fmtDate(iso: String): String = runCatching {
    LocalDateTime.parse(iso).format(dateFormatter)
}.getOrDefault("--")

private fun fmtHwDue(iso: String): String = runCatching {
    LocalDateTime.parse(iso.replace(' ', 'T')).format(hwDueFmt)
}.getOrDefault(iso)
