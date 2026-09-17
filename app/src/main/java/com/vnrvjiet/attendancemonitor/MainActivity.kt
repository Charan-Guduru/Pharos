package com.vnrvjiet.attendancemonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.repository.*
import com.vnrvjiet.attendancemonitor.ui.components.UpdateDialog
import com.vnrvjiet.attendancemonitor.ui.navigation.AppNavigation
import com.vnrvjiet.attendancemonitor.ui.theme.PharosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private lateinit var settingsRepository: SettingsRepository
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        settingsRepository = SettingsRepository.getInstance(applicationContext)
        val updateRepository = UpdateRepository.getInstance(applicationContext)
        
        // Connectivity Listener to trigger sync when internet returns
        setupConnectivityListener()

        // Smart Startup Sync - ensure we catch up after long gaps
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                triggerSyncCheck()
            }
        })

        // Check for updates
        lifecycleScope.launch(Dispatchers.IO) {
            updateRepository.checkForUpdate(manualCheck = intent?.getBooleanExtra("SHOW_UPDATE_DIALOG", false) == true)
        }

        enableEdgeToEdge()
        setContent {
            val themePreference by settingsRepository.theme.collectAsState()
            val updateState by updateRepository.updateState.collectAsState()
            
            PharosTheme(themePreference = themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                    
                    if (updateState is UpdateState.Available || updateState is UpdateState.Downloading || updateState is UpdateState.ReadyToInstall || updateState is UpdateState.DownloadFailed || updateState is UpdateState.Installing) {
                        UpdateDialog(
                            updateState = updateState,
                            onDownload = { manifest -> updateRepository.downloadAndInstallUpdate(manifest) },
                            onInstall = { uri -> updateRepository.installUpdate(uri) },
                            onDismiss = { 
                                intent?.putExtra("SHOW_UPDATE_DIALOG", false)
                                updateRepository.dismissUpdate() 
                            }
                        )
                    }
                }
            }
        }
    }

    private fun triggerSyncCheck() {
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
            
            // Smart Sync (handles both startup and return from background)
            syncRepo.tryStartupSync()
        }
    }

    private fun setupConnectivityListener() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Only trigger if app is in foreground
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    triggerSyncCheck()
                }
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
