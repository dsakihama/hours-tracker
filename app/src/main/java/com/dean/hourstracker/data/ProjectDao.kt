package com.dean.hourstracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("""
        SELECT p.*, COALESCE(SUM(e.hours), 0) AS totalHours, COUNT(e.id) AS entryCount
        FROM projects p
        LEFT JOIN entries e ON e.projectId = p.id
        WHERE p.isArchived = 0
        GROUP BY p.id
        ORDER BY p.createdAt ASC
    """)
    fun observeActiveWithStats(): Flow<List<ProjectWithStats>>

    @Query("""
        SELECT p.*, COALESCE(SUM(e.hours), 0) AS totalHours, COUNT(e.id) AS entryCount
        FROM projects p
        LEFT JOIN entries e ON e.projectId = p.id
        WHERE p.isArchived = 1
        GROUP BY p.id
        ORDER BY p.createdAt ASC
    """)
    fun observeArchivedWithStats(): Flow<List<ProjectWithStats>>

    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<Project>>

    @Query("SELECT * FROM projects ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Project>>

    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)
}
