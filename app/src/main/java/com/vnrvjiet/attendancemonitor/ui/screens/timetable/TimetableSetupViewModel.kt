package com.vnrvjiet.attendancemonitor.ui.screens.timetable

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.data.repository.RoomSubjectRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomTimetableRepository
import com.vnrvjiet.attendancemonitor.data.repository.SubjectMappingRepository
import com.vnrvjiet.attendancemonitor.util.TimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TimetableSetupUiState(
    val mappedSubjects: List<SubjectDisplayModel> = emptyList(),
    val currentDay: Int = 1, // 1-7
    val dailyEntries: List<TimetableItemModel> = emptyList(),
    val isSaving: Boolean = false,
    val isComplete: Boolean = false
)

data class TimetableItemModel(
    val entry: TimetableEntryEntity,
    val subjectName: String
)

data class SubjectDisplayModel(
    val code: String,
    val displayName: String
)

class TimetableSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val timetableRepo = RoomTimetableRepository(db.timetableDao())
    private val subjectRepo = RoomSubjectRepository(db.subjectDao())
    private val mappingRepo = SubjectMappingRepository(db.subjectMappingDao())

    private val _currentDay = MutableStateFlow(1)
    private val _isSaving = MutableStateFlow(false)
    private val _isComplete = MutableStateFlow(false)

    val uiState: StateFlow<TimetableSetupUiState> = combine(
        mappingRepo.allMappings,
        _currentDay,
        timetableRepo.getAllTimetableEntries(),
        subjectRepo.getAllSubjects(),
        _isSaving,
        _isComplete
    ) { params: Array<Any?> ->
        val mappings = params[0] as List<com.vnrvjiet.attendancemonitor.data.local.entity.SubjectMappingEntity>
        val day = params[1] as Int
        val allEntries = params[2] as List<TimetableEntryEntity>
        val subjects = params[3] as List<SubjectEntity>
        val saving = params[4] as Boolean
        val complete = params[5] as Boolean

        val mapped = mappings.map { SubjectDisplayModel(it.subjectCode, it.subjectName) }
        val mappingMap = mappings.associate { it.subjectCode to it.subjectName }

        val dayEntries = allEntries.filter { it.dayOfWeek == day }
            .sortedBy { TimeUtils.parseTimeToMinutes(it.startTime) }
            .map { entry ->
                val subject = subjects.find { it.id == entry.subjectId }
                val name = if (subject != null) {
                    mappingMap[subject.subjectCode] ?: subject.subjectName
                } else "Unknown"
                
                TimetableItemModel(entry, name)
            }
        
        TimetableSetupUiState(
            mappedSubjects = mapped,
            currentDay = day,
            dailyEntries = dayEntries,
            isSaving = saving,
            isComplete = complete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimetableSetupUiState()
    )

    fun setDay(day: Int) {
        _currentDay.value = day
    }

    fun addEntry(startTime: String, endTime: String, subjectCode: String, faculty: String = "", room: String = "", isAttendance: Boolean = true) {
        viewModelScope.launch {
            val allEntries = timetableRepo.getAllTimetableEntries().first()
            val dayEntries = allEntries.filter { it.dayOfWeek == _currentDay.value }
            
            // Basic duplicate check
            if (dayEntries.any { it.startTime == startTime && it.endTime == endTime }) {
                return@launch
            }
            
            // First, find or create SubjectEntity
            val subjects = subjectRepo.getAllSubjects().first()
            var subject = subjects.find { it.subjectCode == subjectCode }
            
            if (subject == null) {
                val newSubject = SubjectEntity(
                    subjectCode = subjectCode,
                    subjectName = subjectCode, // Placeholder, usually name is in mapping
                    facultyName = faculty,
                    roomNumber = room,
                    color = 0xFF2196F3.toInt(),
                    isAttendanceSubject = isAttendance
                )
                subjectRepo.insertSubject(newSubject)
                subject = subjectRepo.getAllSubjects().first().find { it.subjectCode == subjectCode }
            }

            if (subject != null) {
                val entry = TimetableEntryEntity(
                    dayOfWeek = _currentDay.value,
                    startTime = startTime,
                    endTime = endTime,
                    subjectId = subject.id
                )
                timetableRepo.insertTimetableEntry(entry)
            }
        }
    }

    fun deleteEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            timetableRepo.deleteTimetableEntry(entry)
        }
    }

    fun copyFromMonday() {
        viewModelScope.launch {
            val mondayEntries = timetableRepo.getTimetableForDay(1).first()
            val currentEntries = timetableRepo.getTimetableForDay(_currentDay.value).first()
            
            // Clear current day
            currentEntries.forEach { timetableRepo.deleteTimetableEntry(it) }
            
            // Copy Monday
            mondayEntries.forEach { entry ->
                timetableRepo.insertTimetableEntry(entry.copy(id = 0, dayOfWeek = _currentDay.value))
            }
        }
    }

    fun completeSetup() {
        _isComplete.value = true
    }
}
