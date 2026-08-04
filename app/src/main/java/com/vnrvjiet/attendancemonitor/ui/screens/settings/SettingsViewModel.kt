package com.vnrvjiet.attendancemonitor.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.vnrvjiet.attendancemonitor.data.model.LoginResult
import com.vnrvjiet.attendancemonitor.data.repository.EduPrimeRepository
import com.vnrvjiet.attendancemonitor.data.repository.SettingsRepository
import com.vnrvjiet.attendancemonitor.worker.AttendanceSyncWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

data class SettingsUiState(
    val username: String = "",
    val dob: String = "",
    val autoSync: Boolean = true,
    val attendanceAlerts: Boolean = true,
    val milestoneAlerts: Boolean = true,
    val mismatchAlerts: Boolean = true,
    val theme: String = "System",
    val isTestingLogin: Boolean = false,
    val loginTestResult: String? = null,
    val lastVerified: Long = 0L,
    val cooldownSeconds: Int = 0
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository.getInstance(application)
    private val eduPrimeRepo = EduPrimeRepository()

    private val _isTestingLogin = MutableStateFlow(false)
    private val _loginTestResult = MutableStateFlow<String?>(null)
    private val _cooldownSeconds = MutableStateFlow(0)
    private var cooldownJob: Job? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        listOf(
            repository.autoSync,
            repository.attendanceAlerts,
            repository.milestoneAlerts,
            repository.mismatchAlerts,
            repository.theme,
            repository.lastVerified,
            _isTestingLogin,
            _loginTestResult,
            _cooldownSeconds
        )
    ) { params ->
        SettingsUiState(
            username = repository.getUsername(),
            dob = repository.getDob(),
            autoSync = params[0] as Boolean,
            attendanceAlerts = params[1] as Boolean,
            milestoneAlerts = params[2] as Boolean,
            mismatchAlerts = params[3] as Boolean,
            theme = params[4] as String,
            lastVerified = params[5] as Long,
            isTestingLogin = params[6] as Boolean,
            loginTestResult = params[7] as String?,
            cooldownSeconds = params[8] as Int
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun testEduPrimeLogin() {
        if (_cooldownSeconds.value > 0) return

        viewModelScope.launch {
            _isTestingLogin.value = true
            _loginTestResult.value = "Testing credentials..."
            
            val user = repository.getUsername()
            val pass = repository.getPassword()
            val dob = repository.getDob()
            
            val result = eduPrimeRepo.testLogin(user, pass, dob)
            
            _loginTestResult.value = when (result) {
                is LoginResult.Success -> {
                    repository.setLastVerified(System.currentTimeMillis())
                    startCooldown()
                    "✅ Login Successful"
                }
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

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (i in 30 downTo 1) {
                _cooldownSeconds.value = i
                delay(1000)
            }
            _cooldownSeconds.value = 0
        }
    }

    fun updateCredentials(user: String, pass: String, dob: String) {
        repository.saveCredentials(user, pass, dob)
        _loginTestResult.value = null
        _cooldownSeconds.value = 0
        cooldownJob?.cancel()
    }

    fun getPassword(): String = repository.getPassword()

    fun toggleAutoSync(enabled: Boolean) {
        repository.setAutoSync(enabled)
        val workManager = WorkManager.getInstance(getApplication())
        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag("AttendanceSync")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "AttendanceSync",
                ExistingPeriodicWorkPolicy.REPLACE,
                syncRequest
            )
        } else {
            workManager.cancelUniqueWork("AttendanceSync")
        }
    }
    fun toggleAttendanceAlerts(enabled: Boolean) = repository.setAttendanceAlerts(enabled)
    fun toggleMilestoneAlerts(enabled: Boolean) = repository.setMilestoneAlerts(enabled)
    fun toggleMismatchAlerts(enabled: Boolean) = repository.setMismatchAlerts(enabled)
    fun setTheme(theme: String) = repository.setTheme(theme)
}
