package com.dean.hourstracker.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dean.hourstracker.ui.components.EntryRow
import com.dean.hourstracker.ui.components.HoursControl
import com.dean.hourstracker.ui.theme.BorderDk
import com.dean.hourstracker.ui.theme.DeepTeal
import com.dean.hourstracker.ui.theme.Ink
import com.dean.hourstracker.ui.theme.JetBrainsMono
import com.dean.hourstracker.ui.theme.Linen
import com.dean.hourstracker.ui.theme.Mist
import com.dean.hourstracker.ui.theme.Slate
import com.dean.hourstracker.ui.theme.TextFaint
import com.dean.hourstracker.ui.theme.White
import com.dean.hourstracker.util.formatForLogField
import com.dean.hourstracker.util.formatHours
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete this entry?") },
            text = { Text("This cannot be undone.", color = Slate) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete", color = DeepTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("Cancel", color = Slate) }
            },
        )
    }

    if (uiState.editShowDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.editDate.toEpochDay() * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = viewModel::dismissEditDatePicker,
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
                        viewModel.onEditDateSelected(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    } ?: viewModel.dismissEditDatePicker()
                }) { Text("OK", color = DeepTeal) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEditDatePicker) { Text("Cancel", color = Slate) }
            },
        ) { DatePicker(state = dpState) }
    }

    if (uiState.editingEntry != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::cancelEdit,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = White,
        ) {
            EditEntrySheet(uiState = uiState, viewModel = viewModel)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("History", style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text("All logged entries", style = MaterialTheme.typography.bodyMedium, color = Slate)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Linen),
            )
        },
        containerColor = Linen,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepTeal)
            }
            return@Scaffold
        }

        if (uiState.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No entries yet", style = MaterialTheme.typography.headlineMedium, color = TextFaint)
                    Spacer(Modifier.height(8.dp))
                    Text("Log your first hours on the Log tab.", color = Slate)
                }
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
                TotalHeroCard(totalHours = uiState.totalHours, entryCount = uiState.entryCount)
                Spacer(Modifier.height(4.dp))
            }
            items(uiState.items, key = { item ->
                when (item) {
                    is HistoryListItem.Header -> "header_${item.label}"
                    is HistoryListItem.EntryItem -> item.ewp.entry.id
                }
            }) { item ->
                when (item) {
                    is HistoryListItem.Header -> {
                        Text(
                            text = item.label.uppercase(),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = TextFaint,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                        )
                    }
                    is HistoryListItem.EntryItem -> {
                        EntryRow(
                            ewp = item.ewp,
                            onClick = { viewModel.startEdit(item.ewp) },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TotalHeroCard(totalHours: Double, entryCount: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Mist,
        border = BorderStroke(1.dp, BorderDk),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RUNNING TOTAL",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = DeepTeal,
                )
                Text(
                    text = totalHours.formatHours(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepTeal,
                    letterSpacing = (-0.03 * 30).sp,
                    lineHeight = 34.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("hours logged", fontSize = 11.sp, color = Slate)
                Text("across $entryCount ${if (entryCount == 1) "entry" else "entries"}", fontSize = 11.sp, color = Slate)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEntrySheet(uiState: HistoryUiState, viewModel: HistoryViewModel) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedProject = uiState.activeProjects.find { it.id == uiState.editProjectId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Edit entry", style = MaterialTheme.typography.titleMedium, color = Ink)

        // Project dropdown
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedProject?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Project") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
            ) {
                uiState.activeProjects.forEach { project ->
                    DropdownMenuItem(
                        text = { Text(project.name) },
                        onClick = {
                            viewModel.onEditProject(project.id)
                            dropdownExpanded = false
                        },
                    )
                }
            }
        }

        // Date field
        Surface(
            onClick = viewModel::showEditDatePicker,
            shape = RoundedCornerShape(12.dp),
            color = White,
            border = BorderStroke(1.dp, BorderDk),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 14.dp),
            ) {
                Text(
                    text = uiState.editDate.formatForLogField(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
            }
        }

        // Hours control
        HoursControl(
            hours = uiState.editHours,
            onDecrement = viewModel::onEditDecrement,
            onIncrement = viewModel::onEditIncrement,
            onQuickPick = viewModel::onEditQuickPick,
        )

        // Notes
        OutlinedTextField(
            value = uiState.editNote,
            onValueChange = viewModel::onEditNote,
            label = { Text("Notes · optional") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = viewModel::requestDelete,
                modifier = Modifier.weight(1f),
            ) {
                Text("Delete", color = Slate)
            }
            Button(
                onClick = viewModel::saveEdit,
                enabled = uiState.editProjectId != null,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = White),
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
