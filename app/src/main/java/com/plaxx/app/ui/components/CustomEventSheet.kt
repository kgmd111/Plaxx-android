package com.plaxx.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.plaxx.app.ui.theme.BASE_PALETTE
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

/**
 * ModalBottomSheet do tworzenia custom eventu.
 * Wywolaj: if (show) CustomEventSheet(onDismiss, onCreate)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomEventSheet(
    initialDate: LocalDate = LocalDate.now(),
    initialHour: Int = 12,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        description: String?,
        location: String?,
        colorHex: String?,
        start: LocalDateTime,
        end: LocalDateTime,
    ) -> Unit,
    submitting: Boolean = false,
    errorMessage: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf<String?>(BASE_PALETTE[0]) }

    var start by remember {
        mutableStateOf(LocalDateTime.of(initialDate, LocalTime.of(initialHour, 0)))
    }
    var end by remember {
        mutableStateOf(LocalDateTime.of(initialDate, LocalTime.of(initialHour + 1, 0)))
    }

    var showStartDate by remember { mutableStateOf(false) }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndDate by remember { mutableStateOf(false) }
    var showEndTime by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Nowe wydarzenie",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Zamknij")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tytul") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Lokalizacja (opcjonalnie)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Termin", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))

            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showStartDate = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(start.format(DateTimeFormatter.ofPattern("d MMM")), fontSize = 12.sp)
                }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = { showStartTime = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(start.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showEndDate = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(end.format(DateTimeFormatter.ofPattern("d MMM")), fontSize = 12.sp)
                }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = { showEndTime = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(end.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Kolor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            ColorPicker(
                current = colorHex,
                onPick = { colorHex = it }
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Anuluj") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onCreate(
                            title.trim(),
                            description.trim().takeIf { it.isNotEmpty() },
                            location.trim().takeIf { it.isNotEmpty() },
                            colorHex,
                            start,
                            end
                        )
                    },
                    enabled = title.isNotBlank() && end.isAfter(start) && !submitting
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Dodaj")
                    }
                }
            }
        }
    }

    // ─── Dialogi Date/Time ───
    if (showStartDate) {
        DatePickerSheet(
            initial = start.toLocalDate(),
            onDismiss = { showStartDate = false },
            onPick = { picked ->
                start = LocalDateTime.of(picked, start.toLocalTime())
                showStartDate = false
            }
        )
    }
    if (showEndDate) {
        DatePickerSheet(
            initial = end.toLocalDate(),
            onDismiss = { showEndDate = false },
            onPick = { picked ->
                end = LocalDateTime.of(picked, end.toLocalTime())
                showEndDate = false
            }
        )
    }
    if (showStartTime) {
        TimePickerSheet(
            initialHour = start.hour, initialMinute = start.minute,
            onDismiss = { showStartTime = false },
            onPick = { h, m ->
                start = LocalDateTime.of(start.toLocalDate(), LocalTime.of(h, m))
                showStartTime = false
            }
        )
    }
    if (showEndTime) {
        TimePickerSheet(
            initialHour = end.hour, initialMinute = end.minute,
            onDismiss = { showEndTime = false },
            onPick = { h, m ->
                end = LocalDateTime.of(end.toLocalDate(), LocalTime.of(h, m))
                showEndTime = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val date = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    onPick(date)
                } else onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onPick: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Godzina", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                TimePicker(state = state)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onPick(state.hour, state.minute) }) { Text("OK") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(
    current: String?,
    onPick: (String) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BASE_PALETTE.forEach { hex ->
            val color = Color(android.graphics.Color.parseColor(hex))
            val selected = current == hex
            Box(
                Modifier
                    .size(if (selected) 34.dp else 30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
                    .then(
                        if (selected) Modifier.border(
                            2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(50)
                        ) else Modifier
                    )
                    .clickable { onPick(hex) }
            )
        }
    }
}
