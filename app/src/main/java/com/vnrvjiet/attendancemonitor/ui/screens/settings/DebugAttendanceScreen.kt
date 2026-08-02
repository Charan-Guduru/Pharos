package com.vnrvjiet.attendancemonitor.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
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
import com.vnrvjiet.attendancemonitor.data.model.ComparisonResult
import com.vnrvjiet.attendancemonitor.data.model.ComparisonStatus
import com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord
import com.vnrvjiet.attendancemonitor.data.model.MatchStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugAttendanceScreen(
    onBack: () -> Unit,
    fetchViewModel: DebugAttendanceViewModel = viewModel(),
    comparisonViewModel: ComparisonViewModel = viewModel()
) {
    val fetchUiState by fetchViewModel.uiState.collectAsStateWithLifecycle()
    val comparisonUiState by comparisonViewModel.uiState.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Fetch", "Comparison")

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && fetchUiState is DebugAttendanceUiState.Idle) {
            fetchViewModel.fetchAttendance()
        } else if (selectedTab == 1 && comparisonUiState.results.isEmpty() && !comparisonUiState.isLoading) {
            comparisonViewModel.performComparison()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Debug: EduPrime Attendance") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab == 0) {
                FetchTabContent(fetchUiState)
            } else {
                ComparisonTabContent(comparisonUiState)
            }
        }
    }
}

@Composable
fun FetchTabContent(state: DebugAttendanceUiState) {
    when (state) {
        is DebugAttendanceUiState.Loading -> {
            LoadingIndicator("Fetching from EduPrime...")
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
        else -> {}
    }
}

@Composable
fun ComparisonTabContent(state: ComparisonUiState) {
    when {
        state.isLoading -> {
            LoadingIndicator("Comparing data...")
        }
        state.error != null -> {
            ErrorState(state.error)
        }
        state.results.isEmpty() -> {
            EmptyState("No comparison data available.")
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.results) { result ->
                    ComparisonItem(result)
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
fun ComparisonItem(result: ComparisonResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Subject: ${result.subjectCode}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Local (Old)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("${result.prevAttended} / ${result.prevConducted}", fontWeight = FontWeight.Medium)
                }
                
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("EduPrime (New)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("${result.currAttended} / ${result.currConducted}", fontWeight = FontWeight.Medium)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val statusColor = when (result.matchStatus) {
                MatchStatus.MATCH -> Color(0xFF2E7D32)
                MatchStatus.MISMATCH -> Color.Red
                MatchStatus.UNKNOWN -> Color.Gray
            }
            
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${result.matchStatus} | ${result.comparisonStatus}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
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
                    val formattedPercentage = if (record.attendancePercentage % 1 == 0.0) {
                        record.attendancePercentage.toInt().toString()
                    } else {
                        "%.2f".format(record.attendancePercentage)
                    }
                    Text("Percentage", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = "$formattedPercentage%",
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
