package com.vnrvjiet.attendancemonitor.ui.screens.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.repository.BackupRepository
import com.vnrvjiet.attendancemonitor.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupUiState(
    val lastBackupFormatted: String = "Never",
    val isProcessing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository.getInstance(application)
    private val backupRepo = BackupRepository(
        AppDatabase.getDatabase(application),
        settingsRepo
    )

    val uiState: StateFlow<BackupUiState> = settingsRepo.lastBackupAt.map { timestamp ->
        BackupUiState(
            lastBackupFormatted = if (timestamp == 0L) "Never" else java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupUiState())

    private val _isProcessing = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _isError = MutableStateFlow(false)

    val fullUiState = combine(uiState, _isProcessing, _message, _isError) { state, proc, msg, err ->
        state.copy(isProcessing = proc, message = msg, isError = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupUiState())

    fun exportBackup(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val json = backupRepo.createBackupJson()
                onJsonReady(json)
                settingsRepo.setLastBackupAt(System.currentTimeMillis())
                _message.value = "Backup data ready"
                _isError.value = false
            } catch (e: Exception) {
                _message.value = "Export failed: ${e.message}"
                _isError.value = true
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val json = reader.readText()
                inputStream?.close()

                val result = backupRepo.restoreFromJson(json)
                if (result.isSuccess) {
                    _message.value = "Restore successful! Restarting data..."
                    _isError.value = false
                } else {
                    _message.value = "Import failed: ${result.exceptionOrNull()?.message}"
                    _isError.value = true
                }
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
                _isError.value = true
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
