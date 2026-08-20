package com.dean.hourstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dean.hourstracker.data.EntryWithProject
import com.dean.hourstracker.ui.theme.DeepTeal
import com.dean.hourstracker.ui.theme.Ink
import com.dean.hourstracker.ui.theme.Sage
import com.dean.hourstracker.ui.theme.TextFaint
import com.dean.hourstracker.ui.theme.White
import com.dean.hourstracker.util.formatHours
import com.dean.hourstracker.util.formatShort
import java.time.LocalDate

@Composable
fun EntryRow(
    ewp: EntryWithProject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = White,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Sage),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = ewp.project.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                val date = LocalDate.ofEpochDay(ewp.entry.dateEpochDay)
                val subtitle = buildString {
                    append(date.formatShort())
                    if (ewp.entry.note.isNotBlank()) append(" · ${ewp.entry.note}")
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextFaint,
                    modifier = Modifier.padding(start = 14.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = ewp.entry.hours.formatHours(),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepTeal,
                letterSpacing = (-0.02).sp,
            )
        }
    }
}
