package com.habit.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.swipeBar(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
): Modifier = this.pointerInput(onSwipeLeft, onSwipeRight) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onDragEnd = {
            if (totalDrag < -80f) onSwipeLeft?.invoke()
            else if (totalDrag > 80f) onSwipeRight?.invoke()
        },
        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
    )
}
