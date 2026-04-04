package com.habit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habit.viewmodel.ChoicesViewModel
import com.habit.viewmodel.TallyDisplayItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoicesScreen(
    viewModel: ChoicesViewModel,
    onEditTally: (Long) -> Unit,
    onNewTally: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choices") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTally) {
                Icon(Icons.Filled.Add, "new tally")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(uiState.tallies, key = { it.tally.id }) { item ->
                TallyRow(
                    item = item,
                    onNo = { viewModel.recordChoice(item.tally.id, abstained = true) },
                    onYes = { viewModel.recordChoice(item.tally.id, abstained = false) },
                    onEdit = { onEditTally(item.tally.id) }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun TallyRow(
    item: TallyDisplayItem,
    onNo: () -> Unit,
    onYes: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = item.tally.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (item.totalCount > 0) {
            Text(
                text = "${item.abstainCount}/${item.totalCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = indicatorColor(item.ratio)
            )
        }
        TextButton(onClick = onNo) {
            Text("No")
        }
        TextButton(onClick = onYes) {
            Text("Yes")
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, "edit")
        }
    }
}

fun indicatorColor(ratio: Float): Color {
    return lerp(Color.Red, Color.Green, ratio)
}
