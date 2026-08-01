package com.vnrvjiet.attendancemonitor.data.repository

import android.content.Context
import com.vnrvjiet.attendancemonitor.util.SecurePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs = SecurePreferences.getPrefs(context)

    private val _autoSync = MutableStateFlow(prefs.getBoolean("auto_sync", true))
    val autoSync: StateFlow<Boolean> = _autoSync.asStateFlow()

    private val _attendanceAlerts = MutableStateFlow(prefs.getBoolean("alerts_attendance", true))
    val attendanceAlerts: StateFlow<Boolean> = _attendanceAlerts.asStateFlow()

    private val _milestoneAlerts = MutableStateFlow(prefs.getBoolean("alerts_milestone", true))
    val milestoneAlerts: StateFlow<Boolean> = _milestoneAlerts.asStateFlow()

    private val _mismatchAlerts = MutableStateFlow(prefs.getBoolean("alerts_mismatch", true))
    val mismatchAlerts: StateFlow<Boolean> = _mismatchAlerts.asStateFlow()

    private val _theme = MutableStateFlow(prefs.getString("app_theme", "System") ?: "System")
    val theme: StateFlow<String> = _theme.asStateFlow()

    fun getUsername(): String = prefs.getString("eduprime_user", "") ?: ""
    fun getPassword(): String = prefs.getString("eduprime_pass", "") ?: ""

    fun saveCredentials(user: String, pass: String) {
        prefs.edit().apply {
            putString("eduprime_user", user)
            putString("eduprime_pass", pass)
        }.apply()
    }

    fun setAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync", enabled).apply()
        _autoSync.value = enabled
    }

    fun setAttendanceAlerts(enabled: Boolean) {
        prefs.edit().putBoolean("alerts_attendance", enabled).apply()
        _attendanceAlerts.value = enabled
    }

    fun setMilestoneAlerts(enabled: Boolean) {
        prefs.edit().putBoolean("alerts_milestone", enabled).apply()
        _milestoneAlerts.value = enabled
    }

    fun setMismatchAlerts(enabled: Boolean) {
        prefs.edit().putBoolean("alerts_mismatch", enabled).apply()
        _mismatchAlerts.value = enabled
    }

    fun setTheme(theme: String) {
        prefs.edit().putString("app_theme", theme).apply()
        _theme.value = theme
    }
}
