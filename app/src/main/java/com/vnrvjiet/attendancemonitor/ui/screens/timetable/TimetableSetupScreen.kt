package com.vnrvjiet.attendancemonitor.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
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
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.ui.components.WheelTimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSetupScreen(
    onComplete: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: TimetableSetupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable Setup") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.completeSetup() }) {
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Period")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Working Days", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S")
                days.forEachIndexed { index, day ->
                    val dayNum = index + 1
                    FilterChip(
                        selected = uiState.currentDay == dayNum,
                        onClick = { viewModel.setDay(dayNum) },
                        label = { Text(day) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Periods", style = MaterialTheme.typography.titleMedium)
                if (uiState.currentDay != 1) {
                    TextButton(onClick = { viewModel.copyFromMonday() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Mon")
                    }
                }
            }

            if (uiState.dailyEntries.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No periods added for this day.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.dailyEntries) { item ->
                        PeriodItem(
                            item = item,
                            onDelete = { viewModel.deleteEntry(item.entry) }
                        )
                    }
                }
            }
            
            Button(
                onClick = { viewModel.completeSetup() },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Continue to Dashboard")
            }
        }
    }

    if (showAddDialog) {
        AddPeriodDialog(
            subjects = uiState.mappedSubjects,
            onDismiss = { showAddDialog = false },
            onConfirm = { start, end, code, faculty, room, attendance ->
                viewModel.addEntry(start, end, code, faculty, room, attendance)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PeriodItem(item: TimetableItemModel, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.entry.startTime} - ${item.entry.endTime}", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.subjectName, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "Delete", 
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPeriodDialog(
    subjects: List<SubjectDisplayModel>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Boolean) -> Unit
) {
    var startTime by remember { mutableStateOf("09:00 AM") }
    var endTime by remember { mutableStateOf("10:00 AM") }
    var selectedSubjectCode by remember { mutableStateOf(subjects.firstOrNull()?.code ?: "") }
    var attendanceRequired by remember { mutableStateOf(true) }
    
    var expanded by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        WheelTimePickerDialog(
            initialTime = startTime,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                startTime = it
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        WheelTimePickerDialog(
            initialTime = endTime,
            onDismiss = { showEndPicker = false },
            onConfirm = {
                endTime = it
                showEndPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Period") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { },
                            label = { Text("Start") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.AccessTime, contentDescription = null)
                            }
                        )
                        // Invisible clickable layer over the field
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { showStartPicker = true }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { },
                            label = { Text("End") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.AccessTime, contentDescription = null)
                            }
                        )
                        // Invisible clickable layer over the field
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { showEndPicker = true }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = subjects.find { it.code == selectedSubjectCode }?.displayName ?: selectedSubjectCode,
                        onValueChange = { selectedSubjectCode = it },
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject.displayName) },
                                onClick = {
                                    selectedSubjectCode = subject.code
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedSubjectCode.isEmpty()) {
                    OutlinedTextField(value = selectedSubjectCode, onValueChange = { selectedSubjectCode = it }, label = { Text("Subject Code") }, modifier = Modifier.fillMaxWidth())
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = attendanceRequired, onCheckedChange = { attendanceRequired = it })
                    Text("Attendance Required")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(startTime, endTime, selectedSubjectCode, "", "", attendanceRequired) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
