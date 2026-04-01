package com.habit.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.scrollbarIndicator(listState: LazyListState): Modifier =
    drawWithContent {
        drawContent()

        val totalItems = listState.layoutInfo.totalItemsCount
        val visibleItems = listState.layoutInfo.visibleItemsInfo.size

        if (totalItems > visibleItems && totalItems > 0) {
            val barHeight = size.height * visibleItems.toFloat() / totalItems
            val firstVisible = listState.firstVisibleItemIndex
            val scrollFraction = firstVisible.toFloat() / (totalItems - visibleItems)
            val barTop = scrollFraction * (size.height - barHeight)
            val barWidth = 4.dp.toPx()

            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(size.width - barWidth, barTop),
                size = Size(barWidth, barHeight)
            )
        }
    }
