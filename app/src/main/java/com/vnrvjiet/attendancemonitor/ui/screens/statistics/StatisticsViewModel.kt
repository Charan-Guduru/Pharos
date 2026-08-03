package com.vnrvjiet.attendancemonitor.ui.screens.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.repository.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

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
    val percentage: Int,
    val attended: Int,
    val conducted: Int,
    val classesChange: Int,
    val isLead: Boolean,
    val color: Int,
    val status: String
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val subjectRepo = RoomSubjectRepository(db.subjectDao())
    private val settingsRepo = SettingsRepository.getInstance(application)
    private val syncRepo = SyncRepository(
        EduPrimeRepository(),
        db.eduPrimeAttendanceDao(),
        db.subjectMappingDao(),
        NotificationRepository(db.notificationDao()),
        settingsRepo,
        application
    )

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
            
            val isLead = (remote.attendedClasses.toFloat() / (if (remote.conductedClasses == 0) 1 else remote.conductedClasses)) >= threshold
            val classesChange = if (isLead) {
                calculateSafeLeave(remote.attendedClasses, remote.conductedClasses, threshold)
            } else {
                calculateClassesNeeded(remote.attendedClasses, remote.conductedClasses, threshold)
            }

            SubjectStatUiModel(
                subjectName = mappedName,
                percentage = remote.attendancePercentage.toInt(),
                attended = remote.attendedClasses,
                conducted = remote.conductedClasses,
                classesChange = classesChange,
                isLead = isLead,
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
