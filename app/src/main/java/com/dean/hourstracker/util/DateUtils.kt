package com.dean.hourstracker.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun LocalDate.formatForLogField(): String {
    val today = LocalDate.now()
    val monthDay = DateTimeFormatter.ofPattern("MMM d").format(this)
    return if (this == today) "Today · $monthDay"
    else "${dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} · $monthDay"
}

fun LocalDate.formatShort(): String =
    DateTimeFormatter.ofPattern("MMM d").format(this)

fun LocalDate.historyGroupLabel(today: LocalDate = LocalDate.now()): String {
    val thisWeekStart = today.with(DayOfWeek.MONDAY)
    val lastWeekStart = thisWeekStart.minusWeeks(1)
    return when {
        !isBefore(thisWeekStart) -> "This week"
        !isBefore(lastWeekStart) -> "Last week"
        else -> month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + year
    }
}

fun Double.formatHours(): String =
    if (this % 1.0 == 0.0) "%.0f".format(this)
    else "%.2f".format(this).trimEnd('0')
