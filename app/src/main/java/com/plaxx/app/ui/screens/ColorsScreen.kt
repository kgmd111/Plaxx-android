package com.plaxx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plaxx.app.ui.theme.BASE_PALETTE
import com.plaxx.app.ui.theme.contrastText
import com.plaxx.app.ui.viewmodels.ColorsViewModel
import com.plaxx.app.ui.viewmodels.ScheduleViewModel

@Composable
fun ColorsScreen(
    scheduleViewModel: ScheduleViewModel,
    colorsViewModel: ColorsViewModel,
) {
    val state by scheduleViewModel.state.collectAsState()
    val customColors by colorsViewModel.colors.collectAsState()

    val planId = state.planId
    LaunchedEffect(planId) {
        if (planId != null) colorsViewModel.load(planId)
    }

    val subjects = remember(state.subjects) {
        state.subjects.keys.sortedBy { it.lowercase() }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "🎨 Kolory przedmiotow",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Wybierz kolor dla kazdego przedmiotu. Zmiany zapisuja sie od razu.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (subjects.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Brak przedmiotow", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(subjects, key = { it }) { subject ->
                SubjectColorRow(
                    subject = subject,
                    currentHex = customColors[subject]
                        ?: BASE_PALETTE[subject.sumOf { it.code } % BASE_PALETTE.size],
                    hasCustom = customColors.containsKey(subject),
                    onPick = { hex -> planId?.let { colorsViewModel.set(it, subject, hex) } },
                    onReset = { planId?.let { colorsViewModel.reset(it, subject) } },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectColorRow(
    subject: String,
    currentHex: String,
    hasCustom: Boolean,
    onPick: (String) -> Unit,
    onReset: () -> Unit,
) {
    val currentColor = Color(android.graphics.Color.parseColor(currentHex))
    val preview = contrastText(currentHex)

    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Podglad kafelka w kolorze
            Box(
                Modifier
                    .width(46.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(currentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    subject.take(2).uppercase(),
                    color = preview,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    subject,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (hasCustom) "Kolor niestandardowy" else "Kolor z palety (domyslny)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hasCustom) {
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Przywroc domyslny",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BASE_PALETTE.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                val selected = hex.equals(currentHex, ignoreCase = true) && hasCustom
                Box(
                    Modifier
                        .size(if (selected) 32.dp else 28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                        .then(
                            if (selected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.onSurface,
                                RoundedCornerShape(50)
                            ) else Modifier
                        )
                        .clickable { onPick(hex) }
                )
            }
        }
    }
}
