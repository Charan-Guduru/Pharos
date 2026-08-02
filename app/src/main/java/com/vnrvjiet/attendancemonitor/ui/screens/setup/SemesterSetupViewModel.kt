package com.vnrvjiet.attendancemonitor.ui.screens.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectMappingEntity
import com.vnrvjiet.attendancemonitor.data.repository.SubjectMappingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SemesterSetupUiState(
    val missingCodes: List<String> = emptyList(),
    val mappings: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isComplete: Boolean = false
)

class SemesterSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val mappingRepo = SubjectMappingRepository(db.subjectMappingDao())
    private val eduPrimeDao = db.eduPrimeAttendanceDao()

    private val _mappings = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _isSaving = MutableStateFlow(false)
    private val _isComplete = MutableStateFlow(false)

    val uiState: StateFlow<SemesterSetupUiState> = combine(
        eduPrimeDao.getAllAttendance(),
        mappingRepo.allMappings,
        _mappings,
        _isSaving,
        _isComplete
    ) { remoteData, existingMappings, currentEdits, saving, complete ->
        val mappedCodes = existingMappings.map { it.subjectCode }.toSet()
        val missing = remoteData.filter { it.subjectCode !in mappedCodes }.map { it.subjectCode }
        
        SemesterSetupUiState(
            missingCodes = missing,
            mappings = currentEdits,
            isSaving = saving,
            isComplete = complete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SemesterSetupUiState()
    )

    fun updateMapping(code: String, name: String) {
        if (name.length <= 60) {
            val updated = _mappings.value.toMutableMap()
            updated[code] = name
            _mappings.value = updated
        }
    }

    fun saveMappings() {
        val currentMappings = _mappings.value
        val missingCodes = uiState.value.missingCodes
        
        if (missingCodes.all { currentMappings[it]?.trim()?.isNotEmpty() == true }) {
            viewModelScope.launch {
                _isSaving.value = true
                val timestamp = System.currentTimeMillis()
                val entities = missingCodes.map { code ->
                    SubjectMappingEntity(
                        subjectCode = code,
                        subjectName = currentMappings[code]!!.trim(),
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )
                }
                mappingRepo.saveMappings(entities)
                _isSaving.value = false
                _isComplete.value = true
            }
        }
    }
}
