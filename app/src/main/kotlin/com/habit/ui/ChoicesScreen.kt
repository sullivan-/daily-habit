package com.habit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.habit.viewmodel.formatStreakDuration
import java.time.Instant

@Composable
fun ChoicesScreen(
    viewModel: ChoicesViewModel,
    onDetails: (String) -> Unit,
    onNewTally: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            ChoicesBar(
                weeklyAbstainCount = uiState.weeklyAbstainCount,
                weeklyTotalCount = uiState.weeklyTotalCount,
                onBack = onBack,
                onNewTally = onNewTally,
                onSwipeLeft = onBack
            )
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
                    onDetails = { onDetails(item.tally.id) }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun ChoicesBar(
    weeklyAbstainCount: Int,
    weeklyTotalCount: Int,
    onBack: () -> Unit,
    onNewTally: () -> Unit,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.DarkGray)
            .swipeBar(onSwipeLeft = onSwipeLeft, onSwipeRight = onSwipeRight)
    ) {
        MenuButton(
            onNewHabit = {},
            onHabitList = onBack,
            onChoices = {},
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Choices",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            if (weeklyTotalCount > 0) {
                Text(
                    text = "$weeklyAbstainCount/$weeklyTotalCount",
                    color = indicatorColor(weeklyAbstainCount.toFloat() / weeklyTotalCount),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        IconButton(
            onClick = onNewTally,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Filled.Add, "new tally", tint = Color.White)
        }
    }
}

@Composable
fun TallyRow(
    item: TallyDisplayItem,
    onNo: () -> Unit,
    onYes: () -> Unit,
    onDetails: () -> Unit
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
            modifier = Modifier
                .weight(1f)
                .clickable { onDetails() }
        )
        val streakText = item.streakStart?.let {
            formatStreakDuration(it, Instant.now())
        }
        if (streakText != null) {
            Text(
                text = streakText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Green
            )
        }
        OutlinedButton(onClick = onNo, elevation = buttonElevation()) {
            Text("No")
        }
        OutlinedButton(onClick = onYes, elevation = buttonElevation()) {
            Text("Yes")
        }
    }
}

fun indicatorColor(ratio: Float): Color {
    return lerp(Color.Red, Color.Green, ratio)
}
