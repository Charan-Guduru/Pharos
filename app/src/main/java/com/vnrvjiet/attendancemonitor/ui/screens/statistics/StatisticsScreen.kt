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
            modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Overall Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        CircularAttendanceIndicator(uiState.overallPercentage)
                    }
                }
            }

            item {
                SafeLeaveBox(uiState)
            }

            item {
                Text("Subject Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(uiState.subjectStats) { stat ->
                SubjectStatCard(
                    subject = stat.subjectName,
                    percentage = stat.percentage,
                    subtitle = stat.classesDisplay,
                    color = Color(stat.color),
                    status = stat.status
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Weekly Trend", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            Bar(0.4f)
                            Bar(0.6f)
                            Bar(0.7f)
                            Bar(0.9f)
                            Bar(0.8f)
                        }
                    }
                }
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
    val backgroundColor = if (isSafe) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (isSafe) Color(0xFF2E7D32) else Color(0xFFC62828)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
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
                    Text("Safe Leave Calculator", fontWeight = FontWeight.Bold, color = contentColor)
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
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No statistics available", color = Color.Gray)
            Text("Record your attendance to see analysis", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
    }
}

@Composable
fun CircularAttendanceIndicator(percentage: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        Canvas(modifier = Modifier.size(140.dp)) {
            drawArc(
                color = Color(0xFFF5F5F5),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF0D47A1),
                startAngle = -90f,
                sweepAngle = (percentage / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${percentage.toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
            Text(text = "Total", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SubjectStatCard(subject: String, percentage: Int, subtitle: String, color: Color, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(subject, fontWeight = FontWeight.Bold)
                Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$percentage%", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = color,
                trackColor = Color(0xFFF5F5F5),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun Bar(fraction: Float) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight(fraction)
            .background(Color(0xFF0D47A1).copy(alpha = fraction), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
    )
}
