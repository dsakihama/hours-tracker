package com.dean.hourstracker.ui.reports

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class ReportPeriod(val label: String) {
    WEEK("Week"), MONTH("Month"), QUARTER("Quarter"), YEAR("Year")
}

fun dateRangeFor(period: ReportPeriod, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> =
    when (period) {
        ReportPeriod.WEEK -> {
            val start = today.with(DayOfWeek.MONDAY)
            start to start.plusDays(6)
        }
        ReportPeriod.MONTH -> {
            val start = today.withDayOfMonth(1)
            start to today.with(TemporalAdjusters.lastDayOfMonth())
        }
        ReportPeriod.QUARTER -> {
            val qMonth = ((today.monthValue - 1) / 3) * 3 + 1
            val start = LocalDate.of(today.year, qMonth, 1)
            start to start.plusMonths(3).minusDays(1)
        }
        ReportPeriod.YEAR ->
            LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
    }

fun rangeLabel(start: LocalDate, end: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("MMM d")
    return "${start.format(fmt).uppercase()} – ${end.format(fmt).uppercase()}, ${start.year}"
}
