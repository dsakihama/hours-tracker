package com.dean.hourstracker.ui.log

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dean.hourstracker.data.Project
import com.dean.hourstracker.ui.components.HoursControl
import com.dean.hourstracker.ui.theme.BorderDk
import com.dean.hourstracker.ui.theme.BorderLt
import com.dean.hourstracker.ui.theme.DeepTeal
import com.dean.hourstracker.ui.theme.Ink
import com.dean.hourstracker.ui.theme.JetBrainsMono
import com.dean.hourstracker.ui.theme.Linen
import com.dean.hourstracker.ui.theme.Mist
import com.dean.hourstracker.ui.theme.Slate
import com.dean.hourstracker.ui.theme.TextFaint
import com.dean.hourstracker.ui.theme.White
import com.dean.hourstracker.util.formatForLogField
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    viewModel: LogViewModel,
    onNavigateToProjects: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Ticks every 500 ms while the timer is running.  Reseeds from timerStartMs on every
    // recomposition (including after navigating away and back) so the displayed time is always
    // correct without any background service.
    var elapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(uiState.timerStartMs, uiState.timerAccumulatedMs) {
        val startMs = uiState.timerStartMs
        val accMs = uiState.timerAccumulatedMs
        if (startMs == null) {
            elapsedMs = accMs   // paused: freeze at accumulated time; idle: 0
            return@LaunchedEffect
        }
        while (true) {
            elapsedMs = accMs + (System.currentTimeMillis() - startMs)
            delay(500L)
        }
    }

    LaunchedEffect(uiState.savedMessage) {
        val msg = uiState.savedMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.onSnackbarShown()
    }

    if (uiState.showProjectPicker) {
        ProjectPickerSheet(
            projects = uiState.activeProjects,
            selectedId = uiState.selectedProjectId,
            lastUsedId = uiState.lastUsedProjectId,
            onSelect = viewModel::selectProject,
            onDismiss = viewModel::dismissProjectPicker,
            onManageProjects = {
                viewModel.dismissProjectPicker()
                onNavigateToProjects()
            },
        )
    }

    if (uiState.showDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate.toEpochDay() * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = viewModel::dismissDatePicker,
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.selectDate(date)
                    } ?: viewModel.dismissDatePicker()
                }) { Text("OK", color = DeepTeal) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDatePicker) { Text("Cancel", color = Slate) }
            },
        ) {
            DatePicker(state = dpState)
        }
    }

    Scaffold(
        topBar = { LogTopBar(date = uiState.selectedDate, onNavigateToSettings = onNavigateToSettings) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Linen,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepTeal)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            ModeToggle(
                timerMode = uiState.timerMode,
                onManual = { viewModel.setTimerMode(false) },
                onTimer = { viewModel.setTimerMode(true) },
            )

            FieldLabel("Project")
            ProjectField(
                project = uiState.selectedProject,
                isLastUsed = uiState.selectedProjectId == uiState.lastUsedProjectId,
                onClick = viewModel::showProjectPicker,
            )

            FieldLabel("Date")
            DateField(
                date = uiState.selectedDate,
                onClick = viewModel::showDatePicker,
            )

            if (uiState.timerMode && uiState.timerCapturedSecs == null) {
                // Timer tab: idle, running, or paused
                FieldLabel("Timer")
                TimerDisplay(elapsedMs = elapsedMs, paused = uiState.timerPaused)
                Spacer(Modifier.height(16.dp))
                if (uiState.timerRunning || uiState.timerPaused) {
                    // Pause/Resume + Stop side by side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Pause / Resume (outlined — softer action)
                        OutlinedButton(
                            onClick = if (uiState.timerPaused) viewModel::resumeTimer else viewModel::pauseTimer,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.5.dp, DeepTeal),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal),
                        ) {
                            Text(
                                text = if (uiState.timerPaused) "Resume" else "Pause",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        // Stop (filled — finalises session)
                        Button(
                            onClick = viewModel::stopTimer,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepTeal,
                                contentColor = White,
                            ),
                        ) {
                            Text("Stop", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Idle — single full-width Start button
                    Button(
                        onClick = viewModel::startTimer,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepTeal,
                            contentColor = White,
                        ),
                    ) {
                        Text("Start timer", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Manual entry, or pre-populated after timer stop
                if (uiState.timerCapturedSecs != null) {
                    CapturedBanner(elapsedSecs = uiState.timerCapturedSecs!!)
                }
                FieldLabel(
                    text = "Hours",
                    badge = if (uiState.timerCapturedSecs != null) "FROM TIMER" else null,
                )
                HoursControl(
                    hours = uiState.hours,
                    onDecrement = viewModel::decrement,
                    onIncrement = viewModel::increment,
                    onQuickPick = viewModel::quickPick,
                )
                FieldLabel("Notes · optional")
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChange,
                    placeholder = { Text("Add a note…", color = TextFaint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::save,
                    enabled = uiState.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepTeal,
                        contentColor = White,
                    ),
                ) {
                    Text("Save entry", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                val discardLabel = if (uiState.timerCapturedSecs != null) "Discard timer" else "Discard entry"
                val discardAction = if (uiState.timerCapturedSecs != null) viewModel::discardTimer else viewModel::discardEntry
                OutlinedButton(
                    onClick = discardAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, BorderLt),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate),
                ) {
                    Text(discardLabel, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Mode toggle (Manual | Timer) ─────────────────────────────────────────────

@Composable
private fun ModeToggle(
    timerMode: Boolean,
    onManual: () -> Unit,
    onTimer: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = White,
        border = BorderStroke(1.dp, BorderLt),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ModeTab(label = "Manual", selected = !timerMode, onClick = onManual, modifier = Modifier.weight(1f))
            ModeTab(label = "Timer",  selected = timerMode,  onClick = onTimer,  modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        color = if (selected) DeepTeal else Color.Transparent,
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) White else Slate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        )
    }
}

// ── Timer display ─────────────────────────────────────────────────────────────

@Composable
private fun TimerDisplay(elapsedMs: Long, paused: Boolean = false) {
    val totalSecs = elapsedMs / 1000
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    val text = "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Mist,
        border = BorderStroke(1.dp, BorderDk),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = text,
                fontFamily = JetBrainsMono,
                fontSize = 36.sp,
                fontWeight = FontWeight.Medium,
                color = if (paused) TextFaint else DeepTeal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (paused) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "PAUSED",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextFaint,
                    letterSpacing = 0.12.sp,
                )
            }
        }
    }
}

// ── Captured-time banner (shown after timer stops, until entry is saved) ──────

@Composable
private fun CapturedBanner(elapsedSecs: Long) {
    val h = elapsedSecs / 3600
    val m = (elapsedSecs % 3600) / 60
    val s = elapsedSecs % 60
    val rawText = "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Mist,
        border = BorderStroke(1.dp, BorderDk),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = DeepTeal,
                modifier = Modifier.size(18.dp),
            )
            Column {
                Text(
                    text = "TIMER CAPTURED",
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepTeal,
                )
                Text(
                    text = "$rawText → rounded up to nearest ½ hr",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = Slate,
                )
            }
        }
    }
}

// ── Shared field helpers ──────────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String, badge: String? = null) {
    val parts = text.split(" · ")
    Row(
        modifier = Modifier.padding(top = 16.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = parts[0].uppercase(),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = TextFaint,
        )
        if (parts.size > 1) {
            Text(
                text = " · ${parts[1]}",
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = TextFaint,
            )
        }
        if (badge != null) {
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Mist,
                border = BorderStroke(1.dp, BorderDk),
            ) {
                Text(
                    text = badge,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                    color = DeepTeal,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogTopBar(date: LocalDate, onNavigateToSettings: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text("Log Hours", style = MaterialTheme.typography.headlineMedium, color = Ink)
                Text(
                    text = date.formatForLogField(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate,
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = DeepTeal)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Linen),
    )
}

@Composable
private fun ProjectField(
    project: Project?,
    isLastUsed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = White,
        border = BorderStroke(1.dp, BorderDk),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = project?.name ?: "Select a project",
                style = MaterialTheme.typography.titleMedium,
                color = if (project != null) Ink else TextFaint,
                modifier = Modifier.weight(1f),
            )
            if (isLastUsed && project != null) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Mist,
                    border = BorderStroke(1.dp, BorderDk),
                ) {
                    Text(
                        text = "LAST USED",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        color = DeepTeal,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = TextFaint)
        }
    }
}

@Composable
private fun DateField(date: LocalDate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = White,
        border = BorderStroke(1.dp, BorderDk),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date.formatForLogField(),
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Outlined.ChevronRight, null, tint = TextFaint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectPickerSheet(
    projects: List<Project>,
    selectedId: Long?,
    lastUsedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
    onManageProjects: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
    ) {
        Text(
            text = "Select project",
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(projects, key = { it.id }) { project ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(project.id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (project.id == selectedId) DeepTeal else Ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (project.id == lastUsedId) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Mist,
                        ) {
                            Text(
                                text = "LAST USED",
                                fontFamily = JetBrainsMono,
                                fontSize = 9.sp,
                                color = DeepTeal,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = BorderDk.copy(alpha = 0.5f))
            }
            item {
                TextButton(
                    onClick = onManageProjects,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        "Manage projects →",
                        color = DeepTeal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
