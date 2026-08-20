package com.dean.hourstracker.ui.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
import com.dean.hourstracker.util.formatHours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { payload ->
            val uri = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
                File(dir, payload.filename).also { it.writeText(payload.csvContent) }
                    .let { FileProvider.getUriForFile(context, "com.dean.hourstracker.fileprovider", it) }
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Volunteer Hours Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share volunteer hours"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Reports", style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text("Hours by period", style = MaterialTheme.typography.bodyMedium, color = Slate)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Linen),
            )
        },
        containerColor = Linen,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            PeriodSegmentedControl(
                selected = uiState.selectedPeriod,
                onSelect = viewModel::selectPeriod,
            )

            // Total display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = uiState.totalHours.formatHours(),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepTeal,
                    letterSpacing = (-2.08).sp,
                    lineHeight = 52.sp,
                )
                Text(
                    text = "hours this ${uiState.selectedPeriod.label.lowercase()}",
                    fontSize = 12.sp,
                    color = Slate,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = rangeLabel(uiState.startDate, uiState.endDate),
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = TextFaint,
                    modifier = Modifier.padding(top = 6.dp),
                    letterSpacing = 0.05.sp,
                )
            }

            if (uiState.breakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No hours logged this ${uiState.selectedPeriod.label.lowercase()}.", color = TextFaint)
                }
            } else {
                Text(
                    text = "BY PROJECT",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextFaint,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                uiState.breakdown.forEach { item ->
                    BreakdownRow(item)
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                onClick = viewModel::exportCurrentPeriod,
                shape = RoundedCornerShape(12.dp),
                color = DeepTeal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "⤴  Export CSV — Share…",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    modifier = Modifier.padding(vertical = 14.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PeriodSegmentedControl(
    selected: ReportPeriod,
    onSelect: (ReportPeriod) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = White,
        border = BorderStroke(1.dp, BorderDk),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ReportPeriod.entries.forEach { period ->
                val isSelected = period == selected
                Surface(
                    onClick = { onSelect(period) },
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) DeepTeal else White,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = period.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) White else Slate,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(item: ProjectBreakdown) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.projectName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
            Text(
                text = item.hours.formatHours(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeepTeal,
            )
        }
        Spacer(Modifier.height(5.dp))
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = BorderLt,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                LinearProgressIndicator(
                    progress = { item.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Sage,
                    trackColor = BorderLt,
                )
            }
        }
    }
}
