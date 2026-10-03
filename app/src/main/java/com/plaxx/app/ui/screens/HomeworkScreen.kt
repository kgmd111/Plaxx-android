package com.plaxx.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plaxx.app.data.api.HomeworkDto
import com.plaxx.app.ui.viewmodels.HomeworkViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
private val dueFmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(viewModel: HomeworkViewModel) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (items.isEmpty()) viewModel.load()
    }

    // Formularz
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showDuePicker by remember { mutableStateOf(false) }

    val pickFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        pendingUris = uris
    }

    Column(Modifier.fillMaxSize()) {
        // Header z przyciskiem "Dodaj"
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📚 Zadania", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Button(
                onClick = { showForm = !showForm },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Dodaj", fontSize = 13.sp)
            }
        }

        if (showForm) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytul") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis (opcjonalnie)") },
                    modifier = Modifier.fillMaxWidth().height(72.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDuePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            dueDate?.format(DateTimeFormatter.ofPattern("d MMM HH:mm"))
                                ?: "Wybierz termin",
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { pickFiles.launch("*/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📎 Pliki (${pendingUris.size})", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        title = ""; description = ""; dueDate = null; pendingUris = emptyList()
                        showForm = false
                    }) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.create(
                                title = title.trim(),
                                description = description.trim().takeIf { it.isNotEmpty() },
                                dueDate = dueDate,
                                onSuccess = {
                                    title = ""; description = ""; dueDate = null
                                    pendingUris = emptyList(); showForm = false
                                }
                            )
                        },
                        enabled = title.isNotBlank()
                    ) { Text("Dodaj") }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (loading && items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(items, key = { it.id }) { hw ->
                HomeworkRow(
                    hw = hw,
                    onToggle = { viewModel.toggleStatus(hw) },
                    onDelete = { viewModel.remove(hw) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }

    if (showDuePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (dueDate?.toLocalDate() ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDuePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val d = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        // Ustaw domyslnie 23:59
                        dueDate = LocalDateTime.of(d, LocalTime.of(23, 59))
                    }
                    showDuePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDuePicker = false }) { Text("Anuluj") }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun HomeworkRow(
    hw: HomeworkDto,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = hw.status == "done",
            onCheckedChange = { onToggle() }
        )
        Column(Modifier.weight(1f)) {
            Text(
                hw.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textDecoration = if (hw.status == "done") TextDecoration.LineThrough else null
            )
            hw.dueDate?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    "⏰ ${fmtDue(it)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            hw.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, "Usun", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun fmtDue(iso: String): String = runCatching {
    LocalDateTime.parse(iso.replace(' ', 'T')).format(dueFmt)
}.getOrDefault(iso)
