package com.vnrvjiet.attendancemonitor.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.data.repository.RoomAttendanceRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomSubjectRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomTimetableRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class HistoryUiState(
    val groupedItems: Map<String, List<HistoryItem>> = emptyMap(),
    val isEmpty: Boolean = false
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

    val uiState: StateFlow<HistoryUiState> = combine(
        attendanceRepo.getAllRecords(),
        timetableRepo.getAllTimetableEntries(),
        subjectRepo.getAllSubjects(),
        db.subjectMappingDao().getAllMappings()
    ) { records, timetable, subjects, mappings ->
        if (records.isEmpty()) {
            HistoryUiState(isEmpty = true)
        } else {
            val mappingMap = mappings.associate { it.subjectCode to it.subjectName }
            
            val items = records.mapNotNull { record ->
                val entry = timetable.find { it.id == record.timetableEntryId }
                val subject = subjects.find { it.id == entry?.subjectId }
                if (entry != null && subject != null) {
                    val mappedSubject = subject.copy(
                        subjectName = mappingMap[subject.subjectCode] ?: subject.subjectName
                    )
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
            
            HistoryUiState(groupedItems = grouped, isEmpty = items.isEmpty())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

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
            else -> "Older"
        }
    }
}
