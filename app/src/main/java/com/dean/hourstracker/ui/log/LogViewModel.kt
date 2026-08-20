package com.dean.hourstracker.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dean.hourstracker.data.Entry
import com.dean.hourstracker.data.HoursRepository
import com.dean.hourstracker.util.formatHours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class LogViewModel(private val repository: HoursRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState

    private var projectInitialized = false

    init {
        viewModelScope.launch {
            repository.observeActiveProjects().collect { projects ->
                _uiState.update { state ->
                    if (!projectInitialized && projects.isNotEmpty()) {
                        val lastUsedId = repository.getLastUsedProjectId()
                        val defaultId = projects.firstOrNull { it.id == lastUsedId }?.id
                            ?: projects.first().id
                        projectInitialized = true
                        state.copy(
                            activeProjects = projects,
                            selectedProjectId = defaultId,
                            lastUsedProjectId = lastUsedId,
                            isLoading = false,
                        )
                    } else {
                        state.copy(activeProjects = projects)
                    }
                }
            }
        }
    }

    fun selectProject(projectId: Long) =
        _uiState.update { it.copy(selectedProjectId = projectId, showProjectPicker = false) }

    fun selectDate(date: LocalDate) =
        _uiState.update { it.copy(selectedDate = date, showDatePicker = false) }

    fun increment() =
        _uiState.update { it.copy(hours = (it.hours + 0.5).coerceAtMost(24.0)) }

    fun decrement() =
        _uiState.update { it.copy(hours = (it.hours - 0.5).coerceAtLeast(0.5)) }

    fun quickPick(hours: Double) =
        _uiState.update { it.copy(hours = (it.hours + hours).coerceAtMost(24.0)) }

    fun onNoteChange(note: String) =
        _uiState.update { it.copy(note = note) }

    fun showProjectPicker() =
        _uiState.update { it.copy(showProjectPicker = true) }

    fun dismissProjectPicker() =
        _uiState.update { it.copy(showProjectPicker = false) }

    fun showDatePicker() =
        _uiState.update { it.copy(showDatePicker = true) }

    fun dismissDatePicker() =
        _uiState.update { it.copy(showDatePicker = false) }

    fun setTimerMode(enabled: Boolean) = _uiState.update {
        it.copy(
            timerMode = enabled,
            timerStartMs = null,
            timerAccumulatedMs = 0L,
            timerCapturedSecs = null,
        )
    }

    fun startTimer() = _uiState.update {
        it.copy(timerStartMs = System.currentTimeMillis(), timerAccumulatedMs = 0L)
    }

    fun pauseTimer() {
        val state = _uiState.value
        val startMs = state.timerStartMs ?: return
        val banked = state.timerAccumulatedMs + (System.currentTimeMillis() - startMs)
        _uiState.update { it.copy(timerStartMs = null, timerAccumulatedMs = banked) }
    }

    fun resumeTimer() = _uiState.update {
        it.copy(timerStartMs = System.currentTimeMillis())
    }

    fun stopTimer() {
        val state = _uiState.value
        val totalMs = if (state.timerStartMs != null) {
            state.timerAccumulatedMs + (System.currentTimeMillis() - state.timerStartMs)
        } else {
            state.timerAccumulatedMs
        }
        val elapsedSecs = totalMs / 1000L
        val roundedUp = maxOf(0.5, kotlin.math.ceil(totalMs / 3_600_000.0 * 2) / 2)
        _uiState.update {
            it.copy(
                timerStartMs = null,
                timerAccumulatedMs = 0L,
                timerCapturedSecs = elapsedSecs,
                hours = roundedUp,
            )
        }
    }

    fun discardEntry() = _uiState.update {
        it.copy(hours = 1.0, note = "", selectedDate = LocalDate.now())
    }

    fun discardTimer() = _uiState.update {
        it.copy(
            timerStartMs = null,
            timerAccumulatedMs = 0L,
            timerCapturedSecs = null,
            hours = 1.0,
            note = "",
        )
    }

    fun save() {
        val state = _uiState.value
        val projectId = state.selectedProjectId ?: return
        val hoursLogged = state.hours
        viewModelScope.launch {
            repository.addEntry(
                Entry(
                    projectId = projectId,
                    dateEpochDay = state.selectedDate.toEpochDay(),
                    hours = hoursLogged,
                    note = state.note.trim(),
                )
            )
            _uiState.update {
                it.copy(
                    hours = 1.0,
                    note = "",
                    selectedDate = LocalDate.now(),
                    lastUsedProjectId = projectId,
                    savedMessage = "Saved — ${hoursLogged.formatHours()} hrs logged",
                    timerCapturedSecs = null,
                )
            }
        }
    }

    fun onSnackbarShown() =
        _uiState.update { it.copy(savedMessage = null) }

    class Factory(private val repository: HoursRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LogViewModel(repository) as T
    }
}
