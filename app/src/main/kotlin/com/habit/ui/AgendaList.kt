package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habit.viewmodel.AgendaItem

@Composable
fun AgendaList(
    items: List<AgendaItem>,
    onSelect: (String) -> Unit,
    hasOtherHabits: Boolean = false,
    onOther: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier.scrollbarIndicator(listState)
    ) {
        items(items, key = { "${it.habit.id}-${it.activityNumber}" }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item.habit.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = item.habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (item.totalTarget > 1) {
                    Text(
                        text = "${item.activityNumber}/${item.totalTarget}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
        if (hasOtherHabits) {
            item(key = "other") {
                Text(
                    text = "Other\u2026",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOther() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
