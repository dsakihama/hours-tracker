package com.dean.hourstracker.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dean.hourstracker.data.Project
import com.dean.hourstracker.data.ProjectWithStats
import com.dean.hourstracker.ui.theme.DeepTeal
import com.dean.hourstracker.ui.theme.HoursTrackerTheme
import com.dean.hourstracker.ui.theme.Ink
import com.dean.hourstracker.ui.theme.JetBrainsMono
import com.dean.hourstracker.ui.theme.Linen
import com.dean.hourstracker.ui.theme.Mist
import com.dean.hourstracker.ui.theme.Slate
import com.dean.hourstracker.ui.theme.TextFaint
import com.dean.hourstracker.ui.theme.White

@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.renamingProject != null) {
        RenameDialog(
            currentName = uiState.renamingProject!!.project.name,
            renameText = uiState.renameText,
            onTextChange = viewModel::onRenameTextChange,
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::dismissRename,
        )
    }

    if (uiState.archivingProject != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissArchive,
            title = { Text("Archive \"${uiState.archivingProject!!.project.name}\"?") },
            text = { Text("The project won't appear in the picker, but all logged entries keep their data.", color = Slate) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmArchive) { Text("Archive", color = DeepTeal) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissArchive) { Text("Cancel", color = Slate) }
            },
        )
    }

    Scaffold(
        topBar = {
            ProjectsTopBar(onNavigateBack = onNavigateBack)
        },
        containerColor = Linen,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepTeal)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionLabel("Add a project")
                AddProjectRow(
                    value = uiState.newProjectName,
                    onValueChange = viewModel::onNewProjectNameChange,
                    onAdd = viewModel::addProject,
                )
                Spacer(Modifier.height(4.dp))
            }

            item { SectionLabel("Active") }

            items(uiState.activeProjects, key = { it.project.id }) { pws ->
                val tag = when {
                    pws.project.id == uiState.lastUsedProjectId -> ProjectTag.IN_USE
                    pws.project.isPreset -> ProjectTag.PRESET
                    else -> ProjectTag.CUSTOM
                }
                ProjectRow(
                    pws = pws,
                    tag = tag,
                    onRename = { viewModel.startRename(pws) },
                    onArchive = { viewModel.requestArchive(pws) },
                )
            }

            if (uiState.archivedProjects.isNotEmpty()) {
                item { SectionLabel("Archived") }

                items(uiState.archivedProjects, key = { "archived_${it.project.id}" }) { pws ->
                    ProjectRow(
                        pws = pws,
                        tag = ProjectTag.ARCHIVED,
                        onRename = { viewModel.startRename(pws) },
                        onArchive = { viewModel.unarchiveProject(pws) },
                        archived = true,
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Projects",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                )
                Text(
                    text = "Preset + your own",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate,
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Slate)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Linen),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.06.sp,
        color = TextFaint,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun AddProjectRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("New project name…", color = TextFaint) },
            singleLine = true,
            shape = RoundedCornerShape(11.dp),
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                onAdd()
                keyboard?.hide()
            }),
        )
        FilledIconButton(
            onClick = {
                onAdd()
                keyboard?.hide()
            },
            shape = RoundedCornerShape(11.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = DeepTeal),
            modifier = Modifier.width(46.dp).height(56.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add project", tint = White)
        }
    }
}

private enum class ProjectTag { PRESET, CUSTOM, IN_USE, ARCHIVED }

@Composable
private fun ProjectRow(
    pws: ProjectWithStats,
    tag: ProjectTag,
    onRename: () -> Unit,
    onArchive: () -> Unit,
    archived: Boolean = false,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(11.dp),
        color = White,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pws.project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (archived) TextFaint else Ink,
                )
                Spacer(Modifier.height(2.dp))
                val meta = if (archived) {
                    "%.1f hrs · kept on old entries".format(pws.totalHours)
                } else {
                    "%.1f hrs · %d %s".format(
                        pws.totalHours,
                        pws.entryCount,
                        if (pws.entryCount == 1) "entry" else "entries",
                    )
                }
                Text(text = meta, fontSize = 11.sp, color = TextFaint)
            }
            Spacer(Modifier.width(8.dp))
            TagChip(tag)
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Options", tint = TextFaint)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuExpanded = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (archived) "Unarchive" else "Archive") },
                        onClick = { menuExpanded = false; onArchive() },
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(tag: ProjectTag) {
    val (label, textColor, bgColor) = when (tag) {
        ProjectTag.IN_USE  -> Triple("IN USE",   DeepTeal,  Mist)
        ProjectTag.PRESET  -> Triple("PRESET",   DeepTeal,  Mist)
        ProjectTag.CUSTOM  -> Triple("CUSTOM",   DeepTeal,  Mist)
        ProjectTag.ARCHIVED -> Triple("ARCHIVED", TextFaint, Linen)
    }
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bgColor,
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.05.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    renameText: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename \"$currentName\"") },
        text = {
            OutlinedTextField(
                value = renameText,
                onValueChange = onTextChange,
                singleLine = true,
                shape = RoundedCornerShape(11.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = renameText.isNotBlank()) {
                Text("Rename", color = DeepTeal)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate) }
        },
    )
}

// --- Preview ---

@Preview(showBackground = true)
@Composable
private fun ProjectsScreenPreview() {
    HoursTrackerTheme {
        val fakeProjects = listOf(
            ProjectWithStats(Project(1, "Food Bank",      isPreset = true), 42.0, 12),
            ProjectWithStats(Project(2, "Animal Shelter", isPreset = false), 28.5, 9),
            ProjectWithStats(Project(3, "Beach Cleanup",  isPreset = true), 14.0, 5),
        )
        val fakeArchived = listOf(
            ProjectWithStats(Project(4, "Blood Drive", isPreset = false, isArchived = true), 5.0, 2),
        )
        val fakeState = ProjectsUiState(
            activeProjects = fakeProjects,
            archivedProjects = fakeArchived,
            lastUsedProjectId = 1L,
            isLoading = false,
        )
        // Inline preview scaffold — no real ViewModel needed
        ProjectsScreenContent(state = fakeState, onNavigateBack = {})
    }
}

@Composable
internal fun ProjectsScreenContent(
    state: ProjectsUiState,
    onNewProjectNameChange: (String) -> Unit = {},
    onAdd: () -> Unit = {},
    onStartRename: (ProjectWithStats) -> Unit = {},
    onArchive: (ProjectWithStats) -> Unit = {},
    onUnarchive: (ProjectWithStats) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        topBar = { ProjectsTopBar(onNavigateBack = onNavigateBack) },
        containerColor = Linen,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionLabel("Add a project")
                AddProjectRow(
                    value = state.newProjectName,
                    onValueChange = onNewProjectNameChange,
                    onAdd = onAdd,
                )
                Spacer(Modifier.height(4.dp))
            }

            item { SectionLabel("Active") }

            items(state.activeProjects, key = { it.project.id }) { pws ->
                val tag = when {
                    pws.project.id == state.lastUsedProjectId -> ProjectTag.IN_USE
                    pws.project.isPreset -> ProjectTag.PRESET
                    else -> ProjectTag.CUSTOM
                }
                ProjectRow(pws, tag, { onStartRename(pws) }, { onArchive(pws) })
            }

            if (state.archivedProjects.isNotEmpty()) {
                item { SectionLabel("Archived") }
                items(state.archivedProjects, key = { "archived_${it.project.id}" }) { pws ->
                    ProjectRow(pws, ProjectTag.ARCHIVED, { onStartRename(pws) }, { onUnarchive(pws) }, archived = true)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
