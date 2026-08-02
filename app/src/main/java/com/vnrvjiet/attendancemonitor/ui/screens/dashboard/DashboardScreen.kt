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
import com.vnrvjiet.attendancemonitor.data.repository.FakeSubjectRepository
import com.vnrvjiet.attendancemonitor.data.repository.FakeTimetableRepository
import com.vnrvjiet.attendancemonitor.ui.components.ActivityCard
import com.vnrvjiet.attendancemonitor.ui.components.AttendanceCard
import com.vnrvjiet.attendancemonitor.ui.components.MoreSituationsSheet
import com.vnrvjiet.attendancemonitor.ui.components.SummaryCard

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var selectedEntryId by remember { mutableStateOf<Long?>(null) }

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
            .background(Color(0xFFF8F9FA))
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
            SummaryCard()
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mon, Aug 2",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val days = listOf("M", "T", "W", "T", "F")
                    days.forEachIndexed { index, day ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (index == 0) Color(0xFF0D47A1) else Color.Transparent,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = day,
                                    color = if (index == 0) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        items(uiState.timetable) { item ->
            AttendanceCard(
                subject = item.subject.subjectName,
                faculty = item.subject.facultyName,
                time = item.entry.startTime,
                room = item.subject.roomNumber,
                isAttendancePeriod = item.subject.isAttendanceSubject,
                isNow = item.entry.id == 2L, // Dummy logic for current class
                statusText = if (item.entry.id == 2L) "At Risk (72%)" else null,
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

        item {
            ActivityCard()
        }
    }
}
