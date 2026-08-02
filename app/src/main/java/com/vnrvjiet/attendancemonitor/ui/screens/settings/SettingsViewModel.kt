package com.vnrvjiet.attendancemonitor.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.model.LoginResult
import com.vnrvjiet.attendancemonitor.data.repository.EduPrimeRepository
import com.vnrvjiet.attendancemonitor.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val username: String = "",
    val dob: String = "",
    val autoSync: Boolean = true,
    val attendanceAlerts: Boolean = true,
    val milestoneAlerts: Boolean = true,
    val mismatchAlerts: Boolean = true,
    val theme: String = "System",
    val isTestingLogin: Boolean = false,
    val loginTestResult: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val eduPrimeRepo = EduPrimeRepository()

    private val _isTestingLogin = MutableStateFlow(false)
    private val _loginTestResult = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.autoSync,
        repository.attendanceAlerts,
        repository.milestoneAlerts,
        repository.mismatchAlerts,
        repository.theme,
        _isTestingLogin,
        _loginTestResult
    ) { params: Array<Any?> ->
        SettingsUiState(
            username = repository.getUsername(),
            dob = repository.getDob(),
            autoSync = params[0] as Boolean,
            attendanceAlerts = params[1] as Boolean,
            milestoneAlerts = params[2] as Boolean,
            mismatchAlerts = params[3] as Boolean,
            theme = params[4] as String,
            isTestingLogin = params[5] as Boolean,
            loginTestResult = params[6] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun testEduPrimeLogin() {
        viewModelScope.launch {
            _isTestingLogin.value = true
            _loginTestResult.value = "Testing credentials..."
            
            val user = repository.getUsername()
            val pass = repository.getPassword()
            val dob = repository.getDob()
            
            val result = eduPrimeRepo.testLogin(user, pass, dob)
            
            _loginTestResult.value = when (result) {
                is LoginResult.Success -> "✅ Login Successful"
                is LoginResult.InvalidCredentials -> "❌ Invalid Credentials"
                is LoginResult.NetworkUnavailable -> "❌ Network Unavailable"
                is LoginResult.Timeout -> "❌ Connection Timed Out"
                is LoginResult.TokenExtractionFailed -> "❌ Security Token Error"
                is LoginResult.HttpError -> "❌ Server Error (${result.code})"
                is LoginResult.AuthenticationFailed -> "❌ Authentication Failed"
                is LoginResult.UnexpectedResponse -> "❌ Unexpected Server Response"
                is LoginResult.Error -> "❌ Error: ${result.message}"
            }
            
            _isTestingLogin.value = false
        }
    }

    fun updateCredentials(user: String, pass: String, dob: String) {
        repository.saveCredentials(user, pass, dob)
        _loginTestResult.value = null
    }

    fun getPassword(): String = repository.getPassword()

    fun toggleAutoSync(enabled: Boolean) = repository.setAutoSync(enabled)
    fun toggleAttendanceAlerts(enabled: Boolean) = repository.setAttendanceAlerts(enabled)
    fun toggleMilestoneAlerts(enabled: Boolean) = repository.setMilestoneAlerts(enabled)
    fun toggleMismatchAlerts(enabled: Boolean) = repository.setMismatchAlerts(enabled)
    fun setTheme(theme: String) = repository.setTheme(theme)
}
