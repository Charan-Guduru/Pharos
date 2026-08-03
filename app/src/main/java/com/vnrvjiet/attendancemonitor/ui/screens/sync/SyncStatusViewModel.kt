package com.vnrvjiet.attendancemonitor.ui.screens.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class SyncUiState(
    val lastSyncFormatted: String = "Never",
    val isSyncing: Boolean = false,
    val isAutoSyncEnabled: Boolean = true,
    val error: String? = null,
    val success: Boolean = false,
    val navigateToSetup: Boolean = false
)

class SyncStatusViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val settingsRepo = SettingsRepository.getInstance(application)
    private val syncRepo = SyncRepository(
        EduPrimeRepository(),
        db.eduPrimeAttendanceDao(),
        db.subjectMappingDao(),
        NotificationRepository(db.notificationDao()),
        settingsRepo,
        application
    )
    private val verificationEngine = VerificationEngine(db, application)

    val lastManualSyncAt = settingsRepo.lastManualSyncAt
    val lastAutoSyncAt = settingsRepo.lastAutoSyncAt
    val autoSyncEnabled = settingsRepo.autoSync

    val uiState: StateFlow<SyncUiState> = combine(
        lastManualSyncAt,
        lastAutoSyncAt,
        autoSyncEnabled
    ) { manual: Long, auto: Long, enabled: Boolean ->
        val lastSync = if (manual > auto) manual else auto
        SyncUiState(
            lastSyncFormatted = formatLastSync(lastSync),
            isAutoSyncEnabled = enabled,
            success = lastSync > 0L
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncUiState()
    )

    private val _isSyncing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _navigateToSetup = MutableStateFlow(false)

    // Merge manual states into the uiState
    val fullUiState: StateFlow<SyncUiState> = combine(uiState, _isSyncing, _error, _navigateToSetup) { state, syncing, error, navigate ->
        state.copy(isSyncing = syncing, error = error, navigateToSetup = navigate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncUiState())

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null
            _navigateToSetup.value = false
            
            val result = syncRepo.performSync()
            result.onSuccess { hasMissing ->
                settingsRepo.setLastManualSyncAt(System.currentTimeMillis())
                // Run verification engine after manual sync
                verificationEngine.run()
                
                if (hasMissing) {
                    _navigateToSetup.value = true
                }
            }.onFailure { e ->
                _error.value = e.message ?: "Sync failed"
            }
            
            _isSyncing.value = false
        }
    }

    fun onNavigatedToSetup() {
        _navigateToSetup.value = false
    }

    private fun formatLastSync(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        val diff = System.currentTimeMillis() - timestamp
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            else -> "${minutes / 60} hours ago"
        }
    }
}
