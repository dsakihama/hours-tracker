package com.dean.hourstracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Transaction
    @Query("SELECT * FROM entries ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeAllWithProject(): Flow<List<EntryWithProject>>

    @Query("SELECT projectId FROM entries ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastUsedProjectId(): Long?

    @Query("""
        SELECT * FROM entries
        WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY dateEpochDay DESC, createdAt DESC
    """)
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<Entry>>

    @Transaction
    @Query("""
        SELECT * FROM entries
        WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY dateEpochDay DESC, createdAt DESC
    """)
    fun observeInRangeWithProject(startEpochDay: Long, endEpochDay: Long): Flow<List<EntryWithProject>>

    @Query("""
        SELECT projectId, COALESCE(SUM(hours), 0) AS totalHours
        FROM entries
        WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY projectId
    """)
    fun observeProjectTotalsInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ProjectTotal>>

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)
}
