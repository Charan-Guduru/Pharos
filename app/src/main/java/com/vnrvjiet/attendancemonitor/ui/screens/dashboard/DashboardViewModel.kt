package com.vnrvjiet.attendancemonitor.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.*
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.SyncStatus
import com.vnrvjiet.attendancemonitor.data.repository.RoomAttendanceRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomSubjectRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomTimetableRepository
import com.vnrvjiet.attendancemonitor.util.TimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class DashboardUiState(
    val timetable: List<DashboardItem> = emptyList(),
    val weeklyTimetable: List<WeeklyDayGroup> = emptyList(),
    val overallPercentage: Float = 0f,
    val isSynced: Boolean = false,
    val isTimetableConfigured: Boolean = true,
    val isLoading: Boolean = true,
    val classesChange: Int = 0,
    val isLead: Boolean = true
)

data class WeeklyDayGroup(
    val dayOfWeek: Int,
    val dayName: String,
    val items: List<DashboardItem>
)

data class DashboardItem(
    val entry: TimetableEntryEntity,
    val subject: SubjectEntity,
    val attendanceRecord: AttendanceRecordEntity? = null,
    val remoteAttendance: EduPrimeAttendanceEntity? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val timetableRepo = RoomTimetableRepository(db.timetableDao())
    private val subjectRepo = RoomSubjectRepository(db.subjectDao())
    private val attendanceRepo = RoomAttendanceRepository(db.attendanceDao())
    private val mappingDao = db.subjectMappingDao()
    private val eduPrimeDao = db.eduPrimeAttendanceDao()

    private val todayMidnight: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private val currentDayOfWeek: Int
        get() = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let { 
            if (it == Calendar.SUNDAY) 7 else it - 1
        }

    val uiState: StateFlow<DashboardUiState> = combine(
        timetableRepo.getAllTimetableEntries(),
        subjectRepo.getAllSubjects(),
        attendanceRepo.getRecordsForDate(todayMidnight),
        eduPrimeDao.getAllAttendance(),
        mappingDao.getAllMappings()
    ) { params: Array<Any?> ->
        val allEntries = params[0] as List<TimetableEntryEntity>
        val subjects = params[1] as List<SubjectEntity>
        val records = params[2] as List<AttendanceRecordEntity>
        val remoteData = params[3] as List<EduPrimeAttendanceEntity>
        val mappings = params[4] as List<SubjectMappingEntity>

        val mappingMap = mappings.associate { it.subjectCode to it.subjectName }
        
        val allDashboardItems = allEntries.mapNotNull { entry ->
            subjects.find { it.id == entry.subjectId }?.let { subject ->
                val record = if (entry.dayOfWeek == currentDayOfWeek) {
                    records.find { it.timetableEntryId == entry.id }
                } else null
                
                val remote = remoteData.find { it.subjectCode == subject.subjectCode }
                
                val mappedSubject = subject.copy(
                    subjectName = mappingMap[subject.subjectCode] ?: subject.subjectName
                )
                
                DashboardItem(entry, mappedSubject, record, remote)
            }
        }

        val todayItems = allDashboardItems.filter { it.entry.dayOfWeek == currentDayOfWeek }
            .sortedBy { TimeUtils.parseTimeToMinutes(it.entry.startTime) }

        val dayNames = listOf("", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val weeklyGroups = (1..6).map { day ->
            WeeklyDayGroup(
                dayOfWeek = day,
                dayName = dayNames[day],
                items = allDashboardItems.filter { it.entry.dayOfWeek == day }
                    .sortedBy { TimeUtils.parseTimeToMinutes(it.entry.startTime) }
            )
        }.filter { it.items.isNotEmpty() }
        
        val overallPresent = remoteData.sumOf { it.attendedClasses }
        val overallTotal = remoteData.sumOf { it.conductedClasses }
        val percentage = if (overallTotal > 0) (overallPresent.toFloat() / overallTotal) * 100 else 0f
        
        val threshold = 0.75f
        val isLead = (overallPresent.toFloat() / (if (overallTotal == 0) 1 else overallTotal)) >= threshold
        
        val classesChange = if (isLead) {
            if (overallTotal == 0) 0 else kotlin.math.floor(overallPresent.toFloat() / threshold - overallTotal).toInt()
        } else {
            if (overallTotal == 0) 0 else kotlin.math.ceil((threshold * overallTotal - overallPresent) / (1 - threshold)).toInt()
        }

        DashboardUiState(
            timetable = todayItems,
            weeklyTimetable = weeklyGroups,
            overallPercentage = percentage,
            isSynced = remoteData.isNotEmpty(),
            isTimetableConfigured = allEntries.isNotEmpty(),
            isLoading = false,
            classesChange = kotlin.math.max(0, classesChange),
            isLead = isLead
        )
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
