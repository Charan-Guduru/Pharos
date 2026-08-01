package com.vnrvjiet.attendancemonitor.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isEmpty) {
        EmptyHistoryState()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Display Today first if present
            uiState.groupedItems["Today"]?.let { items ->
                item { SectionHeader(Icons.Outlined.History, "Today") }
                items(items) { HistoryItemCard(it) }
            }

            // Display Yesterday if present
            uiState.groupedItems["Yesterday"]?.let { items ->
                item { SectionHeader(Icons.Outlined.History, "Yesterday") }
                items(items) { HistoryItemCard(it) }
            }

            // Display Older if present
            uiState.groupedItems["Older"]?.let { items ->
                item { SectionHeader(Icons.Outlined.History, "Older") }
                items(items) { HistoryItemCard(it) }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No attendance records found", color = Color.Gray)
            Text("Go to Dashboard to record attendance", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
    }
}

@Composable
fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HistoryItemCard(item: HistoryItem) {
    AttendanceHistoryCard(
        subject = item.subject.subjectName,
        faculty = item.subject.facultyName,
        time = "${item.entry.startTime} - ${item.entry.endTime}",
        date = item.formattedDate,
        status = item.record.status,
        syncStatus = item.record.syncStatus.name,
        remarks = item.record.remarks,
        accentColor = Color(item.subject.color)
    )
}

@Composable
fun AttendanceHistoryCard(
    subject: String,
    faculty: String,
    time: String,
    date: String,
    status: AttendanceStatus,
    syncStatus: String,
    remarks: String?,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(subject, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(faculty, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = getStatusColor(status)
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = statusColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(status.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(syncStatus, style = MaterialTheme.typography.labelSmall, color = if (syncStatus == "SYNCED") Color(0xFF4CAF50) else Color.Gray)
            }

            if (!remarks.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = Color(0xFFF5F5F5), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Text(text = remarks, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

fun getStatusColor(status: AttendanceStatus): Color {
    return when (status) {
        AttendanceStatus.PRESENT -> Color(0xFF4CAF50)
        AttendanceStatus.ABSENT -> Color(0xFFF44336)
        AttendanceStatus.BUNK -> Color(0xFFFF9800)
        else -> Color.Gray
    }
}
