package com.dean.hourstracker.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dean.hourstracker.data.HoursRepository
import com.dean.hourstracker.util.CsvExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExportPayload(val csvContent: String, val filename: String)

class ReportsViewModel(private val repository: HoursRepository) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.WEEK)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportsUiState> = _period.flatMapLatest { period ->
        val today = LocalDate.now()
        val (start, end) = dateRangeFor(period, today)
        combine(
            repository.observeProjectTotalsInRange(start.toEpochDay(), end.toEpochDay()),
            repository.observeActiveProjects(),
        ) { totals, projects ->
            val totalHours = totals.sumOf { it.totalHours }
            val maxHours = totals.maxOfOrNull { it.totalHours }?.takeIf { it > 0 } ?: 1.0
            val breakdown = totals
                .mapNotNull { t ->
                    val p = projects.find { it.id == t.projectId } ?: return@mapNotNull null
                    ProjectBreakdown(
                        projectId = p.id,
                        projectName = p.name,
                        hours = t.totalHours,
                        fraction = (t.totalHours / maxHours).toFloat().coerceIn(0f, 1f),
                    )
                }
                .sortedByDescending { it.hours }
            ReportsUiState(
                selectedPeriod = period,
                totalHours = totalHours,
                startDate = start,
                endDate = end,
                breakdown = breakdown,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun selectPeriod(period: ReportPeriod) { _period.value = period }

    private val _exportChannel = Channel<ExportPayload>(Channel.BUFFERED)
    val exportEvent: Flow<ExportPayload> = _exportChannel.receiveAsFlow()

    fun exportCurrentPeriod() {
        viewModelScope.launch {
            val state = uiState.value
            val (start, end) = dateRangeFor(state.selectedPeriod)
            val entries = repository.observeEntriesInRangeWithProject(
                start.toEpochDay(), end.toEpochDay()
            ).first()
            if (entries.isEmpty()) return@launch
            val csv = CsvExporter.buildContent(entries)
            val filename = "volunteer-hours-${state.selectedPeriod.name.lowercase()}-$start.csv"
            _exportChannel.send(ExportPayload(csv, filename))
        }
    }

    class Factory(private val repository: HoursRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReportsViewModel(repository) as T
    }
}
