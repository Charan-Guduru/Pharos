package com.vnrvjiet.attendancemonitor.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OSSLibrary(
    val name: String,
    val license: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit
) {
    val libraries = listOf(
        OSSLibrary("Jetpack Compose", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
        OSSLibrary("Room Persistence", "Apache 2.0", "https://developer.android.com/training/data-storage/room"),
        OSSLibrary("Retrofit", "Apache 2.0", "https://square.github.io/retrofit/"),
        OSSLibrary("OkHttp", "Apache 2.0", "https://square.github.io/okhttp/"),
        OSSLibrary("Gson", "Apache 2.0", "https://github.com/google/gson"),
        OSSLibrary("Jsoup", "MIT", "https://jsoup.org/"),
        OSSLibrary("WorkManager", "Apache 2.0", "https://developer.android.com/topic/libraries/architecture/workmanagement"),
        OSSLibrary("Android Security Crypto", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/security"),
        OSSLibrary("Kotlin Coroutines", "Apache 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
        OSSLibrary("Material Components", "Apache 2.0", "https://material.io/components")
    ).sortedBy { it.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp)
        ) {
            items(libraries) { lib ->
                ListItem(
                    headlineContent = { Text(lib.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(lib.license) },
                    trailingContent = {
                        Text(
                            text = "View",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
