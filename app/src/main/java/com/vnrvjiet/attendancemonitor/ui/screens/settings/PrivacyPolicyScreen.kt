package com.vnrvjiet.attendancemonitor.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrivacySection(
                title = "Data Collection",
                content = "Pharos does not collect any personal data. All information you provide, including your EduPrime credentials, is stored exclusively on your device."
            )

            PrivacySection(
                title = "Security",
                content = "Your EduPrime username and password are encrypted and stored locally using Android's EncryptedSharedPreferences. This ensures that even if someone gains access to your device's storage, your credentials remain secure."
            )

            PrivacySection(
                title = "Data Transmission",
                content = "The application only transmits your credentials to the official EduPrime server to fetch your attendance data. No data is shared with the developers or any third parties."
            )

            PrivacySection(
                title = "No Tracking",
                content = "We do not use any analytics, advertising, or third-party tracking libraries. Your usage patterns are private to you."
            )

            PrivacySection(
                title = "Offline Access",
                content = "Your attendance history and timetable are stored in a local database (Room). This allows you to view your records even when you are offline."
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Last Updated: August 2026",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
