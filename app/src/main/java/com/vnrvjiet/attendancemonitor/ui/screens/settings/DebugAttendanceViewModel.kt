package com.vnrvjiet.attendancemonitor.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord
import com.vnrvjiet.attendancemonitor.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class DebugAttendanceUiState {
    object Idle : DebugAttendanceUiState()
    object Loading : DebugAttendanceUiState()
    data class Success(val records: List<EduPrimeAttendanceRecord>) : DebugAttendanceUiState()
    object Empty : DebugAttendanceUiState()
    data class Error(val message: String) : DebugAttendanceUiState()
}

class DebugAttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val settingsRepo = SettingsRepository(application)
    private val eduPrimeRepo = EduPrimeRepository()
    private val syncRepo = SyncRepository(
        eduPrimeRepo,
        db.eduPrimeAttendanceDao(),
        db.subjectMappingDao(),
        db.notificationDao(),
        settingsRepo,
        application
    )

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DebugAttendanceUiState> = combine(
        syncRepo.syncedAttendance,
        db.subjectMappingDao().getAllMappings(),
        _isLoading,
        _error
    ) { remoteData, mappings, isLoading, error ->
        val mappingMap = mappings.associate { it.subjectCode to it.subjectName }
        when {
            isLoading -> DebugAttendanceUiState.Loading
            error != null -> DebugAttendanceUiState.Error(error)
            remoteData.isEmpty() -> DebugAttendanceUiState.Empty
            else -> {
                val records = remoteData.map {
                    EduPrimeAttendanceRecord(
                        it.subjectCode,
                        mappingMap[it.subjectCode] ?: it.subjectName ?: it.subjectCode,
                        it.conductedClasses,
                        it.attendedClasses,
                        it.attendancePercentage
                    )
                }
                DebugAttendanceUiState.Success(records)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebugAttendanceUiState.Idle
    )

    fun fetchAttendance() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = syncRepo.performSync()
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Sync failed"
            }
            _isLoading.value = false
        }
    }
}
