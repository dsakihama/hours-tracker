package com.dean.hourstracker.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dean.hourstracker.data.EntryWithProject
import com.dean.hourstracker.ui.components.EntryRow
import com.dean.hourstracker.ui.components.HoursControl
import com.dean.hourstracker.ui.theme.BorderDk
import com.dean.hourstracker.ui.theme.BorderLt
import com.dean.hourstracker.ui.theme.DeepTeal
import com.dean.hourstracker.ui.theme.Ink
import com.dean.hourstracker.ui.theme.JetBrainsMono
import com.dean.hourstracker.ui.theme.Linen
import com.dean.hourstracker.ui.theme.Mist
import com.dean.hourstracker.ui.theme.Sage
import com.dean.hourstracker.ui.theme.Slate
import com.dean.hourstracker.ui.theme.TextFaint
import com.dean.hourstracker.ui.theme.White
import com.dean.hourstracker.util.formatForLogField
import com.dean.hourstracker.util.formatHours
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
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
            CalendarEditSheet(uiState = uiState, viewModel = viewModel)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Calendar", style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text("Days with logged hours", style = MaterialTheme.typography.bodyMedium, color = Slate)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Linen),
            )
        },
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
                MonthNavHeader(
                    month = uiState.visibleMonth,
                    onPrev = viewModel::prevMonth,
                    onNext = viewModel::nextMonth,
                )
                Spacer(Modifier.height(10.dp))
            }
            item {
                MonthTotalCard(
                    totalHours = uiState.monthTotalHours,
                    loggedDays = uiState.loggedDaysCount,
                    month = uiState.visibleMonth,
                )
                Spacer(Modifier.height(14.dp))
            }
            item {
                CalendarGrid(
                    month = uiState.visibleMonth,
                    daysWithEntries = uiState.daysWithEntries,
                    selectedDay = uiState.selectedDay,
                    onDayClick = viewModel::selectDay,
                )
                Spacer(Modifier.height(18.dp))
            }
            if (uiState.weekBreakdowns.isNotEmpty()) {
                item {
                    Text(
                        text = "BY WEEK",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextFaint,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                items(uiState.weekBreakdowns) { wb ->
                    WeekBreakdownRow(wb)
                    Spacer(Modifier.height(12.dp))
                }
                item { Spacer(Modifier.height(6.dp)) }
            }
            val selectedDay = uiState.selectedDay
            if (selectedDay != null) {
                item {
                    HorizontalDivider(color = BorderLt, modifier = Modifier.padding(vertical = 4.dp))
                    DayDetailHeader(day = selectedDay, entries = uiState.selectedDayEntries)
                }
                if (uiState.selectedDayEntries.isEmpty()) {
                    item {
                        Text(
                            text = "No entries for this day.",
                            color = TextFaint,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                } else {
                    items(uiState.selectedDayEntries, key = { it.entry.id }) { ewp ->
                        EntryRow(
                            ewp = ewp,
                            onClick = { viewModel.startEdit(ewp) },
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
private fun MonthNavHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = Slate,
            )
        }
        Text(text = label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = Slate,
            )
        }
    }
}

@Composable
private fun MonthTotalCard(totalHours: Double, loggedDays: Int, month: YearMonth) {
    val monthName = month.format(DateTimeFormatter.ofPattern("MMMM"))
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
                    text = "THIS MONTH",
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
                Text(
                    "$loggedDays ${if (loggedDays == 1) "day" else "days"} in $monthName",
                    fontSize = 11.sp,
                    color = Slate,
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    daysWithEntries: Set<LocalDate>,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
) {
    val firstDay = month.atDay(1)
    // Sun=0, Mon=1, …, Sat=6  (java.time DayOfWeek: Mon=1 … Sun=7)
    val startOffset = firstDay.dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()

    val cells = buildList<LocalDate?> {
        repeat(startOffset) { add(null) }
        for (d in 1..daysInMonth) add(firstDay.withDayOfMonth(d))
        val remainder = size % 7
        if (remainder != 0) repeat(7 - remainder) { add(null) }
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextFaint,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    CalendarCell(
                        date = date,
                        hasEntries = date != null && date in daysWithEntries,
                        isSelected = date != null && date == selectedDay,
                        onClick = { date?.let(onDayClick) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate?,
    hasEntries: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (date == null) return@Box

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            color = when {
                isSelected -> DeepTeal
                hasEntries -> White
                else -> Color.Transparent
            },
            border = if (hasEntries && !isSelected) BorderStroke(1.dp, BorderDk) else null,
            tonalElevation = if (hasEntries && !isSelected) 1.dp else 0.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 12.sp,
                    fontWeight = if (hasEntries || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isSelected -> White
                        hasEntries -> Ink
                        else -> TextFaint
                    },
                )
                if (hasEntries) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) White else Sage),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekBreakdownRow(wb: WeekBreakdown) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = wb.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(text = wb.hours.formatHours(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepTeal)
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { wb.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = Sage,
            trackColor = BorderLt,
        )
    }
}

@Composable
private fun DayDetailHeader(day: LocalDate, entries: List<EntryWithProject>) {
    val fmt = DateTimeFormatter.ofPattern("EEEE, MMM d")
    val totalHours = entries.sumOf { it.entry.hours }
    val label = if (entries.isEmpty()) day.format(fmt)
    else "${day.format(fmt)} · ${totalHours.formatHours()} hrs"
    Text(
        text = label,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = DeepTeal,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarEditSheet(uiState: CalendarUiState, viewModel: CalendarViewModel) {
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

        HoursControl(
            hours = uiState.editHours,
            onDecrement = viewModel::onEditDecrement,
            onIncrement = viewModel::onEditIncrement,
            onQuickPick = viewModel::onEditQuickPick,
        )

        OutlinedTextField(
            value = uiState.editNote,
            onValueChange = viewModel::onEditNote,
            label = { Text("Notes · optional") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = viewModel::requestDelete, modifier = Modifier.weight(1f)) {
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
