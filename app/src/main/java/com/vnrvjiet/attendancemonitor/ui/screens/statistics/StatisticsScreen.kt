package com.vnrvjiet.attendancemonitor.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
            contentPadding = PaddingValues(vertical = 16.dp)
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
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryItem("Total Conducted", uiState.totalConducted.toString())
                            SummaryItem("Total Attended", uiState.totalAttended.toString())
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val changeText = if (uiState.isLead) "+${uiState.classesChange} (Lead)" else "${uiState.classesChange} (Needed)"
                            SummaryItem("Status", changeText)
                            SummaryItem("Last Sync", uiState.lastSyncFormatted)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Attendance Analysis", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        CircularAttendanceIndicator(uiState.overallPercentage)
                    }
                }
            }

            item {
                SafeLeaveBox(uiState)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Subject Comparison", 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f), 
                            horizontalArrangement = Arrangement.SpaceEvenly, 
                            verticalAlignment = Alignment.Bottom
                        ) {
                            uiState.subjectStats.forEach { stat ->
                                val riskColor = when {
                                    stat.percentage >= 80 -> Color(0xFF4CAF50)
                                    stat.percentage >= 75 -> Color(0xFFFFC107)
                                    stat.percentage >= 65 -> Color(0xFFFF9800)
                                    else -> Color(0xFFF44336)
                                }
                                Bar(fraction = stat.percentage / 100f, color = riskColor, label = stat.subjectName.take(3).uppercase())
                            }
                        }
                    }
                }
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
                val riskColor = when {
                    stat.percentage >= 80 -> Color(0xFF4CAF50)
                    stat.percentage >= 75 -> Color(0xFFFFC107)
                    stat.percentage >= 65 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
                
                SubjectStatCard(
                    subject = stat.subjectName,
                    percentage = stat.percentage,
                    attended = stat.attended,
                    conducted = stat.conducted,
                    classesChange = stat.classesChange,
                    isLead = stat.isLead,
                    color = riskColor,
                    status = stat.status
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SafeLeaveBox(state: StatisticsUiState) {
    val isSafe = state.overallPercentage >= 75f
    val backgroundColor = if (isSafe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
    val contentColor = if (isSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = contentColor.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isSafe) Icons.Default.CheckCircle else Icons.Default.Info, null, tint = contentColor)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Safe Leave Calculator", 
                        fontWeight = FontWeight.Bold, 
                        color = contentColor,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (isSafe) {
                        Text("You can bunk ${state.safeLeave75} more classes to stay above 75%.", fontSize = 12.sp, color = contentColor)
                    } else {
                        Text("You need ${state.classesNeeded75} more classes to reach 75%.", fontSize = 12.sp, color = contentColor)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = contentColor.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("To 80%", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
                    val text = if (state.overallPercentage >= 80f) "Bunk ${state.safeLeave80}" else "Need ${state.classesNeeded80}"
                    Text(text, fontWeight = FontWeight.Bold, color = contentColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("To 75%", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
                    val text = if (state.overallPercentage >= 75f) "Bunk ${state.safeLeave75}" else "Need ${state.classesNeeded75}"
                    Text(text, fontWeight = FontWeight.Bold, color = contentColor)
                }
            }
        }
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
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        Canvas(modifier = Modifier.size(140.dp)) {
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = (percentage / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percentage.toInt()}%", 
                fontSize = 32.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total", 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SubjectStatCard(
    subject: String, 
    percentage: Int, 
    attended: Int,
    conducted: Int,
    classesChange: Int,
    isLead: Boolean,
    color: Color, 
    status: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subject, 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = color.copy(alpha = 0.1f), 
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.widthIn(min = 60.dp)
                ) {
                    Text(
                        text = status, 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), 
                        color = color, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$percentage%", 
                    fontSize = 36.sp, 
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$attended / $conducted Classes", 
                        fontSize = 14.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isLead) "+$classesChange Lead" else "$classesChange Needed",
                        fontSize = 12.sp,
                        color = if (isLead) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun Bar(fraction: Float, color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(fraction.coerceIn(0.05f, 1f))
                .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
