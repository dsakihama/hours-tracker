package com.dean.hourstracker.ui.projects

import com.dean.hourstracker.data.ProjectWithStats

data class ProjectsUiState(
    val activeProjects: List<ProjectWithStats> = emptyList(),
    val archivedProjects: List<ProjectWithStats> = emptyList(),
    val lastUsedProjectId: Long? = null,
    val newProjectName: String = "",
    val renamingProject: ProjectWithStats? = null,
    val renameText: String = "",
    val archivingProject: ProjectWithStats? = null,
    val isLoading: Boolean = true,
)
