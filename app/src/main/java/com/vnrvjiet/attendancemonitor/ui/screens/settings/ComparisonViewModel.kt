package com.vnrvjiet.attendancemonitor.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.*
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.ComparisonResult
import com.vnrvjiet.attendancemonitor.data.model.VerificationState
import com.vnrvjiet.attendancemonitor.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ComparisonUiState(
    val results: List<ComparisonResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ComparisonViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val attendanceRepo = RoomAttendanceRepository(database.attendanceDao())
    private val timetableRepo = RoomTimetableRepository(database.timetableDao())
    private val subjectRepo = RoomSubjectRepository(database.subjectDao())
    private val notificationRepo = NotificationRepository(database.notificationDao())
    private val eduPrimeRepo = EduPrimeRepository()
    private val comparisonRepo = AttendanceComparisonRepository()
    private val settingsRepo = SettingsRepository(application)
    private val syncRepo = SyncRepository(
        eduPrimeRepo,
        database.eduPrimeAttendanceDao(),
        database.subjectMappingDao(),
        database.notificationDao(),
        settingsRepo
    )

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ComparisonUiState> = combine(
        syncRepo.syncedAttendance,
        attendanceRepo.getAllRecords(),
        timetableRepo.getAllTimetableEntries(),
        subjectRepo.getAllSubjects(),
        database.subjectMappingDao().getAllMappings(),
        _isLoading,
        _error
    ) { params: Array<Any?> ->
        val remoteData = params[0] as List<EduPrimeAttendanceEntity>
        val localRecords = params[1] as List<AttendanceRecordEntity>
        val timetable = params[2] as List<TimetableEntryEntity>
        val subjects = params[3] as List<SubjectEntity>
        val mappings = params[4] as List<SubjectMappingEntity>
        val isLoading = params[5] as Boolean
        val error = params[6] as String?

        val mappingMap = mappings.associate { it.subjectCode to it.subjectName }

        // Convert EduPrimeAttendanceEntity to EduPrimeAttendanceRecord for the comparison engine
        val remoteRecords = remoteData.map {
            com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord(
                subjectCode = it.subjectCode,
                subjectName = mappingMap[it.subjectCode] ?: it.subjectName ?: it.subjectCode,
                conductedClasses = it.conductedClasses,
                attendedClasses = it.attendedClasses,
                attendancePercentage = it.attendancePercentage
            )
        }

        val (results, updatedRecords) = comparisonRepo.compare(localRecords, timetable, subjects, remoteRecords)
        
        // Trigger background update and notifications
        viewModelScope.launch {
            val timetableToSubjectCode = timetable.associate { it.id to (subjects.find { s -> s.id == it.subjectId }?.subjectCode ?: "UNKNOWN") }
            processRecordChanges(localRecords, updatedRecords, mappingMap, timetableToSubjectCode)
        }

        ComparisonUiState(
            results = results,
            isLoading = isLoading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ComparisonUiState()
    )

    private suspend fun processRecordChanges(
        oldRecords: List<AttendanceRecordEntity>,
        newRecords: List<AttendanceRecordEntity>,
        mappingMap: Map<String, String>,
        timetableToSubjectCode: Map<Long, String>
    ) {
        newRecords.forEach { new ->
            val old = oldRecords.find { it.id == new.id }
            if (old != null && old.verificationState != new.verificationState) {
                // Update Room
                attendanceRepo.updateRecord(new)
                
                // Trigger Notifications
                val subjectCode = timetableToSubjectCode[new.timetableEntryId] ?: "UNKNOWN"
                val subjectName = mappingMap[subjectCode] ?: subjectCode
                
                when {
                    old.verificationState == VerificationState.PENDING && new.verificationState == VerificationState.VERIFIED -> {
                        if (new.status == AttendanceStatus.BUNK) {
                            notificationRepo.addNotification(
                                title = "Attendance Granted",
                                message = "You received attendance for $subjectName.",
                                type = "UNEXPECTED"
                            )
                        } else {
                            notificationRepo.addNotification(
                                title = "Attendance Verified",
                                message = "Your attendance for $subjectName has been verified.",
                                type = "VERIFIED"
                            )
                        }
                    }
                    old.verificationState == VerificationState.PENDING && new.verificationState == VerificationState.MISMATCH -> {
                        notificationRepo.addNotification(
                            title = "Attendance Mismatch",
                            message = "Your attendance for $subjectName differs from EduPrime.",
                            type = "MISMATCH"
                        )
                    }
                }
            }
        }
    }

    fun performComparison() {
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
