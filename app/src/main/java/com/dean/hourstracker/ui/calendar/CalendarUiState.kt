package com.dean.hourstracker.ui.calendar

import com.dean.hourstracker.data.EntryWithProject
import com.dean.hourstracker.data.Project
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate? = null,
    val monthTotalHours: Double = 0.0,
    val loggedDaysCount: Int = 0,
    val daysWithEntries: Set<LocalDate> = emptySet(),
    val weekBreakdowns: List<WeekBreakdown> = emptyList(),
    val selectedDayEntries: List<EntryWithProject> = emptyList(),
    val activeProjects: List<Project> = emptyList(),
    val editingEntry: EntryWithProject? = null,
    val editProjectId: Long? = null,
    val editDate: LocalDate = LocalDate.now(),
    val editHours: Double = 1.0,
    val editNote: String = "",
    val editShowDatePicker: Boolean = false,
    val showDeleteConfirm: Boolean = false,
)

data class WeekBreakdown(
    val label: String,
    val hours: Double,
    val fraction: Float,
)
