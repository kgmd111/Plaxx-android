package com.plaxx.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plaxx.app.data.models.EventDto
import com.plaxx.app.ui.theme.EventColors

private val FULL_MIN_HEIGHT = 58.dp
private val MEDIUM_MIN_HEIGHT = 34.dp
private val COMPACT_MIN_HEIGHT = 20.dp

object TileFontSizes {
    const val SUBJECT_FULL = 14f
    const val SUBJECT_MEDIUM = 13f
    const val SUBJECT_COMPACT = 12f
    const val ROOM_FULL = 12f
    const val ROOM_MEDIUM = 12f
    const val ROOM_COMPACT = 11f
    const val GROUP_FULL = 11f
    const val GROUP_MEDIUM = 11f
}

enum class TileContentMode { FULL, MEDIUM, COMPACT, ROOM_ONLY, TINY }

data class ElementSnapshot(
    val text: String,
    val rect: Rect,
    val fontSizeSp: Float,
    val fontWeight: FontWeight
)

data class TileMorphData(
    val event: EventDto,
    val backgroundColor: Color,
    val textColor: Color,
    val tileRect: Rect,
    val subject: ElementSnapshot?,
    val room: ElementSnapshot?,
    val group: ElementSnapshot?
)

@Composable
fun EventTile(
    ev: EventDto,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    homeworkCount: Int = 0,
    /** Kolor przedmiotu z /me/colors (moze byc null -> fallback hash). */
    subjectColorHex: String? = null,
    onExpandClick: ((TileMorphData) -> Unit)? = null
) {
    val density = LocalDensity.current
    var tileHeightPx by remember { mutableIntStateOf(0) }
    var tileWidthPx by remember { mutableIntStateOf(0) }
    val tileCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val subjectCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val roomCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val groupCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }

    val bg = (ev.colorHexOverride ?: subjectColorHex)?.let { hex ->
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
    } ?: EventColors.bgFor(ev.subject)
    val txt = EventColors.textFor(bg)

    val mode = with(density) {
        val w = tileWidthPx.toDp()
        val h = tileHeightPx.toDp()

        when {
            w < 22.dp || h < 14.dp -> TileContentMode.TINY
            w < 34.dp || h < 18.dp -> TileContentMode.ROOM_ONLY
            w < 80.dp -> if (h >= 20.dp) TileContentMode.COMPACT else TileContentMode.ROOM_ONLY
            w < 110.dp || h < 58.dp -> when {
                h >= 34.dp -> TileContentMode.MEDIUM
                h >= 20.dp -> TileContentMode.COMPACT
                else -> TileContentMode.ROOM_ONLY
            }
            else -> when {
                h >= 58.dp -> TileContentMode.FULL
                h >= 34.dp -> TileContentMode.MEDIUM
                h >= 20.dp -> TileContentMode.COMPACT
                else -> TileContentMode.ROOM_ONLY
            }
        }
    }

    DisposableEffect(mode) {
        subjectCoords[0] = null
        roomCoords[0] = null
        groupCoords[0] = null
        onDispose { }
    }

    val clickModifier = if (onExpandClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            val tc = tileCoords[0] ?: return@clickable
            val tb = tc.boundsInRoot()
            val tileRect = Rect(tb.left, tb.top, tb.right, tb.bottom)

            fun snap(
                coords: LayoutCoordinates?,
                text: String?,
                fontSizeSp: Float,
                weight: FontWeight
            ): ElementSnapshot? {
                if (coords == null || text.isNullOrBlank()) return null
                val b = coords.boundsInRoot()
                if (b.width <= 0f || b.height <= 0f) return null
                return ElementSnapshot(
                    text = text,
                    rect = Rect(b.left, b.top, b.right, b.bottom),
                    fontSizeSp = fontSizeSp,
                    fontWeight = weight
                )
            }

            val subjectSnap = when (mode) {
                TileContentMode.FULL -> snap(subjectCoords[0], ev.subject, TileFontSizes.SUBJECT_FULL, FontWeight.Bold)
                TileContentMode.MEDIUM -> snap(subjectCoords[0], ev.subject, TileFontSizes.SUBJECT_MEDIUM, FontWeight.Bold)
                TileContentMode.COMPACT -> snap(subjectCoords[0], ev.subject, TileFontSizes.SUBJECT_COMPACT, FontWeight.Bold)
                else -> null
            }
            val roomSnap = when (mode) {
                TileContentMode.FULL -> snap(roomCoords[0], ev.room, TileFontSizes.ROOM_FULL, FontWeight.SemiBold)
                TileContentMode.MEDIUM -> snap(roomCoords[0], ev.room, TileFontSizes.ROOM_MEDIUM, FontWeight.SemiBold)
                TileContentMode.COMPACT -> snap(roomCoords[0], ev.room, TileFontSizes.ROOM_COMPACT, FontWeight.SemiBold)
                TileContentMode.ROOM_ONLY -> snap(roomCoords[0], ev.room, TileFontSizes.ROOM_COMPACT, FontWeight.SemiBold)
                else -> null
            }
            val groupSnap = when (mode) {
                TileContentMode.FULL -> snap(groupCoords[0], ev.group.takeIf { it.isNotBlank() }, TileFontSizes.GROUP_FULL, FontWeight.Normal)
                TileContentMode.MEDIUM -> snap(groupCoords[0], ev.group.takeIf { it.isNotBlank() }, TileFontSizes.GROUP_MEDIUM, FontWeight.Normal)
                else -> null
            }

            onExpandClick(
                TileMorphData(
                    event = ev,
                    backgroundColor = bg,
                    textColor = txt,
                    tileRect = tileRect,
                    subject = subjectSnap,
                    room = roomSnap,
                    group = groupSnap
                )
            )
        }
    } else Modifier

    Card(
        modifier = modifier
            .graphicsLayer { alpha = if (isExpanded) 0f else 1f }
            .onSizeChanged {
                tileWidthPx = it.width
                tileHeightPx = it.height
            }
            .onGloballyPositioned { tileCoords[0] = it }
            .then(clickModifier),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp,
            hoveredElevation = 5.dp
        )
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (mode) {
                    TileContentMode.FULL -> FullContent(ev, txt,
                        Modifier.onGloballyPositioned { subjectCoords[0] = it },
                        Modifier.onGloballyPositioned { roomCoords[0] = it },
                        Modifier.onGloballyPositioned { groupCoords[0] = it })
                    TileContentMode.MEDIUM -> MediumContent(ev, txt,
                        Modifier.onGloballyPositioned { subjectCoords[0] = it },
                        Modifier.onGloballyPositioned { roomCoords[0] = it },
                        Modifier.onGloballyPositioned { groupCoords[0] = it })
                    TileContentMode.COMPACT -> CompactContent(ev, txt,
                        Modifier.onGloballyPositioned { subjectCoords[0] = it },
                        Modifier.onGloballyPositioned { roomCoords[0] = it })
                    TileContentMode.ROOM_ONLY -> RoomOnlyContent(ev, txt,
                        Modifier.onGloballyPositioned { roomCoords[0] = it })
                    TileContentMode.TINY -> TinyContent(ev, txt)
                }
            }
            if (homeworkCount > 0 && mode != TileContentMode.TINY) {
                HomeworkBadge(count = homeworkCount, textColor = txt)
            }
        }
    }
}

@Composable
private fun FullContent(ev: EventDto, txt: Color, subjectMod: Modifier, roomMod: Modifier, groupMod: Modifier) {
    val groupType = ev.group.takeWhile { it.isLetter() }.ifBlank { "" }
    val groupNumber = ev.group.dropWhile { it.isLetter() }.ifBlank { "" }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                ev.subject, color = txt,
                fontSize = TileFontSizes.SUBJECT_FULL.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp,
                modifier = Modifier.weight(1f).then(subjectMod)
            )
            if (!ev.room.isNullOrBlank()) {
                Spacer(Modifier.width(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("🚪", fontSize = 11.sp)
                    Text(
                        ev.room, color = txt.copy(alpha = 0.95f),
                        fontSize = TileFontSizes.ROOM_FULL.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, modifier = roomMod
                    )
                }
            }
        }
        if (!ev.teacher.isNullOrBlank()) {
            Text(ev.teacher, color = txt.copy(alpha = 0.85f), fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp)
        }
        if (ev.group.isNotBlank()) {
            Box(groupMod) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (groupType.isNotBlank()) {
                        Text(groupType, color = txt, fontSize = TileFontSizes.GROUP_FULL.sp,
                            fontWeight = FontWeight.Normal, lineHeight = 12.sp)
                    }
                    if (groupType.isNotBlank() && groupNumber.isNotBlank()) Spacer(Modifier.width(2.dp))
                    if (groupNumber.isNotBlank()) {
                        Text(groupNumber, color = txt.copy(alpha = 0.9f),
                            fontSize = TileFontSizes.GROUP_FULL.sp, fontWeight = FontWeight.Normal,
                            lineHeight = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediumContent(ev: EventDto, txt: Color, subjectMod: Modifier, roomMod: Modifier, groupMod: Modifier) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                ev.subject, color = txt,
                fontSize = TileFontSizes.SUBJECT_MEDIUM.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).then(subjectMod)
            )
            if (ev.group.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text("|", color = txt.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.width(6.dp))
                Text(ev.group, color = txt.copy(alpha = 0.9f),
                    fontSize = TileFontSizes.GROUP_MEDIUM.sp, fontWeight = FontWeight.Normal,
                    maxLines = 1, modifier = groupMod)
            }
        }
        if (!ev.room.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text("🚪", fontSize = 11.sp)
            Spacer(Modifier.width(2.dp))
            Text(ev.room, color = txt.copy(alpha = 0.95f),
                fontSize = TileFontSizes.ROOM_MEDIUM.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, modifier = roomMod)
        }
    }
}

@Composable
private fun CompactContent(ev: EventDto, txt: Color, subjectMod: Modifier, roomMod: Modifier) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(ev.subject, color = txt, fontSize = TileFontSizes.SUBJECT_COMPACT.sp,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).then(subjectMod))
        if (!ev.room.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Text("🚪", fontSize = 10.sp)
            Spacer(Modifier.width(2.dp))
            Text(ev.room, color = txt.copy(alpha = 0.95f),
                fontSize = TileFontSizes.ROOM_COMPACT.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, modifier = roomMod)
        }
    }
}

@Composable
private fun RoomOnlyContent(ev: EventDto, txt: Color, roomMod: Modifier) {
    Box(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!ev.room.isNullOrBlank()) {
                Text("🚪", fontSize = 10.sp)
                Spacer(Modifier.width(2.dp))
                Text(ev.room, color = txt, fontSize = TileFontSizes.ROOM_COMPACT.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = roomMod)
            } else {
                Text(ev.subject, color = txt, fontSize = TileFontSizes.SUBJECT_COMPACT.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TinyContent(ev: EventDto, txt: Color) {
    Box(Modifier.fillMaxSize().padding(2.dp), contentAlignment = Alignment.Center) {
        val label = ev.room?.trim()?.take(2) ?: ev.subject.take(2)
        Text(label, color = txt, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Clip)
    }
}
