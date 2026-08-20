package com.dean.hourstracker.data

import androidx.room.Embedded
import androidx.room.Relation

data class EntryWithProject(
    @Embedded val entry: Entry,
    @Relation(parentColumn = "projectId", entityColumn = "id")
    val project: Project,
)
