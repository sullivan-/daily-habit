package com.habit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habit.viewmodel.TrackHistoryItem
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun TrackHistoryView(
    habitName: String,
    items: List<TrackHistoryItem>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = ZoneId.systemDefault()

    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$habitName — History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) { Text("Close") }
        }

        if (items.isEmpty()) {
            Text(
                text = "no completed activities",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.activityId }) { index, item ->
                    if (index > 0) {
                        val prevSunday = items[index - 1].completedAt
                            .atZone(zone).toLocalDate()
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                        val curSunday = item.completedAt.atZone(zone).toLocalDate()
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                        if (prevSunday != curSunday) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(
                                thickness = 2.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    TrackHistoryRow(item, dateFormatter, timeFormatter, zone)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                            .copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackHistoryRow(
    item: TrackHistoryItem,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    zone: ZoneId
) {
    val zoned = item.completedAt.atZone(zone)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "${zoned.format(dateFormatter)}, ${zoned.format(timeFormatter)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (item.trackName != null) {
            Row {
                Text(
                    text = item.trackName,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.milestoneNames.isNotEmpty()) {
                    Text(
                        text = " — ${item.milestoneNames.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "no track",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (item.note.isNotBlank()) {
            Text(
                text = item.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
