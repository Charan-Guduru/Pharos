package com.vnrvjiet.attendancemonitor.ui.screens.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.repository.RoomSubjectRepository
import com.vnrvjiet.attendancemonitor.data.repository.SyncRepository
import com.vnrvjiet.attendancemonitor.data.repository.EduPrimeRepository
import com.vnrvjiet.attendancemonitor.data.repository.SettingsRepository
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
    private val settingsRepo = SettingsRepository(application)
    private val syncRepo = SyncRepository(
        EduPrimeRepository(),
        db.eduPrimeAttendanceDao(),
        db.subjectMappingDao(),
        settingsRepo
    )

    val uiState: StateFlow<StatisticsUiState> = combine(
        subjectRepo.getAllSubjects(),
        syncRepo.syncedAttendance,
        db.subjectMappingDao().getAllMappings()
    ) { subjects, remoteData, mappings ->
        if (remoteData.isEmpty()) {
            return@combine StatisticsUiState(isEmpty = true)
        }

        val mappingMap = mappings.associate { it.subjectCode to it.subjectName }

        // Aggregate Overall Stats
        val overallPresent = remoteData.sumOf { it.attendedClasses }
        val overallTotal = remoteData.sumOf { it.conductedClasses }
        val overallPercentage = if (overallTotal > 0) (overallPresent.toFloat() / overallTotal) * 100 else 0f
        
        // Subject-wise Stats
        val subjectStats = remoteData.map { remote ->
            val localSubject = subjects.find { it.subjectCode == remote.subjectCode }
            val mappedName = mappingMap[remote.subjectCode] ?: remote.subjectName ?: remote.subjectCode
            
            SubjectStatUiModel(
                subjectName = mappedName,
                percentage = remote.attendancePercentage.toInt(),
                classesDisplay = "${remote.attendedClasses}/${remote.conductedClasses} Classes",
                color = localSubject?.color ?: 0xFF9E9E9E.toInt(),
                status = getStatusLabel(remote.attendancePercentage.toFloat())
            )
        }

        StatisticsUiState(
            overallPercentage = overallPercentage,
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
}
