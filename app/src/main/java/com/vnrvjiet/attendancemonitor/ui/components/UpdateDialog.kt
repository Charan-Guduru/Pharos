package com.vnrvjiet.attendancemonitor.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnrvjiet.attendancemonitor.data.model.UpdateManifest
import com.vnrvjiet.attendancemonitor.data.repository.UpdateState

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    onDownload: (UpdateManifest) -> Unit,
    onInstall: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    if (updateState is UpdateState.Available) {
        val manifest = updateState.manifest
        // ... (existing code for Available)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Pharos Update Available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Version ${manifest.versionName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "What's New",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = manifest.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onDownload(manifest) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update Now")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    } else if (updateState is UpdateState.Downloading) {
        AlertDialog(
            onDismissRequest = { /* No dismiss while downloading to avoid accidental cancels */ },
            title = {
                Text(
                    text = "Downloading Update",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    Text(
                        text = "Please wait while the update is downloading... You will be prompted to install it shortly.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {},
            dismissButton = {},
            shape = RoundedCornerShape(24.dp)
        )
    } else if (updateState is UpdateState.ReadyToInstall) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Update Downloaded",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "The new version of Pharos is ready to install.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { onInstall(updateState.uri) }) {
                    Text("Install Now")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    } else if (updateState is UpdateState.DownloadFailed) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Download Failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = updateState.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    } else if (updateState is UpdateState.Installing) {
        AlertDialog(
            onDismissRequest = { /* System handles installer UI */ },
            title = {
                Text(
                    text = "Installing Update",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Handing off to Android Package Installer...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {},
            dismissButton = {},
            shape = RoundedCornerShape(24.dp)
        )
    }
}
