package com.dean.hourstracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dean.hourstracker.ui.theme.BorderDk
import com.dean.hourstracker.ui.theme.DeepTeal
import com.dean.hourstracker.ui.theme.Mist
import com.dean.hourstracker.ui.theme.Slate
import com.dean.hourstracker.ui.theme.White
import com.dean.hourstracker.util.formatHours

private val quickPicks = listOf(0.5, 1.0, 2.0, 4.0)

@Composable
fun HoursControl(
    hours: Double,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onQuickPick: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Mist,
        border = BorderStroke(1.dp, BorderDk),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = hours.formatHours(),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepTeal,
                letterSpacing = (-1.32).sp,
                lineHeight = 44.sp,
            )
            Text(
                text = "hours · tap chips to add",
                fontSize = 13.sp,
                color = Slate,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StepButton(label = "−", contentDescription = "Decrease hours", onClick = onDecrement)
                StepButton(label = "+", contentDescription = "Increase hours", onClick = onIncrement)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickPicks.forEach { pick ->
                    QuickChip(
                        label = "+${pick.formatHours()}",
                        onClick = { onQuickPick(pick) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun StepButton(label: String, contentDescription: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = White,
        border = BorderStroke(1.dp, BorderDk),
        modifier = Modifier
            .size(36.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepTeal,
            )
        }
    }
}

@Composable
fun QuickChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = White,
        border = BorderStroke(1.dp, BorderDk),
        modifier = modifier,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepTeal,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
