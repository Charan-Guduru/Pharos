package com.vnrvjiet.attendancemonitor.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.vnrvjiet.attendancemonitor.util.TimeUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import java.text.SimpleDateFormat
import java.util.*

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
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
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

        if (selectedTab == 0) {
            dailyTabContent(uiState, currentDate, onMoreClick = { id ->
                selectedEntryId = id
                showMoreSheet = true
            }, viewModel)
        } else {
            weeklyTabContent(uiState)
        }
    }
}

fun LazyListScope.dailyTabContent(
    uiState: DashboardUiState,
    currentDate: String,
    onMoreClick: (Long) -> Unit,
    viewModel: DashboardViewModel
) {
    val actualToday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 7 else it - 1 }
    val isTodaySelected = uiState.selectedDay == actualToday
    val currentMinutes = TimeUtils.getCurrentTimeInMinutes()

    val nextEntryId = if (isTodaySelected) {
        val attendanceRequiredClasses = uiState.timetable
            .filter { it.subject.isAttendanceSubject }
            .sortedBy { TimeUtils.parseTimeToMinutes(it.entry.startTime) }
        
        val runningClass = attendanceRequiredClasses.find { 
            currentMinutes in TimeUtils.parseTimeToMinutes(it.entry.startTime)..TimeUtils.parseTimeToMinutes(it.entry.endTime)
        }

        if (runningClass != null) {
            attendanceRequiredClasses.find { 
                TimeUtils.parseTimeToMinutes(it.entry.startTime) > TimeUtils.parseTimeToMinutes(runningClass.entry.startTime) 
            }?.entry?.id
        } else {
            attendanceRequiredClasses.find { 
                TimeUtils.parseTimeToMinutes(it.entry.startTime) > currentMinutes 
            }?.entry?.id
        }
    } else null

    item {
        if (uiState.isSynced) {
            SummaryCard(
                percentage = uiState.overallPercentage,
                classesChange = uiState.classesChange,
                isLead = uiState.isLead
            )
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
                val days = listOf("M", "T", "W", "T", "F", "S")
                days.forEachIndexed { index, day ->
                    val dayNum = index + 1
                    val isSelected = uiState.selectedDay == dayNum
                    
                    Surface(
                        onClick = { viewModel.setDay(dayNum) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = day,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
        var isNow = false
        if (isTodaySelected) {
            val startMinutes = TimeUtils.parseTimeToMinutes(item.entry.startTime)
            val endMinutes = TimeUtils.parseTimeToMinutes(item.entry.endTime)
            isNow = currentMinutes in startMinutes..endMinutes
        }

        AttendanceCard(
            subject = item.subject.subjectName,
            time = item.entry.startTime,
            isAttendancePeriod = item.subject.isAttendanceSubject,
            isNow = isNow,
            isNext = item.entry.id == nextEntryId,
            accentColor = Color(item.subject.color),
            currentStatus = item.attendanceRecord?.status,
            onStatusSelected = { status ->
                viewModel.recordAttendance(item.entry.id, status)
            },
            onMoreClick = {
                onMoreClick(item.entry.id)
            }
        )
    }
}

fun LazyListScope.weeklyTabContent(uiState: DashboardUiState) {
    if (uiState.weeklyTimetable.isEmpty()) {
        item {
            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                Text("No classes configured for the week.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        uiState.weeklyTimetable.forEach { group ->
            item {
                Text(
                    text = group.dayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(group.items) { item ->
                WeeklyTimetableCard(item)
            }
        }
    }
}

@Composable
fun WeeklyTimetableCard(item: DashboardItem) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.entry.startTime,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.subject.subjectName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (item.subject.isAttendanceSubject) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "ATTENDANCE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
