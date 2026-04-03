package com.habit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun Modifier.swipeHistoryGesture(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    isAtLeft: Boolean,
    isAtRight: Boolean
): Modifier {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val threshold = 100f

    return this
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(offsetX.value.roundToInt(), 0)
            }
        }
        .pointerInput(isAtLeft, isAtRight) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    when {
                        offsetX.value < -threshold && !isAtLeft -> onSwipeLeft()
                        offsetX.value > threshold && !isAtRight -> onSwipeRight()
                    }
                    scope.launch { offsetX.animateTo(0f, spring()) }
                },
                onDragCancel = {
                    scope.launch { offsetX.animateTo(0f, spring()) }
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        val dampened = if (
                            (dragAmount < 0 && isAtLeft) ||
                            (dragAmount > 0 && isAtRight)
                        ) {
                            dragAmount * 0.3f
                        } else {
                            dragAmount
                        }
                        offsetX.snapTo(offsetX.value + dampened)
                    }
                }
            )
        }
}
