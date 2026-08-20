package com.dean.hourstracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dean.hourstracker.data.EntryWithProject
import com.dean.hourstracker.data.HoursRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class HistoryViewModel(private val repository: HoursRepository) : ViewModel() {

    private data class EditState(
        val entry: EntryWithProject? = null,
        val projectId: Long? = null,
        val date: LocalDate = LocalDate.now(),
        val hours: Double = 1.0,
        val note: String = "",
        val showDatePicker: Boolean = false,
        val showDeleteConfirm: Boolean = false,
    )

    private val _editState = MutableStateFlow(EditState())

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeAllEntriesWithProject(),
        repository.observeActiveProjects(),
        _editState,
    ) { entries, projects, edit ->
        HistoryUiState(
            totalHours = entries.sumOf { it.entry.hours },
            entryCount = entries.size,
            items = groupEntries(entries),
            activeProjects = projects,
            editingEntry = edit.entry,
            editProjectId = edit.projectId,
            editDate = edit.date,
            editHours = edit.hours,
            editNote = edit.note,
            editShowDatePicker = edit.showDatePicker,
            showDeleteConfirm = edit.showDeleteConfirm,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun startEdit(ewp: EntryWithProject) {
        _editState.value = EditState(
            entry = ewp,
            projectId = ewp.entry.projectId,
            date = LocalDate.ofEpochDay(ewp.entry.dateEpochDay),
            hours = ewp.entry.hours,
            note = ewp.entry.note,
        )
    }

    fun cancelEdit() { _editState.value = EditState() }

    fun onEditProject(projectId: Long) = _editState.update { it.copy(projectId = projectId) }
    fun onEditDateSelected(date: LocalDate) = _editState.update { it.copy(date = date, showDatePicker = false) }
    fun onEditIncrement() = _editState.update { it.copy(hours = (it.hours + 0.5).coerceAtMost(24.0)) }
    fun onEditDecrement() = _editState.update { it.copy(hours = (it.hours - 0.5).coerceAtLeast(0.5)) }
    fun onEditQuickPick(hours: Double) = _editState.update { it.copy(hours = (it.hours + hours).coerceAtMost(24.0)) }
    fun onEditNote(note: String) = _editState.update { it.copy(note = note) }
    fun showEditDatePicker() = _editState.update { it.copy(showDatePicker = true) }
    fun dismissEditDatePicker() = _editState.update { it.copy(showDatePicker = false) }
    fun requestDelete() = _editState.update { it.copy(showDeleteConfirm = true) }
    fun dismissDelete() = _editState.update { it.copy(showDeleteConfirm = false) }

    fun saveEdit() {
        val edit = _editState.value
        val entry = edit.entry ?: return
        val projectId = edit.projectId ?: return
        viewModelScope.launch {
            repository.updateEntry(
                entry.entry.copy(
                    projectId = projectId,
                    dateEpochDay = edit.date.toEpochDay(),
                    hours = edit.hours,
                    note = edit.note.trim(),
                )
            )
            _editState.value = EditState()
        }
    }

    fun confirmDelete() {
        val entry = _editState.value.entry ?: return
        viewModelScope.launch {
            repository.deleteEntry(entry.entry)
            _editState.value = EditState()
        }
    }

    private fun groupEntries(entries: List<EntryWithProject>): List<HistoryListItem> {
        if (entries.isEmpty()) return emptyList()
        val today = LocalDate.now()
        val thisWeekStart = today.with(DayOfWeek.MONDAY)
        val lastWeekStart = thisWeekStart.minusWeeks(1)
        val result = mutableListOf<HistoryListItem>()
        var currentLabel = ""
        for (ewp in entries) {
            val date = LocalDate.ofEpochDay(ewp.entry.dateEpochDay)
            val label = when {
                !date.isBefore(thisWeekStart) -> "This week"
                !date.isBefore(lastWeekStart) -> "Last week"
                else -> date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + date.year
            }
            if (label != currentLabel) {
                result += HistoryListItem.Header(label)
                currentLabel = label
            }
            result += HistoryListItem.EntryItem(ewp)
        }
        return result
    }

    class Factory(private val repository: HoursRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(repository) as T
    }
}
