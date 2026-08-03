package com.vnrvjiet.attendancemonitor.ui.screens.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    onBack: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: SyncStatusViewModel = viewModel()
) {
    val uiState by viewModel.fullUiState.collectAsStateWithLifecycle()
    val lastManual by viewModel.lastManualSyncAt.collectAsStateWithLifecycle()
    val lastAuto by viewModel.lastAutoSyncAt.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigateToSetup) {
        if (uiState.navigateToSetup) {
            onNavigateToSetup()
            viewModel.onNavigatedToSetup()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = if (uiState.error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    } else {
                        Icon(
                            if (uiState.error != null) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = if (uiState.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = when {
                    uiState.isSyncing -> "Syncing..."
                    uiState.error != null -> "Sync Failed"
                    else -> "Synced Successfully"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = uiState.error ?: "Your timetable and attendance are up to date.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            SyncInfoCard(
                lastManual = formatTimestamp(lastManual),
                lastAuto = formatTimestamp(lastAuto),
                isAutoEnabled = uiState.isAutoSyncEnabled
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Attendance is synchronized automatically in the background while your device is connected to the internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.syncNow() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isSyncing,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Now")
            }
        }
    }
}

@Composable
fun SyncInfoCard(lastManual: String, lastAuto: String, isAutoEnabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SyncDetailItem("BACKGROUND SYNC", if (isAutoEnabled) "Enabled" else "Disabled")
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SyncDetailItem("LAST MANUAL SYNC", lastManual)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SyncDetailItem("LAST AUTO SYNC", lastAuto)
        }
    }
}

@Composable
fun SyncDetailItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
