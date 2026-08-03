package com.vnrvjiet.attendancemonitor.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.VerificationState

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search subject...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.updateSearch("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    )
                )

                val filters = listOf("All", "Present", "Absent", "Bunk", "Pending", "Verified", "Mismatch")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = uiState.activeFilter == filter,
                            onClick = { viewModel.updateFilter(filter) },
                            label = { Text(filter) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        if (uiState.isEmpty) {
            EmptyHistoryState()
        } else if (uiState.groupedItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No records match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                uiState.groupedItems.forEach { (header, items) ->
                    item {
                        DateHeader(header)
                    }
                    items(items) { historyItem ->
                        HistoryItemCard(historyItem)
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(top = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
fun EmptyHistoryState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.History, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp), 
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No records found", 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun HistoryItemCard(item: HistoryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp, 
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color(item.subject.color), RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subject.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${item.entry.startTime} - ${item.entry.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(item.record.status)
                Spacer(modifier = Modifier.height(4.dp))
                VerificationBadge(item.record.verificationState)
            }
        }
    }
}

@Composable
fun StatusChip(status: AttendanceStatus) {
    val color = when (status) {
        AttendanceStatus.PRESENT -> Color(0xFF4CAF50)
        AttendanceStatus.ABSENT -> Color(0xFFF44336)
        AttendanceStatus.BUNK -> Color(0xFFFF9800)
        AttendanceStatus.VOLUNTEER -> Color(0xFF43A047)
        AttendanceStatus.MEDICAL_LEAVE -> Color(0xFFE91E63)
        AttendanceStatus.HOLIDAY -> Color(0xFF2196F3)
        else -> Color.Gray
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.widthIn(min = 70.dp)
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VerificationBadge(state: VerificationState) {
    val color = when (state) {
        VerificationState.VERIFIED -> Color(0xFF4CAF50)
        VerificationState.MISMATCH -> Color(0xFFF44336)
        VerificationState.PENDING -> MaterialTheme.colorScheme.outline
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when(state) {
                VerificationState.VERIFIED -> Icons.Default.CheckCircle
                VerificationState.MISMATCH -> Icons.Default.Error
                VerificationState.PENDING -> Icons.Default.Pending
            },
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = state.name,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
