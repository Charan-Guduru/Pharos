package com.vnrvjiet.attendancemonitor.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.SyncStatus
import com.vnrvjiet.attendancemonitor.data.repository.RoomAttendanceRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomSubjectRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomTimetableRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class DashboardUiState(
    val timetable: List<DashboardItem> = emptyList()
)

data class DashboardItem(
    val entry: TimetableEntryEntity,
    val subject: SubjectEntity,
    val attendanceRecord: AttendanceRecordEntity? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val timetableRepo = RoomTimetableRepository(db.timetableDao())
    private val subjectRepo = RoomSubjectRepository(db.subjectDao())
    private val attendanceRepo = RoomAttendanceRepository(db.attendanceDao())

    private val todayMidnight: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    val uiState: StateFlow<DashboardUiState> = combine(
        timetableRepo.getTimetableForDay(1),
        subjectRepo.getAllSubjects(),
        attendanceRepo.getRecordsForDate(todayMidnight)
    ) { timetable, subjects, records ->
        val items = timetable.mapNotNull { entry ->
            subjects.find { it.id == entry.subjectId }?.let { subject ->
                val record = records.find { it.timetableEntryId == entry.id }
                DashboardItem(entry, subject, record)
            }
        }
        DashboardUiState(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun recordAttendance(timetableEntryId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            val existingRecord = attendanceRepo.getRecordForEntryAndDate(timetableEntryId, todayMidnight)
            if (existingRecord != null) {
                val updatedRecord = existingRecord.copy(
                    status = status,
                    lastModified = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING
                )
                attendanceRepo.updateRecord(updatedRecord)
            } else {
                val newRecord = AttendanceRecordEntity(
                    date = todayMidnight,
                    timetableEntryId = timetableEntryId,
                    status = status,
                    syncStatus = SyncStatus.PENDING,
                    remarks = null,
                    lastModified = System.currentTimeMillis()
                )
                attendanceRepo.insertRecord(newRecord)
            }
        }
    }
}
