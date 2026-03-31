package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habit.data.TargetMode
import com.habit.viewmodel.CompletedItem
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun CompletedList(
    items: List<CompletedItem>,
    onSelect: (Long) -> Unit,
    onDoAgain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(items, key = { it.activity.id }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item.activity.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.habit.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (item.activity.note.isNotEmpty()) {
                        Text(
                            text = item.activity.note,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
                item.activity.completedAt?.let { instant ->
                    val time = LocalTime.ofInstant(
                        instant, ZoneId.systemDefault()
                    )
                    Text(
                        text = "%d:%02d".format(time.hour, time.minute),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (item.habit.dailyTargetMode == TargetMode.AT_LEAST) {
                    TextButton(onClick = { onDoAgain(item.habit.id) }) {
                        Text("Again")
                    }
                }
            }
        }
    }
}
