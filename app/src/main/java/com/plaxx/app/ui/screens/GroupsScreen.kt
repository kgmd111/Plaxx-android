package com.plaxx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.plaxx.app.data.models.SubjectInfo
import com.plaxx.app.ui.theme.EventColors
import com.plaxx.app.ui.viewmodels.ScheduleViewModel

@Composable
fun GroupsScreen(viewModel: ScheduleViewModel) {
    val state by viewModel.state.collectAsState()

    if (!state.isAuthenticated) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nie zalogowano")
        }
        return
    }

    val subjects = remember(state.subjects) {
        state.subjects.values.sortedBy { it.name.lowercase() }
    }

    val isDirty = state.selected != state.myGroups

    Column(Modifier.fillMaxSize()) {
        // ===== Pasek akcji =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.selectAllGroups() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text("Wszystko", maxLines = 1, fontSize = 12.sp, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = { viewModel.clearGroups() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text("Nic", maxLines = 1, fontSize = 12.sp, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = { viewModel.restoreMyGroups() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Refresh, null, Modifier.width(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Przywroc", maxLines = 1, fontSize = 12.sp, overflow = TextOverflow.Ellipsis)
            }
        }

        // ===== Pasek zapisu =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isDirty) "Niezapisane zmiany" else "Wszystko zapisane",
                fontSize = 11.sp,
                color = if (isDirty) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { viewModel.saveMyGroups() },
                enabled = isDirty,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Save, null, Modifier.width(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Zapisz moje grupy", fontSize = 12.sp, maxLines = 1)
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ===== Lista =====
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(subjects, key = { it.name }) { info ->
                SubjectCard(
                    info = info,
                    selected = state.selected,
                    onToggleExpand = { viewModel.toggleSubject(info.name) },
                    onToggleGroup = { group ->
                        viewModel.toggleGroup("${info.name} \u2022 $group")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectCard(
    info: SubjectInfo,
    selected: Set<String>,
    onToggleExpand: () -> Unit,
    onToggleGroup: (String) -> Unit
) {
    val bg = EventColors.bgFor(info.name)
    val txt = EventColors.textFor(bg)

    val selectedCount = info.groups.count { "${info.name} \u2022 $it" in selected }
    val totalCount = info.groups.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = info.name,
                        color = txt,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$selectedCount z $totalCount wybranych",
                        color = txt.copy(alpha = 0.78f),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (info.open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (info.open) "Zwin" else "Rozwin",
                    tint = txt
                )
            }

            if (info.open && info.groups.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info.groups.forEach { group ->
                        val logicalId = "${info.name} \u2022 $group"
                        val isSelected = logicalId in selected
                        GroupPill(
                            text = group,
                            isSelected = isSelected,
                            accentColor = bg,
                            accentTextColor = txt,
                            onClick = { onToggleGroup(group) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupPill(
    text: String,
    isSelected: Boolean,
    accentColor: Color,
    accentTextColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    val containerColor = if (isSelected) accentColor else Color.Transparent
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
    val contentColor = if (isSelected) accentTextColor else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
