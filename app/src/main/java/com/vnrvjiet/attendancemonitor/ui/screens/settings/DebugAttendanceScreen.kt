package com.vnrvjiet.attendancemonitor.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugAttendanceScreen(
    onBack: () -> Unit,
    viewModel: DebugAttendanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.fetchAttendance()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug: EduPrime Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is DebugAttendanceUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching from EduPrime...")
                    }
                }
                is DebugAttendanceUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.records) { record ->
                            AttendanceItem(record)
                        }
                    }
                }
                is DebugAttendanceUiState.Empty -> {
                    EmptyState("No records found in the attendance table.")
                }
                is DebugAttendanceUiState.Error -> {
                    ErrorState(state.message)
                }
                is DebugAttendanceUiState.Idle -> {
                    // Waiting for LaunchedEffect
                }
            }
        }
    }
}

@Composable
fun AttendanceItem(record: EduPrimeAttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = record.subjectName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = record.subjectCode,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Conducted", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("${record.conductedClasses}", fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Attended", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("${record.attendedClasses}", fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Percentage", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = "${record.attendancePercentage}%",
                        color = if (record.attendancePercentage < 75) Color.Red else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = Color.Gray)
    }
}

@Composable
fun ErrorState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Error: $message", color = MaterialTheme.colorScheme.error)
    }
}
