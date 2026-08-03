package com.vnrvjiet.attendancemonitor.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.vnrvjiet.attendancemonitor.util.SecurePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository private constructor(context: Context) {
    private val prefs = SecurePreferences.getPrefs(context)
    
    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

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

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        when (key) {
            "auto_sync" -> _autoSync.value = sharedPrefs.getBoolean(key, true)
            "alerts_attendance" -> _attendanceAlerts.value = sharedPrefs.getBoolean(key, true)
            "alerts_milestone" -> _milestoneAlerts.value = sharedPrefs.getBoolean(key, true)
            "alerts_mismatch" -> _mismatchAlerts.value = sharedPrefs.getBoolean(key, true)
            "app_theme" -> _theme.value = sharedPrefs.getString(key, "System") ?: "System"
            "last_verified" -> _lastVerified.value = sharedPrefs.getLong(key, 0L)
            "last_backup_at" -> _lastBackupAt.value = sharedPrefs.getLong(key, 0L)
            "last_auto_sync_at" -> _lastAutoSyncAt.value = sharedPrefs.getLong(key, 0L)
            "last_manual_sync_at" -> _lastManualSyncAt.value = sharedPrefs.getLong(key, 0L)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    private val _lastVerified = MutableStateFlow(prefs.getLong("last_verified", 0L))
    val lastVerified: StateFlow<Long> = _lastVerified.asStateFlow()

    private val _lastBackupAt = MutableStateFlow(prefs.getLong("last_backup_at", 0L))
    val lastBackupAt: StateFlow<Long> = _lastBackupAt.asStateFlow()

    private val _lastAutoSyncAt = MutableStateFlow(prefs.getLong("last_auto_sync_at", 0L))
    val lastAutoSyncAt: StateFlow<Long> = _lastAutoSyncAt.asStateFlow()

    private val _lastManualSyncAt = MutableStateFlow(prefs.getLong("last_manual_sync_at", 0L))
    val lastManualSyncAt: StateFlow<Long> = _lastManualSyncAt.asStateFlow()

    fun getUsername(): String = prefs.getString("eduprime_user", "") ?: ""
    fun getPassword(): String = prefs.getString("eduprime_pass", "") ?: ""
    fun getDob(): String = prefs.getString("eduprime_dob", "") ?: ""

    fun saveCredentials(user: String, pass: String, dob: String = "") {
        prefs.edit().apply {
            putString("eduprime_user", user)
            putString("eduprime_pass", pass)
            if (dob.isNotEmpty()) putString("eduprime_dob", dob)
            // Reset verification on credential change
            putLong("last_verified", 0L)
        }.apply()
        _lastVerified.value = 0L
    }

    fun setLastVerified(timestamp: Long) {
        prefs.edit().putLong("last_verified", timestamp).apply()
        _lastVerified.value = timestamp
    }

    fun setLastBackupAt(timestamp: Long) {
        prefs.edit().putLong("last_backup_at", timestamp).apply()
        _lastBackupAt.value = timestamp
    }

    fun setLastAutoSyncAt(timestamp: Long) {
        prefs.edit().putLong("last_auto_sync_at", timestamp).apply()
        _lastAutoSyncAt.value = timestamp
    }

    fun setLastManualSyncAt(timestamp: Long) {
        prefs.edit().putLong("last_manual_sync_at", timestamp).apply()
        _lastManualSyncAt.value = timestamp
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
