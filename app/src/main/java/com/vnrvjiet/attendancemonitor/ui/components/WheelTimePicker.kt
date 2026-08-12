package com.vnrvjiet.attendancemonitor.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.*

@Composable
fun WheelTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialValues = remember(initialTime) {
        try {
            val parts = initialTime.split(" ", ":")
            Triple(parts[0].toInt(), parts[1].toInt(), parts[2])
        } catch (e: Exception) {
            Triple(9, 0, "AM")
        }
    }

    var selectedHour by remember { mutableIntStateOf(initialValues.first) }
    var selectedMinute by remember { mutableIntStateOf(initialValues.second) }
    var selectedAmPm by remember { mutableStateOf(initialValues.third) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour Wheel
                    WheelPicker(
                        items = (1..12).toList(),
                        initialItem = selectedHour,
                        onItemSelected = { selectedHour = it },
                        modifier = Modifier.weight(1f)
                    )

                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))

                    // Minute Wheel
                    WheelPicker(
                        items = (0..59).toList(),
                        initialItem = selectedMinute,
                        format = { String.format(Locale.getDefault(), "%02d", it) },
                        onItemSelected = { selectedMinute = it },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // AM/PM Wheel
                    WheelPicker(
                        items = listOf("AM", "PM"),
                        initialItem = selectedAmPm,
                        onItemSelected = { selectedAmPm = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(onClick = {
                        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", selectedHour, selectedMinute, selectedAmPm)
                        onConfirm(formattedTime)
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    format: (T) -> String = { it.toString() }
) {
    val initialIndex = items.indexOf(initialItem).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = state)
    
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val centerIndex = state.firstVisibleItemIndex
            if (centerIndex in items.indices) {
                onItemSelected(items[centerIndex])
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Highlight background for the middle item
        Surface(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {}

        LazyColumn(
            state = state,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = 60.dp), // Height / 2 - ItemHeight / 2 = 160/2 - 40/2 = 60
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val item = items[index]
                
                // Using a derived state to determine selection/scaling to avoid excessive recomposition
                val isSelected = remember { derivedStateOf { state.firstVisibleItemIndex == index } }
                
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Subdue items that are not in the center
                            alpha = if (isSelected.value) 1f else 0.4f
                            scaleX = if (isSelected.value) 1.2f else 0.9f
                            scaleY = if (isSelected.value) 1.2f else 0.9f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(item),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isSelected.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected.value) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
