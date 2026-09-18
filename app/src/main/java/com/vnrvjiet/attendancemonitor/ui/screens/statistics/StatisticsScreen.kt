package com.vnrvjiet.attendancemonitor.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showForecast by viewModel.showForecast.collectAsStateWithLifecycle()
    val forecastState by viewModel.forecastState.collectAsStateWithLifecycle()
    val selectedDates by viewModel.selectedDates.collectAsStateWithLifecycle()

    var showMultiDatePicker by remember { mutableStateOf(false) }

    if (showMultiDatePicker) {
        MultiDatePickerDialog(
            selectedDates = selectedDates,
            onDateToggled = { viewModel.toggleForecastDate(it) },
            onClear = { viewModel.clearForecastDates() },
            onConfirm = {
                viewModel.setShowForecast(true)
                showMultiDatePicker = false
            },
            onDismiss = { showMultiDatePicker = false }
        )
    }

    if (showForecast && forecastState != null) {
        LeaveForecastSheet(
            state = forecastState!!,
            onDismiss = { viewModel.setShowForecast(false) }
        )
    }

    if (uiState.isEmpty) {
        EmptyStatisticsState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Overall Summary", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            SummaryItem(
                                label = "Total Conducted", 
                                value = uiState.totalConducted.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryItem(
                                label = "Total Attended", 
                                value = uiState.totalAttended.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val goalText = if (uiState.isLead) "+${uiState.classesChange} Lead" else "Need ${uiState.classesChange} Classes"
                            SummaryItem(
                                label = "Attendance Goal", 
                                value = goalText,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryItem(
                                label = "Last Sync", 
                                value = uiState.lastSyncFormatted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Attendance Analysis", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showMultiDatePicker = true }) {
                            Icon(
                                Icons.Default.CalendarMonth, 
                                contentDescription = "Leave Forecast",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularAttendanceIndicator(uiState.overallPercentage)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            item {
                AttendanceGoalCard(uiState)
            }

            item {
                Text(
                    "Subject Breakdown", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(uiState.subjectStats) { stat ->
                SubjectStatCard(stat)
            }
        }
    }
}

@Composable
fun AttendanceGoalCard(state: StatisticsUiState) {
    val (backgroundColor, contentColor) = when {
        state.overallPercentage < 75f -> Color(0xFFFFF3E0) to Color(0xFFE65100) // Orange
        state.overallPercentage < 80f -> Color(0xFFFFFDE7) to Color(0xFFFBC02D) // Yellow
        else -> Color(0xFFE8F5E9) to Color(0xFF2E7D32) // Green
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape, 
                    color = contentColor.copy(alpha = 0.1f), 
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (state.overallPercentage >= 75f) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.TrendingUp, 
                            null, 
                            tint = contentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Attendance Goal", 
                        fontWeight = FontWeight.Bold, 
                        color = contentColor,
                        style = MaterialTheme.typography.titleSmall
                    )
                    val naturalText = if (state.overallPercentage >= 75f) {
                        "You may bunk ${state.safeLeave75} classes and remain above 75%."
                    } else {
                        "Need ${state.classesNeeded75} more classes to reach 75%."
                    }
                    Text(naturalText, fontSize = 14.sp, color = contentColor.copy(alpha = 0.9f))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = contentColor.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                GoalDetailItem(
                    label = "TO 75%", 
                    isSafe = state.overallPercentage >= 75f, 
                    safeCount = state.safeLeave75, 
                    neededCount = state.classesNeeded75, 
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
                GoalDetailItem(
                    label = "TO 80%", 
                    isSafe = state.overallPercentage >= 80f, 
                    safeCount = state.safeLeave80, 
                    neededCount = state.classesNeeded80, 
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun GoalDetailItem(
    label: String, 
    isSafe: Boolean, 
    safeCount: Int, 
    neededCount: Int, 
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        val valueText = if (isSafe) "Bunk $safeCount" else "Need $neededCount"
        Text(valueText, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
    }
}

@Composable
fun EmptyStatisticsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Info, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp), 
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No statistics available", 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Record your attendance to see analysis", 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun CircularAttendanceIndicator(percentage: Float) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = getRiskColor(percentage)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        Canvas(modifier = Modifier.size(120.dp)) {
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = (percentage / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percentage.toInt()}%", 
                fontSize = 32.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "OVERALL", 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SubjectStatCard(stat: SubjectStatUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stat.subjectName, 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stat.subjectCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                RiskBadge(stat.percentage.toFloat(), stat.status)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${stat.percentage}%", 
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stat.attended} / ${stat.conducted} Classes", 
                    fontSize = 13.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            LinearProgressIndicator(
                progress = { stat.percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = getRiskColor(stat.percentage.toFloat()),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiDatePickerDialog(
    selectedDates: Set<Long>,
    onDateToggled: (Long) -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Plan Your Leave", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap dates to toggle leave status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Month Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sdf = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
                    Text(
                        text = sdf.format(currentMonth.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        IconButton(
                            onClick = {
                                val prev = currentMonth.clone() as Calendar
                                prev.add(Calendar.MONTH, -1)
                                currentMonth = prev
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                val next = currentMonth.clone() as Calendar
                                next.add(Calendar.MONTH, 1)
                                currentMonth = next
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                // Calendar Grid
                CalendarGrid(
                    month = currentMonth,
                    selectedDates = selectedDates,
                    onDateToggled = onDateToggled
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val daysText = if (selectedDates.size == 1) "1 day selected" else "${selectedDates.size} days selected"
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedDates.isNotEmpty(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Calculate")
            }
        },
        dismissButton = {
            TextButton(onClick = onClear) {
                Text("Clear All")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
fun CalendarGrid(
    month: Calendar,
    selectedDates: Set<Long>,
    onDateToggled: (Long) -> Unit
) {
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    // Sunday-first: Sunday=0, Monday=1...
    val startOffset = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
    
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayMillis = todayCal.timeInMillis

    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
    
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        val totalCells = (startOffset + daysInMonth + 6) / 7 * 7
        for (i in 0 until totalCells step 7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0 until 7) {
                    val dayIndex = i + j
                    val dayNum = dayIndex - startOffset + 1
                    
                    if (dayNum in 1..daysInMonth) {
                        val dateCal = (month.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, dayNum)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val dateMillis = dateCal.timeInMillis
                        val isSelected = selectedDates.contains(dateMillis)
                        val isPast = dateMillis < todayMillis
                        val isToday = dateMillis == todayMillis
                        val isSunday = dateCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                                    } else Modifier
                                )
                                .clickable(enabled = !isPast) { onDateToggled(dateMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    isSunday && !isSelected -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveForecastSheet(
    state: LeaveForecastUiState,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    val periodStr = if (state.selectedDates.size == 1) {
        sdf.format(Date(state.selectedDates.first()))
    } else {
        "${state.selectedDates.size} planned days"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Leave Forecast",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Prediction assuming absence on explicitly selected dates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            ForecastDetailRow("Planned Absence", periodStr)
            ForecastDetailRow("Current Attendance", "${String.format(Locale.getDefault(), "%.2f", state.currentPercentage)}%")
            ForecastDetailRow("Classes Missed", state.totalClassesMissed.toString())
            
            if (state.dailyBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Absence Breakdown",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .heightIn(max = 160.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val breakdownSdf = remember { SimpleDateFormat("dd MMM (EEE)", Locale.getDefault()) }
                    LazyColumn(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.dailyBreakdown) { day ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = breakdownSdf.format(Date(day.date)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (day.isSunday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (day.isSunday) "Sunday (0)" else "${day.classCount} classes",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Predicted Attendance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val riskColor = getRiskColor(state.predictedPercentage)
                    Text(
                        text = state.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = "${String.format(Locale.getDefault(), "%.2f", state.predictedPercentage)}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val changeSign = if (state.change > 0) "+" else ""
            val changeColor = if (state.change >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Text(
                text = "Change: $changeSign${String.format(Locale.getDefault(), "%.2f", state.change)} percentage points",
                style = MaterialTheme.typography.bodyMedium,
                color = changeColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun ForecastDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RiskBadge(percentage: Float, label: String) {
    val color = getRiskColor(percentage)
    Surface(
        color = color.copy(alpha = 0.1f), 
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.widthIn(min = 60.dp)
    ) {
        Text(
            text = label, 
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), 
            color = color, 
            fontSize = 10.sp, 
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

fun getRiskColor(percentage: Float): Color {
    return when {
        percentage >= 80f -> Color(0xFF4CAF50) // Green
        percentage >= 75f -> Color(0xFFFFC107) // Yellow
        percentage >= 65f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}
