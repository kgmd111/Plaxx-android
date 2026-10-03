package com.plaxx.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plaxx.app.data.PlanImportRepository
import com.plaxx.app.data.api.PreviewResp
import com.plaxx.app.domain.GroupType
import com.plaxx.app.ui.theme.BASE_PALETTE
import com.plaxx.app.ui.theme.contrastText
import com.plaxx.app.ui.viewmodels.ScheduleViewModel
import kotlinx.coroutines.launch

/**
 * 3 kroki rejestracji (patrz web RegisterForm.svelte):
 *  1. dane konta (username/first/last/password/confirm)
 *  2. plik z planem (.ics/.xlsx) + podglad (POST /parse-preview)
 *  3. wybor grup (jedna na typ) + POST /auth/register + /plans/import + PUT /me/groups
 */
@Composable
fun RegisterFlow(
    viewModel: ScheduleViewModel,
    onCancel: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PlanImportRepository() }

    var step by remember { mutableIntStateOf(1) }
    var submitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Krok 1
    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    // Krok 2
    var planUri by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<PreviewResp?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    var planName by remember { mutableStateOf("") }

    // Krok 3
    var selectedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        planUri = uri
        preview = null
        previewLoading = true
        scope.launch {
            repo.preview(context.contentResolver, uri)
                .onSuccess {
                    preview = it
                    planName = it.filename.substringBeforeLast(".")
                }
                .onFailure { errorMsg = it.message ?: "Blad parsowania" }
            previewLoading = false
        }
    }

    Column(modifier.fillMaxSize()) {
        // ═══ Pasek postepu ═══
        LinearProgressIndicator(
            progress = { step / 3f },
            modifier = Modifier.fillMaxWidth().height(4.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { step -= 1; errorMsg = null },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.ArrowBack, null, Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Wstecz", fontSize = 12.sp)
                }
            } else {
                OutlinedButton(
                    onClick = onCancel,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Anuluj", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Krok $step z 3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ═══ Zawartosc kroku ═══
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                1 -> Step1UserData(
                    username, { username = it },
                    firstName, { firstName = it },
                    lastName, { lastName = it },
                    password, { password = it },
                    passwordConfirm, { passwordConfirm = it },
                    Modifier.fillMaxSize()
                )
                2 -> Step2FilePreview(
                    planUri = planUri,
                    preview = preview,
                    previewLoading = previewLoading,
                    planName = planName,
                    onPlanNameChange = { planName = it },
                    onPick = { pickFile.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxSize()
                )
                3 -> Step3GroupSelect(
                    preview = preview,
                    selectedGroups = selectedGroups,
                    onToggle = { lid ->
                        selectedGroups = if (lid in selectedGroups) {
                            selectedGroups - lid
                        } else {
                            // jedna grupa na typ w obrebie przedmiotu
                            val (subject, group) = splitLogical(lid)
                            val type = GroupType.from(group)
                            val sameSubjectSameType = selectedGroups.filter { existing ->
                                val (s, g) = splitLogical(existing)
                                s == subject && GroupType.from(g) == type
                            }.toSet()
                            (selectedGroups - sameSubjectSameType) + lid
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ═══ Blad ═══
        errorMsg?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // ═══ Nawigacja ═══
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (step) {
                1 -> {
                    val valid = username.trim().length >= 3 &&
                            password.length >= 6 &&
                            password == passwordConfirm &&
                            firstName.trim().isNotEmpty() &&
                            lastName.trim().isNotEmpty()
                    Button(
                        onClick = { step = 2; errorMsg = null },
                        enabled = valid
                    ) {
                        Text("Dalej")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.ArrowForward, null, Modifier.width(18.dp))
                    }
                }
                2 -> {
                    Button(
                        onClick = { step = 3; errorMsg = null },
                        enabled = planUri != null && preview != null && planName.isNotBlank() && !previewLoading
                    ) {
                        Text("Dalej")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.ArrowForward, null, Modifier.width(18.dp))
                    }
                }
                3 -> {
                    Button(
                        onClick = {
                            val uri = planUri ?: return@Button
                            submitting = true
                            errorMsg = null
                            scope.launch {
                                // 1) register
                                val authResp = repo.register(
                                    username = username,
                                    password = password,
                                    firstName = firstName.trim().ifEmpty { null },
                                    lastName = lastName.trim().ifEmpty { null }
                                ).getOrElse { e ->
                                    errorMsg = e.message ?: "Blad rejestracji"
                                    submitting = false
                                    return@launch
                                }
                                // 2) import plan
                                val importResp = repo.importPlan(
                                    context.contentResolver, uri, planName.trim()
                                ).getOrElse { e ->
                                    errorMsg = e.message ?: "Blad importu planu"
                                    submitting = false
                                    return@launch
                                }
                                // 3) PUT /me/groups
                                val planId = importResp.planId
                                val repository = com.plaxx.app.data.ScheduleRepository()
                                repository.replaceGroups(planId, selectedGroups)
                                    .onFailure { e ->
                                        errorMsg = e.message ?: "Blad zapisu grup"
                                        submitting = false
                                        return@launch
                                    }

                                // 4) wczytaj stan (jak po loginie)
                                viewModel.loadAfterAuth(authResp.user.username)
                                submitting = false
                                onSuccess()
                            }
                        },
                        enabled = !submitting && selectedGroups.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(18.dp).height(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Check, null, Modifier.width(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Zaloz konto i przejdz do planu")
                        }
                    }
                }
            }
        }
    }
}

// ─── Krok 1 ─────────────────────────────────────────────────

@Composable
private fun Step1UserData(
    username: String, onUsername: (String) -> Unit,
    firstName: String, onFirstName: (String) -> Unit,
    lastName: String, onLastName: (String) -> Unit,
    password: String, onPassword: (String) -> Unit,
    passwordConfirm: String, onPasswordConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(20.dp)) {
        Text("Dane konta", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        AppTextField(username, onUsername, "Nazwa uzytkownika", "👤")
        Spacer(Modifier.height(12.dp))
        AppTextField(firstName, onFirstName, "Imie", "📛")
        Spacer(Modifier.height(12.dp))
        AppTextField(lastName, onLastName, "Nazwisko", "📛")
        Spacer(Modifier.height(12.dp))
        AppTextField(password, onPassword, "Haslo (min. 6)", "🔒", isPassword = true)
        Spacer(Modifier.height(12.dp))
        AppTextField(passwordConfirm, onPasswordConfirm, "Powtorz haslo", "🔒", isPassword = true)

        if (passwordConfirm.isNotBlank() && password != passwordConfirm) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Hasla nie sa takie same",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
    }
}

// ─── Krok 2 ─────────────────────────────────────────────────

@Composable
private fun Step2FilePreview(
    planUri: Uri?,
    preview: PreviewResp?,
    previewLoading: Boolean,
    planName: String,
    onPlanNameChange: (String) -> Unit,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(20.dp)) {
        Text("Plik z planem", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Wybierz plik .ics / .xlsx / .xls / .txt z planem zajec.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Upload, null, Modifier.width(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (planUri == null) "Wybierz plik" else "Zmien plik")
        }

        if (previewLoading) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Parsowanie pliku…", fontSize = 13.sp)
            }
        }

        preview?.let { p ->
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Column {
                    Text("✅ ${p.filename}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${p.count} zajec • ${p.subjects.size} przedmiotow",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            AppTextField(planName, onPlanNameChange, "Nazwa planu", "📝")

            Spacer(Modifier.height(16.dp))
            Text("Podglad przedmiotow:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            p.subjects.entries.take(8).forEach { (subject, groups) ->
                Text(
                    "• $subject (${groups.groups.size} grup)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (p.subjects.size > 8) {
                Text("…i ${p.subjects.size - 8} wiecej", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── Krok 3 ─────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step3GroupSelect(
    preview: PreviewResp?,
    selectedGroups: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (preview == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Brak podgladu planu", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val subjects = preview.subjects.entries.sortedBy { it.key.lowercase() }

    Column(modifier) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Wybierz grupy", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Mozesz wybrac jedna grupe kazdego typu (W / C / L / P / S) w przedmiocie.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(subjects, key = { it.key }) { (subject, groupData) ->
                SubjectGroupRow(
                    subject = subject,
                    groups = groupData.groups.map { it.trim() }.filter { it.isNotBlank() },
                    selected = selectedGroups,
                    onToggle = onToggle
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectGroupRow(
    subject: String,
    groups: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    val bgHex = BASE_PALETTE[subject.sumOf { it.code } % BASE_PALETTE.size]
    val bgColor = Color(android.graphics.Color.parseColor(bgHex))
    val txtColor = contrastText(bgHex)

    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(bgColor)
            )
            Spacer(Modifier.width(10.dp))
            Text(subject, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))

        // Grupowanie po GroupType
        val byType = groups.groupBy { GroupType.from(it) }.toSortedMap(compareBy { it.order })

        byType.forEach { (type, typeGroups) ->
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Top) {
                Text(
                    type.name,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(38.dp).padding(top = 8.dp)
                )
                FlowRow(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    typeGroups.sorted().forEach { g ->
                        val lid = "$subject \u2022 $g"
                        val isSel = lid in selected
                        GroupPillInline(
                            text = g,
                            selected = isSel,
                            accent = bgColor,
                            accentText = txtColor,
                            onClick = { onToggle(lid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupPillInline(
    text: String,
    selected: Boolean,
    accent: Color,
    accentText: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val container = if (selected) accent else Color.Transparent
    val border = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
    val content = if (selected) accentText else MaterialTheme.colorScheme.onSurface

    Box(
        Modifier
            .clip(shape)
            .background(container)
            .border(1.dp, border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = content,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ─── Utils ──────────────────────────────────────────────────

private fun splitLogical(lid: String): Pair<String, String> {
    val idx = lid.lastIndexOf(" \u2022 ")
    if (idx < 0) return lid to ""
    return lid.substring(0, idx) to lid.substring(idx + 3)
}
