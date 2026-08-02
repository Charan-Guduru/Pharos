package com.vnrvjiet.attendancemonitor.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.vnrvjiet.attendancemonitor.ui.components.AttendanceCard
import com.vnrvjiet.attendancemonitor.ui.components.MoreSituationsSheet
import com.vnrvjiet.attendancemonitor.ui.components.SummaryCard

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToSetup: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.isLoading, uiState.isTimetableConfigured) {
        if (!uiState.isLoading && !uiState.isTimetableConfigured) {
            onNavigateToSetup()
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var selectedEntryId by remember { mutableStateOf<Long?>(null) }
    
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val currentDate = remember { dateFormatter.format(Date()) }

    if (showMoreSheet && selectedEntryId != null) {
        MoreSituationsSheet(
            onStatusSelected = { status ->
                viewModel.recordAttendance(selectedEntryId!!, status)
            },
            onDismiss = {
                showMoreSheet = false
                selectedEntryId = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Daily") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Weekly") }
                )
            }
        }

        item {
            if (uiState.isSynced) {
                SummaryCard(percentage = uiState.overallPercentage)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Not Synced", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val days = listOf("M", "T", "W", "T", "F")
                    days.forEachIndexed { index, day ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (index == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = day,
                                    color = if (index == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.timetable.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Notifications, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.outline, 
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No classes scheduled today.", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        items(uiState.timetable) { item ->
            val attendancePercentage = item.remoteAttendance?.attendancePercentage
            val statusText = if (attendancePercentage != null) {
                if (attendancePercentage < 75) "Low (${attendancePercentage.toInt()}%)" 
                else "${attendancePercentage.toInt()}%"
            } else {
                null
            }

            AttendanceCard(
                subject = item.subject.subjectName,
                faculty = item.subject.facultyName,
                time = item.entry.startTime,
                room = item.subject.roomNumber,
                isAttendancePeriod = item.subject.isAttendanceSubject,
                isNow = item.entry.id == 2L, // Dummy logic for current class
                statusText = statusText,
                accentColor = Color(item.subject.color),
                currentStatus = item.attendanceRecord?.status,
                onStatusSelected = { status ->
                    viewModel.recordAttendance(item.entry.id, status)
                },
                onMoreClick = {
                    selectedEntryId = item.entry.id
                    showMoreSheet = true
                }
            )
        }

        /* item {
            ActivityCard()
        } */
    }
}
