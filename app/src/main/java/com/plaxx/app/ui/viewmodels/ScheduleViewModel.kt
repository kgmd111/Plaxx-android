package com.plaxx.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.plaxx.app.data.sync.SyncWorker
import com.plaxx.app.data.ScheduleRepository
import com.plaxx.app.data.sync.SyncManager
import com.plaxx.app.data.api.ChangeDto
import com.plaxx.app.data.SyncPayloadParser
import com.plaxx.app.data.models.EventDto
import com.plaxx.app.data.models.SubjectInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface LoginState {
    object Idle : LoginState
    object Loading : LoginState
    data class Success(val username: String) : LoginState
    data class Error(val message: String) : LoginState
}

data class ScheduleState(
    val isAuthenticated: Boolean = false,
    val userName: String = "",
    val planId: Long? = null,
    val planName: String = "",
    val events: List<EventDto> = emptyList(),
    val subjects: Map<String, SubjectInfo> = emptyMap(),

    /**
     * Wybor w widoku (filtr kafelkow). Zmienia sie przy kazdym ticku grupy
     * w widoku - NIE idzie od razu do API.
     */
    val selected: Set<String> = emptySet(),

    /**
     * Trwaly wybor uzytkownika (jak w bazie, GET /me/groups).
     * Zmienia sie tylko po "Zapisz moje grupy".
     */
    val myGroups: Set<String> = emptySet(),

    val loading: Boolean = false,
    val error: String? = null,
    val selectedDate: LocalDate = LocalDate.now(),

    /** Ostatnio zsynchronizowana wersja planu (planId -> current_version). */
    val lastVersion: Map<Long, Long> = emptyMap(),

    /** Czy aktualnie trwa synchronizacja (do UI: pasek/indykator). */
    val syncing: Boolean = false
)

class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository(),
    private val syncManager: SyncManager = SyncManager(repository)
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _state = MutableStateFlow(ScheduleState())
    val state: StateFlow<ScheduleState> = _state.asStateFlow()

    // ===== Logowanie =====

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            repository.login(username, password)
                .onSuccess { auth ->
                    val plans = repository.listPlans().getOrNull().orEmpty()
                    val plan = plans.firstOrNull()
                    if (plan == null) {
                        _loginState.value = LoginState.Error("Brak planow na koncie")
                        return@launch
                    }

                    val eventsDeferred = async { repository.planEvents(plan.id) }
                    val groupsDeferred = async { repository.myGroups(plan.id) }

                    val events = eventsDeferred.await().getOrElse { emptyList() }
                    val myGroups = groupsDeferred.await().getOrElse { emptySet() }

                    val subjects = buildSubjectsMap(events)

                    val today = LocalDate.now()
                    val eventDates = events
                        .mapNotNull { ev ->
                            runCatching { LocalDateTime.parse(ev.start).toLocalDate() }.getOrNull()
                        }
                        .distinct()
                        .sorted()

                    val initialDate = when {
                        eventDates.contains(today) -> today
                        eventDates.isNotEmpty() -> eventDates.first()
                        else -> today
                    }

                    _state.value = ScheduleState(
                        isAuthenticated = true,
                        userName = auth.user.username,
                        planId = plan.id,
                        planName = plan.name,
                        events = events,
                        subjects = subjects,
                        selected = myGroups,
                        myGroups = myGroups,
                        selectedDate = initialDate,
                        lastVersion = mapOf(plan.id to plan.currentVersion)
                    )
                    _loginState.value = LoginState.Success(auth.user.username)
                }
                .onFailure { e ->
                    _loginState.value = LoginState.Error(e.message ?: "Blad logowania")
                }
        }
    }

    // ===== Widok (bez zapisu do API) =====

    fun toggleGroup(logicalId: String) {
        val cur = _state.value
        val newSelected = cur.selected.toMutableSet().apply {
            if (contains(logicalId)) remove(logicalId) else add(logicalId)
        }
        _state.value = cur.copy(selected = newSelected)
    }

    fun selectAllGroups() {
        val cur = _state.value
        val all = cur.events.map { it.logicalId }.toSet()
        _state.value = cur.copy(selected = all)
    }

    fun clearGroups() {
        _state.value = _state.value.copy(selected = emptySet())
    }

    /** Przywroc widok do ostatnio zapisanych grup (bez API). */
    fun restoreMyGroups() {
        val cur = _state.value
        _state.value = cur.copy(selected = cur.myGroups)
    }

    // ===== Trwaly zapis =====

    /**
     * Zapisuje biezacy widok (selected) do API jako "moje grupy".
     * Wywoluje PUT /me/groups/:plan_id.
     */
    fun saveMyGroups(onDone: (Boolean) -> Unit = {}) {
        val cur = _state.value
        val planId = cur.planId ?: return
        val toSave = cur.selected
        viewModelScope.launch {
            repository.replaceGroups(planId, toSave)
                .onSuccess {
                    _state.value = _state.value.copy(myGroups = toSave)
                    onDone(true)
                }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message)
                    onDone(false)
                }
        }
    }

    /** Pobiera "moje grupy" z API (GET /me/groups) i ustawia je jako widok. */
    fun reloadMyGroupsFromApi() {
        val planId = _state.value.planId ?: return
        viewModelScope.launch {
            repository.myGroups(planId).onSuccess { saved ->
                _state.value = _state.value.copy(myGroups = saved, selected = saved)
            }
        }
    }

    // ===== Subject helpers =====

    fun toggleSubject(subject: String) {
        val cur = _state.value
        val info = cur.subjects[subject] ?: return
        _state.value = cur.copy(
            subjects = cur.subjects + (subject to info.copy(open = !info.open))
        )
    }

    fun selectDate(date: LocalDate) {
        _state.value = _state.value.copy(selectedDate = date)
    }


    /**
     * Uzywane po zakonczeniu rejestracji (RegisterFlow) — konto + plan + grupy
     * sa juz na serwerze. Wczytujemy plan i ustawiamy stan jak po loginie.
     */
    fun loadAfterAuth(username: String) {
        viewModelScope.launch {
            val plans = repository.listPlans().getOrNull().orEmpty()
            val plan = plans.firstOrNull()
            if (plan == null) return@launch

            val events = repository.planEvents(plan.id).getOrElse { emptyList() }
            val myGroups = repository.myGroups(plan.id).getOrElse { emptySet() }

            val today = LocalDate.now()
            val eventDates = events
                .mapNotNull { ev ->
                    runCatching { LocalDateTime.parse(ev.start).toLocalDate() }.getOrNull()
                }
                .distinct()
                .sorted()

            val initialDate = when {
                eventDates.contains(today) -> today
                eventDates.isNotEmpty() -> eventDates.first()
                else -> today
            }

            _state.value = ScheduleState(
                isAuthenticated = true,
                userName = username,
                planId = plan.id,
                planName = plan.name,
                events = events,
                subjects = buildSubjectsMap(events),
                selected = myGroups,
                myGroups = myGroups,
                selectedDate = initialDate,
                lastVersion = mapOf(plan.id to plan.currentVersion)
            )
            _loginState.value = LoginState.Success(username)
        }
    }


    // ═══════════════════════════════════════════════════════════
    // Delta sync
    // ═══════════════════════════════════════════════════════════


    /**
     * Rejestruje jednorazowy sync z aktualnym planId i lastVersion.
     * Worker (SyncWorker) sam wykona GET /sync i aplikuje delty (patrz jego inputData).
     */
    fun scheduleSyncWork(context: Context) {
        val cur = _state.value
        val planId = cur.planId ?: return
        val since = cur.lastVersion[planId] ?: 0L

        val data = Data.Builder()
            .putStringArray("plan_ids", arrayOf(planId.toString()))
            .putString("since_$planId", since.toString())
            .build()

        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SyncWorker.UNIQUE_NAME + "-once",
                ExistingWorkPolicy.REPLACE,
                work
            )
    }

    // ═══════════════════════════════════════════════════════════
    // Delta sync
    // ═══════════════════════════════════════════════════════════

    /**
     * Wola GET /sync z zapisanymi wersjami i aplikuje delty w miejscu.
     * Bezpieczne do wywolania z UI (np. przy powrocie do apki, pull-to-refresh).
     */
    fun syncNow() {
        val cur = _state.value
        val planId = cur.planId ?: return
        if (cur.syncing) return

        viewModelScope.launch {
            _state.value = _state.value.copy(syncing = true)
            val since = _state.value.lastVersion

            syncManager.sync(
                planIds = listOf(planId),
                sinceVersions = since
            ).onSuccess { resp ->
                applySyncResponse(resp)
                _state.value = _state.value.copy(syncing = false, error = null)
            }.onFailure { e ->
                _state.value = _state.value.copy(syncing = false, error = e.message)
            }
        }
    }

    /**
     * Aplikuje delty eventow (CREATE/UPDATE/DELETE) i aktualizuje lastVersion.
     * Nie rusza widokowych pol: selected / myGroups.
     */
    private fun applySyncResponse(resp: com.plaxx.app.data.api.SyncResp) {
        val cur = _state.value
        val currentEvents = cur.events.toMutableList()

        resp.plans.forEach { plan ->
            plan.changes.forEach { change ->
                applyChange(currentEvents, change)
            }
        }

        // Nowe wersje per plan
        val newVersions = cur.lastVersion.toMutableMap()
        resp.plans.forEach { p -> newVersions[p.id] = p.currentVersion }

        // Zaktualizuj myGroups z serwera (jesli backend zwrocil)
        val myGroupsFromSync: Set<String>? = if (resp.groups.isNotEmpty()) {
            resp.groups
                .filter { it.planId == cur.planId }
                .map { "${it.subject.trim()} \u2022 ${it.groupName.trim()}" }
                .toSet()
        } else null

        _state.value = cur.copy(
            events = currentEvents,
            subjects = buildSubjectsMap(currentEvents),
            lastVersion = newVersions,
            myGroups = myGroupsFromSync ?: cur.myGroups,
            // jesli user nie mial zadnych grup wybranych w widoku - dociagnij z serwera
            selected = if (cur.selected.isEmpty() && myGroupsFromSync != null) myGroupsFromSync
                       else cur.selected
        )
    }

    private fun applyChange(events: MutableList<EventDto>, change: ChangeDto) {
        when (change.op.uppercase()) {
            "CREATE", "UPDATE" -> {
                val dto = SyncPayloadParser.parseEvent(change) ?: return
                if (dto.deleted != 0) {
                    // traktuj jako usuniecie
                    events.removeAll { it.id == dto.id.toString() }
                    return
                }
                val internal = dto.toInternalForSync()
                val idx = events.indexOfFirst { it.id == internal.id }
                if (idx >= 0) events[idx] = internal
                else events.add(internal)
            }
            "DELETE" -> {
                val id = change.entityId?.toString() ?: return
                events.removeAll { it.id == id }
            }
        }
    }

    private fun buildSubjectsMap(events: List<EventDto>): Map<String, SubjectInfo> {
        val map = mutableMapOf<String, SubjectInfo>()
        events.forEach { ev ->
            val info = map[ev.subject]
            if (info == null) {
                map[ev.subject] = SubjectInfo(name = ev.subject, groups = listOf(ev.group))
            } else if (!info.groups.contains(ev.group)) {
                map[ev.subject] = info.copy(groups = info.groups + ev.group)
            }
        }
        return map.mapValues { (_, info) -> info.copy(groups = info.groups.sorted()) }
    }

    init {
        if (com.plaxx.app.ui.debug.MockData.DEBUG_OFFLINE) {
            loadMockData()
        }
    }
    private fun com.plaxx.app.data.api.PlanEventDto.toInternalForSync(): EventDto {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val startLdt = runCatching { java.time.LocalDateTime.parse(startDatetime, fmt) }.getOrNull()
        val endLdt = runCatching { java.time.LocalDateTime.parse(endDatetime, fmt) }.getOrNull()
        val startIso = startLdt?.toString() ?: startDatetime
        val endIso = endLdt?.toString() ?: endDatetime
        val day = startLdt?.dayOfWeek?.value ?: 1
        val s = subject.trim()
        val g = groupName.trim()
        return EventDto(
            subject = s,
            group = g,
            teacher = teacher?.trim(),
            room = room?.trim(),
            desc = null,
            start = startIso,
            end = endIso,
            day = day,
            id = id.toString(),
            logicalId = "$s \u2022 $g",
            remoteId = id
        )
    }
    private fun loadMockData() {
        val mock = com.plaxx.app.ui.debug.MockData.buildSession()
        _state.value = ScheduleState(
            isAuthenticated = true,
            userName = mock.user.username,
            planId = mock.planId,
            planName = mock.planName,
            events = mock.events,
            subjects = buildSubjectsMap(mock.events),
            selected = mock.selectedGroups,
            myGroups = mock.selectedGroups,
            selectedDate = mock.events
                .mapNotNull { runCatching { LocalDateTime.parse(it.start).toLocalDate() }.getOrNull() }
                .sorted()
                .firstOrNull() ?: LocalDate.now()
        )
        _loginState.value = LoginState.Success(mock.user.username)
    }
}
