package com.dean.hourstracker.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dean.hourstracker.data.HoursRepository
import com.dean.hourstracker.data.ProjectWithStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProjectsViewModel(private val repository: HoursRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                repository.observeActiveProjectsWithStats(),
                repository.observeArchivedProjectsWithStats(),
            ) { active, archived -> active to archived }
                .collect { (active, archived) ->
                    val lastUsed = repository.getLastUsedProjectId()
                    _uiState.update {
                        it.copy(
                            activeProjects = active,
                            archivedProjects = archived,
                            lastUsedProjectId = lastUsed,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun onNewProjectNameChange(name: String) {
        _uiState.update { it.copy(newProjectName = name) }
    }

    fun addProject() {
        val name = _uiState.value.newProjectName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addProject(name)
            _uiState.update { it.copy(newProjectName = "") }
        }
    }

    fun startRename(project: ProjectWithStats) {
        _uiState.update { it.copy(renamingProject = project, renameText = project.project.name) }
    }

    fun onRenameTextChange(text: String) {
        _uiState.update { it.copy(renameText = text) }
    }

    fun confirmRename() {
        val state = _uiState.value
        val project = state.renamingProject ?: return
        val newName = state.renameText.trim()
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameProject(project.project, newName)
            _uiState.update { it.copy(renamingProject = null, renameText = "") }
        }
    }

    fun dismissRename() {
        _uiState.update { it.copy(renamingProject = null, renameText = "") }
    }

    fun requestArchive(project: ProjectWithStats) {
        _uiState.update { it.copy(archivingProject = project) }
    }

    fun confirmArchive() {
        val project = _uiState.value.archivingProject ?: return
        viewModelScope.launch {
            repository.setArchived(project.project, archived = true)
            _uiState.update { it.copy(archivingProject = null) }
        }
    }

    fun dismissArchive() {
        _uiState.update { it.copy(archivingProject = null) }
    }

    fun unarchiveProject(project: ProjectWithStats) {
        viewModelScope.launch { repository.setArchived(project.project, archived = false) }
    }

    class Factory(private val repository: HoursRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProjectsViewModel(repository) as T
    }
}
