package com.vnrvjiet.attendancemonitor.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord
import com.vnrvjiet.attendancemonitor.data.repository.EduPrimeRepository
import com.vnrvjiet.attendancemonitor.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DebugAttendanceUiState {
    object Idle : DebugAttendanceUiState()
    object Loading : DebugAttendanceUiState()
    data class Success(val records: List<EduPrimeAttendanceRecord>) : DebugAttendanceUiState()
    object Empty : DebugAttendanceUiState()
    data class Error(val message: String) : DebugAttendanceUiState()
}

class DebugAttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val eduPrimeRepo = EduPrimeRepository()

    private val _uiState = MutableStateFlow<DebugAttendanceUiState>(DebugAttendanceUiState.Idle)
    val uiState: StateFlow<DebugAttendanceUiState> = _uiState.asStateFlow()

    fun fetchAttendance() {
        viewModelScope.launch {
            _uiState.value = DebugAttendanceUiState.Loading
            
            val user = repository.getUsername()
            val pass = repository.getPassword()
            val dob = repository.getDob()
            
            val result = eduPrimeRepo.fetchAttendance(user, pass, dob)
            
            result.fold(
                onSuccess = { records ->
                    if (records.isEmpty()) {
                        _uiState.value = DebugAttendanceUiState.Empty
                    } else {
                        _uiState.value = DebugAttendanceUiState.Success(records)
                    }
                },
                onFailure = { error ->
                    _uiState.value = DebugAttendanceUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
}
