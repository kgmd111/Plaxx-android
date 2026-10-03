package com.plaxx.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plaxx.app.data.UserEventRepository
import com.plaxx.app.data.api.UpdateUserEventReq
import com.plaxx.app.data.api.UserEventDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.plaxx.app.data.models.EventDto
import java.time.LocalDateTime
class CustomEventsViewModel(
    private val repository: UserEventRepository = UserEventRepository()
) : ViewModel() {

    private val _items = MutableStateFlow<List<UserEventDto>>(emptyList())
    val items: StateFlow<List<UserEventDto>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.list()
                .onSuccess { _items.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun create(
        title: String,
        description: String?,
        location: String?,
        colorHex: String?,
        start: LocalDateTime,
        end: LocalDateTime,
        onSuccess: (Long) -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            repository.create(title, description, location, colorHex, start, end)
                .onSuccess {
                    load()
                    onSuccess(it)
                }
                .onFailure { e ->
                    _error.value = e.message
                    onError(e.message ?: "Blad")
                }
        }
    }

    fun update(id: Long, req: UpdateUserEventReq) {
        viewModelScope.launch {
            repository.update(id, req).onSuccess { load() }
        }
    }

    fun remove(id: Long) {
        val prev = _items.value
        _items.value = prev.filterNot { it.id == id }
        viewModelScope.launch {
            repository.delete(id).onFailure { _items.value = prev }
        }
    }


    /**
     * Konwertuje custom events do wewnetrznego EventDto tak, zeby mogly byc
     * renderowane tym samym kodem co plan events (EventTile, DominoPager).
     */
    fun asEventDtos(): List<com.plaxx.app.data.models.EventDto> =
        _items.value.map { it.toInternalEventDto() }
    fun forDate(date: java.time.LocalDate): List<UserEventDto> =
        _items.value.filter { ev ->
            runCatching {
                LocalDateTime.parse(ev.startDatetime.replace(' ', 'T')).toLocalDate()
            }.getOrNull() == date
        }
    fun UserEventDto.toInternalEventDto(): EventDto {
        val startIso = runCatching {
            LocalDateTime.parse(startDatetime.replace(' ', 'T')).toString()
        }.getOrDefault(startDatetime)

        val endIso = runCatching {
            LocalDateTime.parse(endDatetime.replace(' ', 'T')).toString()
        }.getOrDefault(endDatetime)

        val day = runCatching {
            LocalDateTime.parse(startDatetime.replace(' ', 'T')).dayOfWeek.value
        }.getOrDefault(1)

        return EventDto(
            subject = title,
            group = "",
            teacher = null,
            room = location,
            desc = description,
            start = startIso,
            end = endIso,
            day = day,
            id = "custom-$id",
            logicalId = "custom:$id",
            isCustom = true,
            colorHexOverride = colorHex,
            remoteId = id
        )
    }
}
