package com.dean.hourstracker.ui.history

import com.dean.hourstracker.data.EntryWithProject
import com.dean.hourstracker.data.Project
import java.time.LocalDate

data class HistoryUiState(
    val totalHours: Double = 0.0,
    val entryCount: Int = 0,
    val items: List<HistoryListItem> = emptyList(),
    val activeProjects: List<Project> = emptyList(),
    // edit sheet
    val editingEntry: EntryWithProject? = null,
    val editProjectId: Long? = null,
    val editDate: LocalDate = LocalDate.now(),
    val editHours: Double = 1.0,
    val editNote: String = "",
    val editShowDatePicker: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface HistoryListItem {
    data class Header(val label: String) : HistoryListItem
    data class EntryItem(val ewp: EntryWithProject) : HistoryListItem
}
