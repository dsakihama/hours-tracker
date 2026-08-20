package com.dean.hourstracker.data

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class ProjectWithStats(
    @Embedded val project: Project,
    @ColumnInfo(name = "totalHours") val totalHours: Double,
    @ColumnInfo(name = "entryCount") val entryCount: Int,
)
