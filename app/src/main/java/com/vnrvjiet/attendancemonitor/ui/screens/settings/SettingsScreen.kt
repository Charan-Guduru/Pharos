package com.vnrvjiet.attendancemonitor.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var username by remember(uiState.username) { mutableStateOf(uiState.username) }
    var password by remember { mutableStateOf(viewModel.getPassword()) }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ACCOUNT SECTION
            SettingsSection(title = "Account") {
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        viewModel.updateCredentials(it, password)
                    },
                    label = { Text("EduPrime Username") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        viewModel.updateCredentials(username, it)
                    },
                    label = { Text("EduPrime Password") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
            }

            // SYNC SECTION
            SettingsSection(title = "Synchronization") {
                SettingsSwitchItem(
                    title = "Auto Sync",
                    subtitle = "Automatically fetch attendance from EduPrime",
                    checked = uiState.autoSync,
                    onCheckedChange = { viewModel.toggleAutoSync(it) }
                )
                
                Text(
                    text = "Sync Schedule: 10:30, 12:30, 14:30, 16:00, 17:00, 18:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // NOTIFICATIONS SECTION
            SettingsSection(title = "Notifications") {
                SettingsSwitchItem(
                    title = "Attendance Alerts",
                    subtitle = "Notify when attendance is recorded",
                    checked = uiState.attendanceAlerts,
                    onCheckedChange = { viewModel.toggleAttendanceAlerts(it) }
                )
                SettingsSwitchItem(
                    title = "Milestone Alerts",
                    subtitle = "Notify when attendance goals are reached",
                    checked = uiState.milestoneAlerts,
                    onCheckedChange = { viewModel.toggleMilestoneAlerts(it) }
                )
                SettingsSwitchItem(
                    title = "Mismatch Alerts",
                    subtitle = "Notify if EduPrime data differs from local",
                    checked = uiState.mismatchAlerts,
                    onCheckedChange = { viewModel.toggleMismatchAlerts(it) }
                )
            }

            // APPEARANCE SECTION
            SettingsSection(title = "Appearance") {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = uiState.theme,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Theme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("System", "Light", "Dark").forEach { theme ->
                            DropdownMenuItem(
                                text = { Text(theme) },
                                onClick = {
                                    viewModel.setTheme(theme)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ABOUT SECTION
            SettingsSection(title = "About") {
                AboutItem("App Version", "1.0.0")
                AboutItem("Developer", "VNR VJIET Student")
                TextButton(onClick = {}, modifier = Modifier.padding(start = 0.dp)) {
                    Text("Privacy Policy")
                }
                TextButton(onClick = {}, modifier = Modifier.padding(start = 0.dp)) {
                    Text("Open Source Licenses")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun AboutItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
