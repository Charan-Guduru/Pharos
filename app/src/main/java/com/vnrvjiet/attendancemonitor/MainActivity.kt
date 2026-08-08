package com.vnrvjiet.attendancemonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.repository.*
import com.vnrvjiet.attendancemonitor.ui.navigation.AppNavigation
import com.vnrvjiet.attendancemonitor.ui.theme.PharosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val settingsRepository = SettingsRepository.getInstance(applicationContext)
        
        // Smart Startup Sync
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val syncRepo = SyncRepository(
                eduPrimeRepo = EduPrimeRepository(),
                attendanceDao = db.eduPrimeAttendanceDao(),
                mappingDao = db.subjectMappingDao(),
                notificationRepo = NotificationRepository(db.notificationDao()),
                snapshotDao = db.attendanceSnapshotDao(),
                settingsRepo = settingsRepository,
                context = applicationContext
            )
            // Ensure worker is scheduled if enabled
            syncRepo.initializeBackgroundWorker()
            
            // Smart Startup Sync
            syncRepo.tryStartupSync()
        }

        enableEdgeToEdge()
        setContent {
            val themePreference by settingsRepository.theme.collectAsState()
            
            PharosTheme(themePreference = themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
