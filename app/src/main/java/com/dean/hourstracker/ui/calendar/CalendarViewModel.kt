package com.dean.hourstracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dean.hourstracker.data.EntryWithProject
import com.dean.hourstracker.data.HoursRepository
import com.dean.hourstracker.data.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarViewModel(private val repository: HoursRepository) : ViewModel() {

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDay = MutableStateFlow<LocalDate?>(null)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = _visibleMonth
        .flatMapLatest { month ->
            val start = month.atDay(1).toEpochDay()
            val end = month.atEndOfMonth().toEpochDay()
            combine(
                repository.observeEntriesInRangeWithProject(start, end),
                repository.observeActiveProjects(),
                _selectedDay,
                _editState,
            ) { entries, projects, selectedDay, editState ->
                buildUiState(month, selectedDay, entries, projects, editState)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun prevMonth() = _visibleMonth.update { it.minusMonths(1) }
    fun nextMonth() = _visibleMonth.update { it.plusMonths(1) }

    fun selectDay(date: LocalDate) {
        _selectedDay.update { if (it == date) null else date }
    }

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

    fun onEditProject(id: Long) = _editState.update { it.copy(projectId = id) }

    fun showEditDatePicker() = _editState.update { it.copy(showDatePicker = true) }
    fun dismissEditDatePicker() = _editState.update { it.copy(showDatePicker = false) }

    fun onEditDateSelected(date: LocalDate) {
        _editState.update { it.copy(date = date, showDatePicker = false) }
    }

    fun onEditDecrement() = _editState.update {
        it.copy(hours = (it.hours - 0.5).coerceAtLeast(0.5))
    }

    fun onEditIncrement() = _editState.update {
        it.copy(hours = (it.hours + 0.5).coerceAtMost(24.0))
    }

    fun onEditQuickPick(hours: Double) = _editState.update { it.copy(hours = (it.hours + hours).coerceAtMost(24.0)) }

    fun onEditNote(note: String) = _editState.update { it.copy(note = note) }

    fun requestDelete() = _editState.update { it.copy(showDeleteConfirm = true) }
    fun dismissDelete() = _editState.update { it.copy(showDeleteConfirm = false) }

    fun confirmDelete() {
        val entry = _editState.value.entry?.entry ?: return
        viewModelScope.launch {
            repository.deleteEntry(entry)
            _editState.value = EditState()
        }
    }

    fun saveEdit() {
        val state = _editState.value
        val original = state.entry?.entry ?: return
        val projectId = state.projectId ?: return
        viewModelScope.launch {
            repository.updateEntry(
                original.copy(
                    projectId = projectId,
                    dateEpochDay = state.date.toEpochDay(),
                    hours = state.hours,
                    note = state.note,
                )
            )
            _editState.value = EditState()
        }
    }

    private fun buildUiState(
        month: YearMonth,
        selectedDay: LocalDate?,
        entries: List<EntryWithProject>,
        projects: List<Project>,
        editState: EditState,
    ): CalendarUiState {
        val byDate = entries.groupBy { LocalDate.ofEpochDay(it.entry.dateEpochDay) }
        val daysWithEntries = byDate.keys.toSet()
        val monthTotalHours = entries.sumOf { it.entry.hours }
        val selectedDayEntries = selectedDay?.let { byDate[it] } ?: emptyList()

        val weekBreakdowns = computeWeekBreakdowns(month, byDate)
        val maxWeekHours = weekBreakdowns.maxOfOrNull { it.hours }?.takeIf { it > 0.0 } ?: 1.0
        val breakdownsWithFraction = weekBreakdowns.map {
            it.copy(fraction = (it.hours / maxWeekHours).toFloat().coerceIn(0f, 1f))
        }

        return CalendarUiState(
            visibleMonth = month,
            selectedDay = selectedDay,
            monthTotalHours = monthTotalHours,
            loggedDaysCount = daysWithEntries.size,
            daysWithEntries = daysWithEntries,
            weekBreakdowns = breakdownsWithFraction,
            selectedDayEntries = selectedDayEntries,
            activeProjects = projects,
            editingEntry = editState.entry,
            editProjectId = editState.projectId,
            editDate = editState.date,
            editHours = editState.hours,
            editNote = editState.note,
            editShowDatePicker = editState.showDatePicker,
            showDeleteConfirm = editState.showDeleteConfirm,
        )
    }

    private fun computeWeekBreakdowns(
        month: YearMonth,
        byDate: Map<LocalDate, List<EntryWithProject>>,
    ): List<WeekBreakdown> {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val fmt = DateTimeFormatter.ofPattern("MMM d")

        var weekStart = monthStart.with(DayOfWeek.MONDAY)
        if (weekStart.isAfter(monthStart)) weekStart = weekStart.minusWeeks(1)

        val result = mutableListOf<WeekBreakdown>()
        while (!weekStart.isAfter(monthEnd)) {
            val weekEnd = weekStart.plusDays(6)
            val weekHours = byDate.entries
                .filter { (date, _) -> !date.isBefore(weekStart) && !date.isAfter(weekEnd) }
                .sumOf { (_, ewps) -> ewps.sumOf { it.entry.hours } }
            result += WeekBreakdown(
                label = "${weekStart.format(fmt)} – ${weekEnd.format(fmt)}",
                hours = weekHours,
                fraction = 0f,
            )
            weekStart = weekStart.plusWeeks(1)
        }
        return result
    }

    class Factory(private val repository: HoursRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalendarViewModel(repository) as T
    }
}
