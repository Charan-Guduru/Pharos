package com.vnrvjiet.attendancemonitor.worker

import android.content.Context
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.VerificationState
import com.vnrvjiet.attendancemonitor.data.repository.*
import kotlinx.coroutines.flow.first
import java.util.*

class AttendanceSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val settingsRepo = SettingsRepository(applicationContext)
        
        // 1. Check if Auto Sync is enabled
        if (!settingsRepo.autoSync.first()) {
            return Result.success()
        }

        // 2. Check Time Windows (09:00, 10:00, 12:30, 02:00, 05:00)
        // Simplified window check: allow sync if within 30 mins of target
        if (!isInSyncWindow()) {
            return Result.success()
        }

        val eduPrimeRepo = EduPrimeRepository()
        val syncRepo = SyncRepository(
            eduPrimeRepo,
            database.eduPrimeAttendanceDao(),
            database.subjectMappingDao(),
            database.notificationDao(),
            settingsRepo
        )

        return try {
            val syncResult = syncRepo.performSync()
            if (syncResult.isSuccess) {
                settingsRepo.setLastAutoSyncAt(System.currentTimeMillis())
                
                // 3. Trigger Verification Engine (driven by Comparison logic)
                performVerificationUpdate(database, eduPrimeRepo, syncRepo)
                
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun isInSyncWindow(): Boolean {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute

        val windows = listOf(
            9 * 60,         // 09:00
            10 * 60,        // 10:00
            12 * 60 + 30,   // 12:30
            14 * 60,        // 02:00 PM
            17 * 60         // 05:00 PM
        )

        // Return true if current time is within 30 mins after any window
        return windows.any { window -> currentMinutes in window..(window + 30) }
    }

    private suspend fun performVerificationUpdate(
        db: AppDatabase,
        eduPrimeRepo: EduPrimeRepository,
        syncRepo: SyncRepository
    ) {
        val attendanceRepo = RoomAttendanceRepository(db.attendanceDao())
        val timetableRepo = RoomTimetableRepository(db.timetableDao())
        val subjectRepo = RoomSubjectRepository(db.subjectDao())
        val comparisonRepo = AttendanceComparisonRepository()
        val mappingDao = db.subjectMappingDao()
        val notificationRepo = NotificationRepository(db.notificationDao())

        val localRecords = attendanceRepo.getAllRecords().first()
        val timetable = timetableRepo.getAllTimetableEntries().first()
        val subjects = subjectRepo.getAllSubjects().first()
        val remoteData = syncRepo.syncedAttendance.first()
        val mappings = mappingDao.getAllMappings().first()
        
        val mappingMap = mappings.associate { it.subjectCode to it.subjectName }

        val remoteRecords = remoteData.map {
            com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord(
                subjectCode = it.subjectCode,
                subjectName = mappingMap[it.subjectCode] ?: it.subjectName ?: it.subjectCode,
                conductedClasses = it.conductedClasses,
                attendedClasses = it.attendedClasses,
                attendancePercentage = it.attendancePercentage
            )
        }

        val (_, updatedRecords) = comparisonRepo.compare(localRecords, timetable, subjects, remoteRecords)
        
        // Update records and notify
        val timetableToSubjectCode = timetable.associate { it.id to (subjects.find { s -> s.id == it.subjectId }?.subjectCode ?: "UNKNOWN") }
        
        db.withTransaction {
            updatedRecords.forEach { new ->
                val old = localRecords.find { it.id == new.id }
                if (old != null && old.verificationState != new.verificationState) {
                    attendanceRepo.updateRecord(new)
                    
                    val subjectCode = timetableToSubjectCode[new.timetableEntryId] ?: "UNKNOWN"
                    val subjectName = mappingMap[subjectCode] ?: subjectCode
                    
                    when {
                        old.verificationState == VerificationState.PENDING && new.verificationState == VerificationState.VERIFIED -> {
                            if (new.status == AttendanceStatus.BUNK) {
                                notificationRepo.addNotification("Attendance Granted", "You received attendance for $subjectName.", "UNEXPECTED")
                            } else {
                                notificationRepo.addNotification("Attendance Verified", "Your attendance for $subjectName has been verified.", "VERIFIED")
                            }
                        }
                        old.verificationState == VerificationState.PENDING && new.verificationState == VerificationState.MISMATCH -> {
                            notificationRepo.addNotification("Attendance Mismatch", "Your attendance for $subjectName differs from EduPrime.", "MISMATCH")
                        }
                    }
                }
            }
        }
    }
}
