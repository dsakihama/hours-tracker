package com.dean.hourstracker.util

import com.dean.hourstracker.data.EntryWithProject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object CsvExporter {

    fun buildContent(entries: List<EntryWithProject>): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Project,Hours,Note")
        entries
            .sortedWith(compareBy({ it.entry.dateEpochDay }, { it.entry.createdAt }))
            .forEach { ewp ->
                val date = LocalDate.ofEpochDay(ewp.entry.dateEpochDay)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                sb.appendLine(
                    "${field(date)},${field(ewp.project.name)},${ewp.entry.hours},${field(ewp.entry.note)}"
                )
            }
        return sb.toString()
    }

    private fun field(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' })
            "\"${value.replace("\"", "\"\"")}\""
        else value
}
