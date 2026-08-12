package com.vnrvjiet.attendancemonitor.ui.screens.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceSnapshotEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectMappingEntity
import com.vnrvjiet.attendancemonitor.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class SyncUiState(
    val lastSyncFormatted: String = "Never",
    val isSyncing: Boolean = false,
    val isAutoSyncEnabled: Boolean = true,
    val hasUnviewedChanges: Boolean = false,
    val showAttendancePercentage: Boolean = false,
    val dailyChanges: List<DailyChange> = emptyList(),
    val overallYesterday: Float? = null,
    val overallToday: Float? = null,
    val overallChange: Float? = null,
    val error: String? = null,
    val success: Boolean = false,
    val navigateToSetup: Boolean = false
)

data class DailyChange(
    val subjectName: String,
    val subjectCode: String,
    val prevConducted: Int,
    val prevAttended: Int,
    val currConducted: Int,
    val currAttended: Int
)

class SyncStatusViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val settingsRepo = SettingsRepository.getInstance(application)
    private val syncRepo = SyncRepository(
        EduPrimeRepository(),
        db.eduPrimeAttendanceDao(),
        db.subjectMappingDao(),
        NotificationRepository(db.notificationDao()),
        db.attendanceSnapshotDao(),
        settingsRepo,
        application
    )
    private val verificationEngine = VerificationEngine(db, application)

    val lastManualSyncAt = settingsRepo.lastManualSyncAt
    val lastAutoSyncAt = settingsRepo.lastAutoSyncAt
    val autoSyncEnabled = settingsRepo.autoSync

    val uiState: StateFlow<SyncUiState> = combine(
        listOf(
            lastManualSyncAt,
            lastAutoSyncAt,
            autoSyncEnabled,
            settingsRepo.hasUnviewedChanges,
            settingsRepo.showAttendancePercentage,
            db.eduPrimeAttendanceDao().getAllAttendance(),
            db.attendanceSnapshotDao().getAllSnapshots(),
            db.subjectMappingDao().getAllMappings()
        )
    ) { params ->
        val manual = params[0] as Long
        val auto = params[1] as Long
        val enabled = params[2] as Boolean
        val hasChanges = params[3] as Boolean
        val showPercentage = params[4] as Boolean
        val current = params[5] as List<EduPrimeAttendanceEntity>
        val snapshots = params[6] as List<AttendanceSnapshotEntity>
        val mappings = params[7] as List<SubjectMappingEntity>

        val lastSync = if (manual > auto) manual else auto
        val mappingMap = mappings.associate { it.subjectCode to it.subjectName }

        val dailyChanges = if (hasChanges) {
            current.mapNotNull { curr ->
                val snap = snapshots.find { it.subjectCode == curr.subjectCode }
                if (snap != null && (snap.conductedClasses != curr.conductedClasses || snap.attendedClasses != curr.attendedClasses)) {
                    DailyChange(
                        subjectName = mappingMap[curr.subjectCode] ?: curr.subjectName ?: curr.subjectCode,
                        subjectCode = curr.subjectCode,
                        prevConducted = snap.conductedClasses,
                        prevAttended = snap.attendedClasses,
                        currConducted = curr.conductedClasses,
                        currAttended = curr.attendedClasses
                    )
                } else null
            }
        } else emptyList()

        var overallYesterday: Float? = null
        var overallToday: Float? = null
        var overallChange: Float? = null

        if (showPercentage && snapshots.isNotEmpty()) {
            val totalYesterdayConducted = snapshots.sumOf { it.conductedClasses }
            val totalYesterdayAttended = snapshots.sumOf { it.attendedClasses }
            
            val totalTodayConducted = current.sumOf { it.conductedClasses }
            val totalTodayAttended = current.sumOf { it.attendedClasses }

            if (totalYesterdayConducted > 0) {
                overallYesterday = (totalYesterdayAttended.toFloat() / totalYesterdayConducted) * 100
            }
            if (totalTodayConducted > 0) {
                overallToday = (totalTodayAttended.toFloat() / totalTodayConducted) * 100
            }

            if (overallYesterday != null && overallToday != null) {
                overallChange = overallToday - overallYesterday
            }
        }

        SyncUiState(
            lastSyncFormatted = formatLastSync(lastSync),
            isAutoSyncEnabled = enabled,
            hasUnviewedChanges = hasChanges,
            showAttendancePercentage = showPercentage,
            dailyChanges = dailyChanges,
            overallYesterday = overallYesterday,
            overallToday = overallToday,
            overallChange = overallChange,
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

    fun acknowledgeChanges() {
        viewModelScope.launch {
            syncRepo.acknowledgeChanges()
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
