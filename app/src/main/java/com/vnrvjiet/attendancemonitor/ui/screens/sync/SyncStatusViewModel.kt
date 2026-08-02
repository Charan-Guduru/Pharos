package com.vnrvjiet.attendancemonitor.ui.screens.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.repository.EduPrimeRepository
import com.vnrvjiet.attendancemonitor.data.repository.SettingsRepository
import com.vnrvjiet.attendancemonitor.data.repository.SyncRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class SyncUiState(
    val lastSyncFormatted: String = "Never",
    val isSyncing: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class SyncStatusViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val settingsRepo = SettingsRepository(application)
    private val syncRepo = SyncRepository(
        EduPrimeRepository(),
        db.eduPrimeAttendanceDao(),
        settingsRepo
    )

    val uiState: StateFlow<SyncUiState> = settingsRepo.lastVerified.map { timestamp ->
        SyncUiState(
            lastSyncFormatted = formatLastSync(timestamp),
            success = timestamp > 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncUiState()
    )

    private val _isSyncing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Merge manual states into the uiState
    val fullUiState: StateFlow<SyncUiState> = combine(uiState, _isSyncing, _error) { state, syncing, error ->
        state.copy(isSyncing = syncing, error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncUiState())

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null
            
            val result = syncRepo.performSync()
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Sync failed"
            }
            
            _isSyncing.value = false
        }
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
