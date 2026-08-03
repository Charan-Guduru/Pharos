package com.vnrvjiet.attendancemonitor.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isEmpty) {
        EmptyStatisticsState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Overall Summary Card
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

            // Attendance Analysis (Circular Indicator)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Attendance Analysis", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularAttendanceIndicator(uiState.overallPercentage)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Attendance Goal Card (Redesigned Safe Leave Box)
            item {
                AttendanceGoalCard(uiState)
            }

            // Subject Breakdown Header
            item {
                Text(
                    "Subject Breakdown", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Subject Stat Cards
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), 
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
