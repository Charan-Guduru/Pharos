package com.vnrvjiet.attendancemonitor.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var dob by remember(uiState.dob) { mutableStateOf(uiState.dob) }
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
                        viewModel.updateCredentials(it, password, dob)
                    },
                    label = { Text("EduPrime Username") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { 
                        dob = it
                        viewModel.updateCredentials(username, password, it)
                    },
                    label = { Text("Date of Birth (DD-MM-YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        viewModel.updateCredentials(username, it, dob)
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
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.testEduPrimeLogin() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !uiState.isTestingLogin,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isTestingLogin) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Authenticating...")
                    } else {
                        Text("Test EduPrime Login")
                    }
                }

                uiState.loginTestResult?.let { result ->
                    Surface(
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        color = if (result.contains("✅")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = result,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (result.contains("✅")) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // SYNC SECTION
            SettingsSection(title = "Synchronization") {
                SettingsSwitchItem(
                    title = "Auto Sync",
                    subtitle = "Automatically fetch attendance from EduPrime",
                    checked = uiState.autoSync,
                    onCheckedChange = { viewModel.toggleAutoSync(it) }
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "Sync Schedule: 10:30, 12:30, 14:30, 16:00, 17:00, 18:00",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Privacy Policy")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Source Licenses")
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
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
