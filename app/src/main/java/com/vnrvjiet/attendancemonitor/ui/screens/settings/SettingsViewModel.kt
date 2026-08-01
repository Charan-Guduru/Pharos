package com.vnrvjiet.attendancemonitor.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*

data class SettingsUiState(
    val username: String = "",
    val autoSync: Boolean = true,
    val attendanceAlerts: Boolean = true,
    val milestoneAlerts: Boolean = true,
    val mismatchAlerts: Boolean = true,
    val theme: String = "System"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.autoSync,
        repository.attendanceAlerts,
        repository.milestoneAlerts,
        repository.mismatchAlerts,
        repository.theme
    ) { autoSync, attendance, milestone, mismatch, theme ->
        SettingsUiState(
            username = repository.getUsername(),
            autoSync = autoSync,
            attendanceAlerts = attendance,
            milestoneAlerts = milestone,
            mismatchAlerts = mismatch,
            theme = theme
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateCredentials(user: String, pass: String) {
        repository.saveCredentials(user, pass)
    }

    fun getPassword(): String = repository.getPassword()

    fun toggleAutoSync(enabled: Boolean) = repository.setAutoSync(enabled)
    fun toggleAttendanceAlerts(enabled: Boolean) = repository.setAttendanceAlerts(enabled)
    fun toggleMilestoneAlerts(enabled: Boolean) = repository.setMilestoneAlerts(enabled)
    fun toggleMismatchAlerts(enabled: Boolean) = repository.setMismatchAlerts(enabled)
    fun setTheme(theme: String) = repository.setTheme(theme)
}
