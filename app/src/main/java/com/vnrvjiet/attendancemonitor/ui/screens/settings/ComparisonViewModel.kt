package com.vnrvjiet.attendancemonitor.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.data.model.ComparisonResult
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
    private val eduPrimeRepo = EduPrimeRepository()
    private val comparisonRepo = AttendanceComparisonRepository()
    private val settingsRepo = SettingsRepository(application)
    private val syncRepo = SyncRepository(
        eduPrimeRepo,
        database.eduPrimeAttendanceDao(),
        settingsRepo
    )

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ComparisonUiState> = combine(
        syncRepo.syncedAttendance,
        attendanceRepo.getAllRecords(),
        timetableRepo.getAllTimetableEntries(),
        subjectRepo.getAllSubjects(),
        _isLoading,
        _error
    ) { params: Array<Any?> ->
        val remoteData = params[0] as List<EduPrimeAttendanceEntity>
        val localRecords = params[1] as List<AttendanceRecordEntity>
        val timetable = params[2] as List<TimetableEntryEntity>
        val subjects = params[3] as List<SubjectEntity>
        val isLoading = params[4] as Boolean
        val error = params[5] as String?

        // Convert EduPrimeAttendanceEntity to EduPrimeAttendanceRecord for the comparison engine
        val remoteRecords = remoteData.map {
            com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord(
                subjectCode = it.subjectCode,
                subjectName = it.subjectName ?: "",
                conductedClasses = it.conductedClasses,
                attendedClasses = it.attendedClasses,
                attendancePercentage = it.attendancePercentage
            )
        }

        val results = comparisonRepo.compare(localRecords, timetable, subjects, remoteRecords)
        
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
