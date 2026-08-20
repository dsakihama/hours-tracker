package com.dean.hourstracker.data

import androidx.room.ColumnInfo

data class ProjectTotal(
    val projectId: Long,
    @ColumnInfo(name = "totalHours") val totalHours: Double,
)
