package com.dean.hourstracker.ui.reports

import java.time.LocalDate

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.WEEK,
    val totalHours: Double = 0.0,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now(),
    val breakdown: List<ProjectBreakdown> = emptyList(),
)

data class ProjectBreakdown(
    val projectId: Long,
    val projectName: String,
    val hours: Double,
    val fraction: Float,
)
