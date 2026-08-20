package com.dean.hourstracker.ui.log

import com.dean.hourstracker.data.Project
import java.time.LocalDate

data class LogUiState(
    val activeProjects: List<Project> = emptyList(),
    val selectedProjectId: Long? = null,
    val lastUsedProjectId: Long? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val hours: Double = 1.0,
    val note: String = "",
    val showProjectPicker: Boolean = false,
    val showDatePicker: Boolean = false,
    val savedMessage: String? = null,
    val isLoading: Boolean = true,
    // Timer fields
    val timerMode: Boolean = false,
    val timerStartMs: Long? = null,         // non-null only while actively ticking
    val timerAccumulatedMs: Long = 0L,      // banked ms from completed run segments (pauses)
    val timerCapturedSecs: Long? = null,    // set after stop; drives captured-time banner
) {
    val selectedProject: Project? get() = activeProjects.find { it.id == selectedProjectId }
    val canSave: Boolean get() = selectedProject != null && hours > 0
    val timerRunning: Boolean get() = timerStartMs != null
    val timerPaused: Boolean get() = timerStartMs == null && timerAccumulatedMs > 0 && timerCapturedSecs == null
}
