package com.plaxx.app.ui.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plaxx.app.data.HomeworkRepository
import com.plaxx.app.data.api.AttachmentDto
import com.plaxx.app.data.api.HomeworkDto
import com.plaxx.app.data.api.UpdateHomeworkReq
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class HomeworkViewModel(
    private val repository: HomeworkRepository = HomeworkRepository()
) : ViewModel() {

    private val _items = MutableStateFlow<List<HomeworkDto>>(emptyList())
    val items: StateFlow<List<HomeworkDto>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.list()
                .onSuccess { _items.value = it.sortedBy { hw -> hw.dueDate ?: "9999" } }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun create(
        title: String,
        description: String?,
        dueDate: LocalDateTime?,
        planEventId: Long? = null,
        userEventId: Long? = null,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            repository.create(title, description, dueDate, planEventId, userEventId)
                .onSuccess {
                    load()
                    onSuccess?.invoke()
                }
                .onFailure { e ->
                    _error.value = e.message
                    onError?.invoke(e.message ?: "Blad")
                }
        }
    }

    fun toggleStatus(hw: HomeworkDto) {
        val newStatus = if (hw.status == "done") "pending" else "done"
        val prev = _items.value
        _items.value = prev.map { if (it.id == hw.id) it.copy(status = newStatus) else it }
        viewModelScope.launch {
            repository.update(hw.id, UpdateHomeworkReq(status = newStatus))
                .onFailure { _items.value = prev }
        }
    }

    fun update(hw: HomeworkDto, req: UpdateHomeworkReq) {
        val prev = _items.value
        _items.value = prev.map {
            if (it.id == hw.id) it.copy(
                title = req.title ?: it.title,
                description = req.description ?: it.description,
                dueDate = req.dueDate ?: it.dueDate,
                status = req.status ?: it.status
            ) else it
        }
        viewModelScope.launch {
            repository.update(hw.id, req).onFailure { _items.value = prev }
        }
    }

    fun remove(hw: HomeworkDto) {
        val prev = _items.value
        _items.value = prev.filterNot { it.id == hw.id }
        viewModelScope.launch {
            repository.delete(hw.id).onFailure { _items.value = prev }
        }
    }

    fun uploadAttachments(
        homeworkId: Long,
        contentResolver: ContentResolver,
        uris: List<Uri>,
        onResult: (List<AttachmentDto>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val parts = repository.buildParts(contentResolver, uris)
            if (parts.isEmpty()) {
                _error.value = "Nie udalo sie odczytac plikow"
                return@launch
            }
            repository.uploadAttachments(homeworkId, parts)
                .onSuccess { onResult(it) }
                .onFailure { _error.value = it.message }
        }
    }

    /** Zadania przypisane do konkretnego eventu planu. */
    fun forPlanEvent(planEventId: Long): List<HomeworkDto> =
        _items.value.filter { it.planEventId == planEventId }

    /** Zadania przypisane do custom eventu. */
    fun forUserEvent(userEventId: Long): List<HomeworkDto> =
        _items.value.filter { it.userEventId == userEventId }

    /** Mapa planEventId -> count (do badge na kafelkach). */
    fun countsByPlanEvent(): Map<Long, Int> =
        _items.value
            .filter { it.planEventId != null && it.status != "done" }
            .groupingBy { it.planEventId!! }
            .eachCount()
}
