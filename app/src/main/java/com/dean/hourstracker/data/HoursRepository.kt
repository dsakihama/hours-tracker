package com.dean.hourstracker.data

import kotlinx.coroutines.flow.Flow

class HoursRepository(
    private val projectDao: ProjectDao,
    private val entryDao: EntryDao,
) {
    // Projects
    fun observeActiveProjectsWithStats(): Flow<List<ProjectWithStats>> =
        projectDao.observeActiveWithStats()

    fun observeArchivedProjectsWithStats(): Flow<List<ProjectWithStats>> =
        projectDao.observeArchivedWithStats()

    fun observeActiveProjects(): Flow<List<Project>> =
        projectDao.observeActive()

    suspend fun addProject(name: String): Long =
        projectDao.insert(Project(name = name))

    suspend fun renameProject(project: Project, newName: String) =
        projectDao.update(project.copy(name = newName))

    suspend fun setArchived(project: Project, archived: Boolean) =
        projectDao.update(project.copy(isArchived = archived))

    suspend fun getLastUsedProjectId(): Long? =
        entryDao.getLastUsedProjectId()

    // Entries
    fun observeAllEntriesWithProject(): Flow<List<EntryWithProject>> =
        entryDao.observeAllWithProject()

    fun observeEntriesInRangeWithProject(start: Long, end: Long): Flow<List<EntryWithProject>> =
        entryDao.observeInRangeWithProject(start, end)

    fun observeProjectTotalsInRange(start: Long, end: Long): Flow<List<ProjectTotal>> =
        entryDao.observeProjectTotalsInRange(start, end)

    suspend fun addEntry(entry: Entry): Long =
        entryDao.insert(entry)

    suspend fun updateEntry(entry: Entry) =
        entryDao.update(entry)

    suspend fun deleteEntry(entry: Entry) =
        entryDao.delete(entry)
}
