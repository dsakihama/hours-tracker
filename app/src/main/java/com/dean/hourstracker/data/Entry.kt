package com.dean.hourstracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entries",
    indices = [Index(value = ["projectId"])],
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val dateEpochDay: Long,
    val hours: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
