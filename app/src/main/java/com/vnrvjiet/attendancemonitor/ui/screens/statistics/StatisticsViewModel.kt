package com.vnrvjiet.attendancemonitor.ui.screens.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.data.repository.*
import com.vnrvjiet.attendancemonitor.util.TimeUtils
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class DailyForecastModel(
    val date: Long,
    val classCount: Int,
    val isSunday: Boolean = false,
    val isLeave: Boolean = false
)

data class LeaveForecastUiState(
    val selectedDates: List<Long>,
    val currentPercentage: Float,
    val predictedPercentage: Float,
    val totalClassesMissed: Int,
    val change: Float,
    val status: String,
    val dailyBreakdown: List<DailyForecastModel> = emptyList()
)

data class StatisticsUiState(
    val overallPercentage: Float = 0f,
    val totalConducted: Int = 0,
    val totalAttended: Int = 0,
    val classesChange: Int = 0,
    val isLead: Boolean = true,
    val riskLevel: String = "SAFE",
    val lastSyncFormatted: String = "Never",
    val safeLeave75: Int = 0,
    val safeLeave80: Int = 0,
    val classesNeeded75: Int = 0,
    val classesNeeded80: Int = 0,
    val subjectStats: List<SubjectStatUiModel> = emptyList(),
    val isEmpty: Boolean = true
)

data class SubjectStatUiModel(
    val subjectName: String,
    val subjectCode: String,
    val percentage: Int,
    val attended: Int,
    val conducted: Int,
    val color: Int,
    val status: String
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val subjectRepo = RoomSubjectRepository(db.subjectDao())
    private val timetableRepo = RoomTimetableRepository(db.timetableDao())
    private val settingsRepo = SettingsRepository.getInstance(application)
    private val syncRepo = SyncRepository(
        EduPrimeRepository(),
        db.eduPrimeAttendanceDao(),
        db.subjectMappingDao(),
        NotificationRepository(db.notificationDao()),
        db.attendanceSnapshotDao(),
        settingsRepo,
        application
    )

    private val _selectedDates = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDates = _selectedDates.asStateFlow()

    private val _showForecast = MutableStateFlow(false)
    val showForecast = _showForecast.asStateFlow()

    fun toggleForecastDate(date: Long) {
        val current = _selectedDates.value.toMutableSet()
        // We only care about the date part
        val cal = Calendar.getInstance().apply { 
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val normalizedDate = cal.timeInMillis

        if (current.contains(normalizedDate)) {
            current.remove(normalizedDate)
        } else {
            current.add(normalizedDate)
        }
        _selectedDates.value = current
    }

    fun clearForecastDates() {
        _selectedDates.value = emptySet()
    }

    fun setShowForecast(show: Boolean) {
        _showForecast.value = show
    }

    val forecastState: StateFlow<LeaveForecastUiState?> = combine(
        syncRepo.syncedAttendance,
        timetableRepo.getAllTimetableEntries(),
        subjectRepo.getAllSubjects(),
        _selectedDates
    ) { remoteData, timetable, subjects, leaveDates ->
        if (remoteData.isEmpty() || leaveDates.isEmpty()) return@combine null

        val sortedLeaveDates = leaveDates.toList().sorted()
        val furthestLeaveDate = sortedLeaveDates.last()

        val currentAttended = remoteData.sumOf { it.attendedClasses }
        val currentConducted = remoteData.sumOf { it.conductedClasses }
        val currentPercentage = if (currentConducted > 0) (currentAttended.toFloat() / currentConducted * 100) else 0f

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMillis = todayCal.timeInMillis
        val currentMinutes = Calendar.getInstance().run { get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE) }

        var predictedAttended = currentAttended
        var predictedConducted = currentConducted
        var totalClassesMissed = 0
        val dailyBreakdown = mutableListOf<DailyForecastModel>()

        // Iterate through each day from today until furthestLeaveDate
        val iterCal = Calendar.getInstance().apply { timeInMillis = todayMillis }
        val finalCal = Calendar.getInstance().apply { timeInMillis = furthestLeaveDate }

        while (!iterCal.after(finalCal)) {
            val dayOfWeek = iterCal.get(Calendar.DAY_OF_WEEK)
            val mappedDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
            val currentTime = iterCal.timeInMillis
            val isToday = currentTime == todayMillis
            val isLeaveDay = leaveDates.contains(currentTime)

            if (mappedDay == 7) {
                if (isLeaveDay) {
                    dailyBreakdown.add(DailyForecastModel(currentTime, 0, isSunday = true, isLeave = true))
                }
            } else {
                val dayEntries = timetable.filter { it.dayOfWeek == mappedDay }
                var dayClassesMissed = 0

                dayEntries.forEach { entry ->
                    // For today, only count future classes
                    val endMinutes = TimeUtils.parseTimeToMinutes(entry.endTime)
                    if (!isToday || endMinutes > currentMinutes) {
                        val subject = subjects.find { it.id == entry.subjectId }
                        if (subject?.isAttendanceSubject == true) {
                            val units = calculateAttendanceUnits(entry)
                            predictedConducted += units
                            
                            if (isLeaveDay) {
                                // Explicitly selected leave date
                                totalClassesMissed += units
                                dayClassesMissed += units
                            } else {
                                // Normal future day (assumed present)
                                predictedAttended += units
                            }
                        }
                    }
                }
                if (isLeaveDay) {
                    dailyBreakdown.add(DailyForecastModel(currentTime, dayClassesMissed, isLeave = true))
                }
            }
            iterCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val predictedPercentage = if (predictedConducted > 0) (predictedAttended.toFloat() / predictedConducted * 100) else 0f
        val change = predictedPercentage - currentPercentage

        LeaveForecastUiState(
            selectedDates = sortedLeaveDates,
            currentPercentage = currentPercentage,
            predictedPercentage = predictedPercentage,
            totalClassesMissed = totalClassesMissed,
            change = change,
            status = getStatusLabel(predictedPercentage),
            dailyBreakdown = dailyBreakdown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun calculateAttendanceUnits(entry: TimetableEntryEntity): Int {
        val startMinutes = TimeUtils.parseTimeToMinutes(entry.startTime)
        val endMinutes = TimeUtils.parseTimeToMinutes(entry.endTime)
        val durationMinutes = endMinutes - startMinutes
        return if (durationMinutes > 0) {
            // Standard rule: 1 hour = 1 unit. 
            // We round to the nearest hour to handle slight deviations if any, 
            // but the prompt says 3-hour lab = 3 classes.
            (durationMinutes + 30) / 60 
        } else 0
    }

    val uiState: StateFlow<StatisticsUiState> = combine(
        subjectRepo.getAllSubjects(),
        syncRepo.syncedAttendance,
        db.subjectMappingDao().getAllMappings(),
        settingsRepo.lastVerified
    ) { subjects, remoteData, mappings, lastSync ->
        if (remoteData.isEmpty()) {
            return@combine StatisticsUiState(isEmpty = true)
        }

        val mappingMap = mappings.associate { it.subjectCode to it.subjectName }
        val threshold = 0.75f

        // Aggregate Overall Stats
        val overallPresent = remoteData.sumOf { it.attendedClasses }
        val overallTotal = remoteData.sumOf { it.conductedClasses }
        val overallPercentage = if (overallTotal > 0) (overallPresent.toFloat() / overallTotal) * 100 else 0f
        
        val isOverallLead = (overallPresent.toFloat() / (if (overallTotal == 0) 1 else overallTotal)) >= threshold
        val overallClassesChange = if (isOverallLead) {
            calculateSafeLeave(overallPresent, overallTotal, threshold)
        } else {
            calculateClassesNeeded(overallPresent, overallTotal, threshold)
        }

        // Subject-wise Stats
        val subjectStats = remoteData.map { remote ->
            val localSubject = subjects.find { it.subjectCode == remote.subjectCode }
            val mappedName = mappingMap[remote.subjectCode] ?: remote.subjectName ?: remote.subjectCode
            
            SubjectStatUiModel(
                subjectName = mappedName,
                subjectCode = remote.subjectCode,
                percentage = remote.attendancePercentage.toInt(),
                attended = remote.attendedClasses,
                conducted = remote.conductedClasses,
                color = localSubject?.color ?: 0xFF9E9E9E.toInt(),
                status = getStatusLabel(remote.attendancePercentage.toFloat())
            )
        }

        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        val lastSyncStr = if (lastSync == 0L) "Never" else sdf.format(Date(lastSync))

        StatisticsUiState(
            overallPercentage = overallPercentage,
            totalConducted = overallTotal,
            totalAttended = overallPresent,
            classesChange = overallClassesChange,
            isLead = isOverallLead,
            riskLevel = getStatusLabel(overallPercentage),
            lastSyncFormatted = lastSyncStr,
            safeLeave75 = calculateSafeLeave(overallPresent, overallTotal, 0.75f),
            safeLeave80 = calculateSafeLeave(overallPresent, overallTotal, 0.80f),
            classesNeeded75 = calculateClassesNeeded(overallPresent, overallTotal, 0.75f),
            classesNeeded80 = calculateClassesNeeded(overallPresent, overallTotal, 0.80f),
            subjectStats = subjectStats,
            isEmpty = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )

    private fun calculateSafeLeave(present: Int, total: Int, threshold: Float): Int {
        if (total == 0) return 0
        val currentPercentage = present.toFloat() / total
        if (currentPercentage < threshold) return 0
        
        val maxTotal = present / threshold
        return floor(maxTotal - total).toInt()
    }

    private fun calculateClassesNeeded(present: Int, total: Int, threshold: Float): Int {
        if (total == 0) return 0
        val currentPercentage = present.toFloat() / total
        if (currentPercentage >= threshold) return 0
        
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
}
