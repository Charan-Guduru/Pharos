package com.vnrvjiet.attendancemonitor.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectMappingEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.VerificationState
import com.vnrvjiet.attendancemonitor.data.repository.RoomAttendanceRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomSubjectRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomTimetableRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class HistoryUiState(
    val groupedItems: Map<String, List<HistoryItem>> = emptyMap(),
    val isEmpty: Boolean = false,
    val searchQuery: String = "",
    val activeFilter: String = "All"
)

data class HistoryItem(
    val record: AttendanceRecordEntity,
    val entry: TimetableEntryEntity,
    val subject: SubjectEntity,
    val formattedDate: String
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val attendanceRepo = RoomAttendanceRepository(db.attendanceDao())
    private val timetableRepo = RoomTimetableRepository(db.timetableDao())
    private val subjectRepo = RoomSubjectRepository(db.subjectDao())

    private val dateFormatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow("All")

    val uiState: StateFlow<HistoryUiState> = combine(
        attendanceRepo.getAllRecords(),
        timetableRepo.getAllTimetableEntries(),
        subjectRepo.getAllSubjects(),
        db.subjectMappingDao().getAllMappings(),
        _searchQuery,
        _activeFilter
    ) { params: Array<Any?> ->
        val records = params[0] as List<AttendanceRecordEntity>
        val timetable = params[1] as List<TimetableEntryEntity>
        val subjects = params[2] as List<SubjectEntity>
        val mappings = params[3] as List<SubjectMappingEntity>
        val query = params[4] as String
        val filter = params[5] as String

        if (records.isEmpty()) {
            HistoryUiState(isEmpty = true, searchQuery = query, activeFilter = filter)
        } else {
            val mappingMap = mappings.associate { it.subjectCode to it.subjectName }
            
            val items = records.mapNotNull { record ->
                val entry = timetable.find { it.id == record.timetableEntryId }
                val subject = subjects.find { it.id == entry?.subjectId }
                if (entry != null && subject != null) {
                    val mappedName = mappingMap[subject.subjectCode] ?: subject.subjectName
                    
                    // Apply Search
                    if (query.isNotEmpty() && !mappedName.contains(query, ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    
                    // Apply Filter
                    if (!matchesFilter(record, filter)) {
                        return@mapNotNull null
                    }

                    val mappedSubject = subject.copy(subjectName = mappedName)
                    HistoryItem(
                        record = record,
                        entry = entry,
                        subject = mappedSubject,
                        formattedDate = dateFormatter.format(Date(record.date))
                    )
                } else null
            }
            
            val grouped = items.groupBy { item ->
                getGroupHeader(item.record.date)
            }
            
            HistoryUiState(
                groupedItems = grouped, 
                isEmpty = records.isEmpty(),
                searchQuery = query,
                activeFilter = filter
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    private fun matchesFilter(record: AttendanceRecordEntity, filter: String): Boolean {
        return when (filter) {
            "All" -> true
            "Present" -> record.status == AttendanceStatus.PRESENT
            "Absent" -> record.status == AttendanceStatus.ABSENT
            "Bunk" -> record.status == AttendanceStatus.BUNK
            "Pending" -> record.verificationState == VerificationState.PENDING
            "Verified" -> record.verificationState == VerificationState.VERIFIED
            "Mismatch" -> record.verificationState == VerificationState.MISMATCH
            else -> true
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _activeFilter.value = filter
    }

    private fun getGroupHeader(dateMillis: Long): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.timeInMillis

        return when (dateMillis) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> SimpleDateFormat("dd MMMM", Locale.getDefault()).format(Date(dateMillis))
        }
    }
}
