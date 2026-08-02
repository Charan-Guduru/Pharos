package com.vnrvjiet.attendancemonitor.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
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

    private val _uiState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    fun performComparison() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val user = settingsRepo.getUsername()
                val pass = settingsRepo.getPassword()
                val dob = settingsRepo.getDob()
                
                // 1. Fetch latest from EduPrime
                val eduPrimeResult = eduPrimeRepo.fetchAttendance(user, pass, dob)
                
                if (eduPrimeResult.isSuccess) {
                    val remoteData = eduPrimeResult.getOrNull() ?: emptyList()
                    
                    // 2. Fetch all local data once using first() to get the current list
                    val localRecords = attendanceRepo.getAllRecords().first()
                    val timetable = timetableRepo.getAllTimetableEntries().first()
                    val subjects = subjectRepo.getAllSubjects().first()
                    
                    // 3. Perform comparison
                    val results = comparisonRepo.compare(localRecords, timetable, subjects, remoteData)
                    
                    _uiState.value = _uiState.value.copy(
                        results = results,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = eduPrimeResult.exceptionOrNull()?.message ?: "Fetch failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Comparison error"
                )
            }
        }
    }
}
