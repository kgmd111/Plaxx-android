package com.plaxx.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.layout.padding
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaxxTopBar(
    monthLabel: String = "",
    monthOrder: Int = 0,
    onSettingsClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onExportIcs: (() -> Unit)? = null,
    onSyncNow: (() -> Unit)? = null,
    isSyncing: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Plaxx", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (monthLabel.isNotBlank()) {
                    Spacer(Modifier.width(20.dp))
                    AnimatedMonthLabel(monthLabel, monthOrder, Modifier.weight(1f))
                }
            }
        },
        actions = {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (onExportIcs != null || onSyncNow != null) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Wiecej")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    onSyncNow?.let { cb ->
                        DropdownMenuItem(
                            text = { Text("Synchronizuj teraz") },
                            onClick = {
                                menuOpen = false
                                cb()
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                            }
                        )
                    }
                    onExportIcs?.let { cb ->
                        DropdownMenuItem(
                            text = { Text("Eksportuj tydzien (.ics)") },
                            onClick = {
                                menuOpen = false
                                cb()
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Share, contentDescription = null)
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Ustawienia")
            }
            IconButton(onClick = onAccountClick) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Konto")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun AnimatedMonthLabel(label: String, order: Int, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = label to order,
        transitionSpec = {
            val dir = if (targetState.second > initialState.second) 1 else -1
            (slideInVertically(tween(280), initialOffsetY = { dir * it }) + fadeIn(tween(280))) togetherWith
                    (slideOutVertically(tween(220), targetOffsetY = { -dir * it }) + fadeOut(tween(180)))
        },
        label = "monthLabel",
        modifier = modifier
    ) { (text, _) ->
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun monthLabel(date: java.time.LocalDate): String {
    val ym = YearMonth.from(date)
    val name = ym.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("pl"))
        .replaceFirstChar { it.uppercase() }
    return "$name ${ym.year}"
}

fun monthOrder(date: java.time.LocalDate): Int {
    val ym = YearMonth.from(date)
    return ym.year * 12 + ym.monthValue
}
