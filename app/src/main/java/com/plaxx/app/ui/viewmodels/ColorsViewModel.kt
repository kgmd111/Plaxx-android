package com.plaxx.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plaxx.app.data.ColorsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Trzyma custom kolory (subject -> hex) dla aktywnego planu.
 * API:
 *   load(planId)                    - GET /me/colors/:planId
 *   set(planId, subject, hex)       - PUT + optimistic update
 *   reset(planId, subject)          - DELETE + optimistic rollback
 */
class ColorsViewModel(
    private val repository: ColorsRepository = ColorsRepository()
) : ViewModel() {

    private val _colors = MutableStateFlow<Map<String, String>>(emptyMap())
    val colors: StateFlow<Map<String, String>> = _colors.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load(planId: Long) {
        viewModelScope.launch {
            _loading.value = true
            repository.get(planId)
                .onSuccess { _colors.value = it }
            _loading.value = false
        }
    }

    fun set(planId: Long, subject: String, hex: String) {
        val prev = _colors.value
        _colors.value = prev + (subject to hex)
        viewModelScope.launch {
            repository.set(planId, subject, hex).onFailure {
                _colors.value = prev
            }
        }
    }

    fun reset(planId: Long, subject: String) {
        val prev = _colors.value
        _colors.value = prev - subject
        viewModelScope.launch {
            repository.reset(planId, subject).onFailure {
                _colors.value = prev
            }
        }
    }

    fun get(planId: Long) = load(planId)

    /**
     * Zwraca hex dla przedmiotu: custom z bazy, albo deterministyczny hash z BASE_PALETTE.
     * Uzywane przez UI do koloru kafelkow.
     */
    fun colorHexFor(subject: String): String =
        _colors.value[subject]
            ?: com.plaxx.app.ui.theme.BASE_PALETTE[
                subject.sumOf { it.code } % com.plaxx.app.ui.theme.BASE_PALETTE.size
            ]

    /** Czy przedmiot ma custom kolor w bazie. */
    fun hasCustom(subject: String): Boolean = _colors.value.containsKey(subject)

}
