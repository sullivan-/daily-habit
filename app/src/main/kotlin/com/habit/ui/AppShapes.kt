package com.habit.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val ControlShape = RoundedCornerShape(8.dp)

@Composable
fun buttonElevation(): ButtonElevation = ButtonDefaults.buttonElevation(
    defaultElevation = 2.dp,
    pressedElevation = 4.dp
)

fun segmentedShape(index: Int, count: Int): RoundedCornerShape = when {
    count == 1 -> ControlShape
    index == 0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
    index == count - 1 -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
    else -> RoundedCornerShape(0.dp)
}
