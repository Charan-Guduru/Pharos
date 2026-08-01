package com.vnrvjiet.attendancemonitor.ui.screens.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.repository.RoomAttendanceRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomSubjectRepository
import com.vnrvjiet.attendancemonitor.data.repository.RoomTimetableRepository
import kotlinx.coroutines.flow.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class StatisticsUiState(
    val overallPercentage: Float = 0f,
    val safeLeave75: Int = 0,
    val safeLeave80: Int = 0,
    val classesNeeded75: Int = 0,
    val classesNeeded80: Int = 0,
    val subjectStats: List<SubjectStatUiModel> = emptyList(),
    val isEmpty: Boolean = true
)

data class SubjectStatUiModel(
    val subjectName: String,
    val percentage: Int,
    val classesDisplay: String,
    val color: Int,
    val status: String
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val subjectRepo = RoomSubjectRepository(db.subjectDao())
    private val attendanceRepo = RoomAttendanceRepository(db.attendanceDao())
    private val timetableRepo = RoomTimetableRepository(db.timetableDao())

    val uiState: StateFlow<StatisticsUiState> = combine(
        subjectRepo.getAllSubjects(),
        attendanceRepo.getAllRecords(),
        timetableRepo.getAllTimetableEntries()
    ) { subjects, records, timetable ->
        if (records.isEmpty()) {
            return@combine StatisticsUiState(isEmpty = true)
        }

        val attendanceSubjects = subjects.filter { it.isAttendanceSubject }
        val attendanceSubjectIds = attendanceSubjects.map { it.id }.toSet()

        // Filter records that belong to attendance subjects
        val validRecords = records.filter { record ->
            val entry = timetable.find { it.id == record.timetableEntryId }
            entry != null && attendanceSubjectIds.contains(entry.subjectId)
        }

        if (validRecords.isEmpty()) {
            return@combine StatisticsUiState(isEmpty = true)
        }

        // Overall Stats
        val overallStats = calculateStats(validRecords)
        
        // Subject-wise Stats
        val subjectStats = attendanceSubjects.map { subject ->
            val subjectTimetableIds = timetable.filter { it.subjectId == subject.id }.map { it.id }.toSet()
            val subjectRecords = validRecords.filter { subjectTimetableIds.contains(it.timetableEntryId) }
            val stats = calculateStats(subjectRecords)
            
            SubjectStatUiModel(
                subjectName = subject.subjectName,
                percentage = stats.percentage.toInt(),
                classesDisplay = "${stats.present}/${stats.total} Classes",
                color = subject.color,
                status = getStatusLabel(stats.percentage)
            )
        }

        StatisticsUiState(
            overallPercentage = overallStats.percentage,
            safeLeave75 = calculateSafeLeave(overallStats.present, overallStats.total, 0.75f),
            safeLeave80 = calculateSafeLeave(overallStats.present, overallStats.total, 0.80f),
            classesNeeded75 = calculateClassesNeeded(overallStats.present, overallStats.total, 0.75f),
            classesNeeded80 = calculateClassesNeeded(overallStats.present, overallStats.total, 0.80f),
            subjectStats = subjectStats,
            isEmpty = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )

    private fun calculateStats(records: List<AttendanceRecordEntity>): AttendanceCalculation {
        val present = records.count { it.status == AttendanceStatus.PRESENT }
        val absent = records.count { it.status == AttendanceStatus.ABSENT }
        val bunk = records.count { it.status == AttendanceStatus.BUNK }
        val total = present + absent + bunk
        
        val percentage = if (total > 0) (present.toFloat() / total) * 100 else 0f
        return AttendanceCalculation(present, total, percentage)
    }

    private fun calculateSafeLeave(present: Int, total: Int, threshold: Float): Int {
        if (total == 0) return 0
        val currentPercentage = present.toFloat() / total
        if (currentPercentage < threshold) return 0
        
        // P / (T + M) >= G  => M <= (P / G) - T
        val maxTotal = present / threshold
        return floor(maxTotal - total).toInt()
    }

    private fun calculateClassesNeeded(present: Int, total: Int, threshold: Float): Int {
        if (total == 0) return 0
        val currentPercentage = present.toFloat() / total
        if (currentPercentage >= threshold) return 0
        
        // (P + x) / (T + x) >= G => x >= (G*T - P) / (1 - G)
        val needed = ceil((threshold * total - present) / (1 - threshold))
        return max(0, needed.toInt())
    }

    private fun getStatusLabel(percentage: Float): String {
        return when {
            percentage >= 80f -> "SAFE"
            percentage >= 75f -> "AT RISK"
            else -> "LOW"
        }
    }

    private data class AttendanceCalculation(val present: Int, val total: Int, val percentage: Float)
}
