package com.vnrvjiet.attendancemonitor.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vnrvjiet.attendancemonitor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageTimetableClick: () -> Unit,
    onDebugAttendanceClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    backupViewModel: BackupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backupState by backupViewModel.fullUiState.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    
    var username by remember(uiState.username) { mutableStateOf(uiState.username) }
    var dob by remember(uiState.dob) { mutableStateOf(uiState.dob) }
    var password by remember { mutableStateOf(viewModel.getPassword()) }
    var passwordVisible by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                backupViewModel.exportBackup { json ->
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(json.toByteArray())
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { backupViewModel.importBackup(it) }
        }
    )

    LaunchedEffect(backupState.message) {
        backupState.message?.let {
            // Show toast or Snack bar (simplified to toast for this task)
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            backupViewModel.clearMessage()
        }
    }

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

                if (uiState.lastVerified > 0L && uiState.loginTestResult?.contains("✅") != false) {
                    val sdfDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                    val sdfTime = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    val dateStr = sdfDate.format(Date(uiState.lastVerified))
                    val timeStr = sdfTime.format(Date(uiState.lastVerified))

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        color = StatusVerified.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusVerified.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusVerified,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "🟢 EduPrime Verified",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusVerified,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Last Verified:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    "$dateStr at $timeStr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.testEduPrimeLogin() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !uiState.isTestingLogin && uiState.cooldownSeconds == 0,
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
                    } else if (uiState.cooldownSeconds > 0) {
                        Text("Cooldown (${uiState.cooldownSeconds}s)")
                    } else if (uiState.lastVerified > 0L) {
                        Text("Verify Again")
                    } else {
                        Text("Test EduPrime Login")
                    }
                }

                uiState.loginTestResult?.let { result ->
                    if (!result.contains("✅")) {
                        Surface(
                            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = result,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // SYNC SECTION
            SettingsSection(title = "Synchronization") {
                OutlinedButton(
                    onClick = onManageTimetableClick,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Manage Timetable")
                }

                SettingsSwitchItem(
                    title = "Auto Sync",
                    subtitle = "Automatically fetch attendance from EduPrime",
                    checked = uiState.autoSync,
                    onCheckedChange = { viewModel.toggleAutoSync(it) }
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text =  "Keeps your attendance synchronized automatically in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            // BACKUP SECTION
            SettingsSection(title = "Backup & Restore") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Last Backup: ${backupState.lastBackupFormatted}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("AttendanceMonitor_Backup.json") },
                        modifier = Modifier.weight(1f),
                        enabled = !backupState.isProcessing,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export")
                    }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        enabled = !backupState.isProcessing,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import")
                    }
                }
                
                if (backupState.isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
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

            // DEBUG SECTION (TEMPORARY)
            SettingsSection(title = "Developer Settings (Debug)") {
                OutlinedButton(
                    onClick = onDebugAttendanceClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetch EduPrime Attendance (Debug)")
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
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        Text(
            text = label, 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}