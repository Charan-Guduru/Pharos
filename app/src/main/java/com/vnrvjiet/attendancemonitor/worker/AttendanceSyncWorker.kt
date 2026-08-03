package com.vnrvjiet.attendancemonitor.worker

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.VerificationState
import com.vnrvjiet.attendancemonitor.data.repository.*
import com.vnrvjiet.attendancemonitor.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.*

class AttendanceSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val WORK_TAG = "WorkManager"

    override suspend fun doWork(): Result {
        Log.d(WORK_TAG, "Worker started")
        val database = AppDatabase.getDatabase(applicationContext)
        val settingsRepo = SettingsRepository(applicationContext)
        
        // 1. Check if Auto Sync is enabled
        if (!settingsRepo.autoSync.first()) {
            Log.d(WORK_TAG, "Auto Sync disabled, finishing")
            return Result.success()
        }

        // 2. Check Time Windows (09:00, 10:00, 12:30, 02:00, 05:00)
        if (!isInSyncWindow()) {
            Log.d(WORK_TAG, "Outside sync window, finishing")
            return Result.success()
        }

        Log.d(WORK_TAG, "Within sync window, proceeding")

        val eduPrimeRepo = EduPrimeRepository()
        val syncRepo = SyncRepository(
            eduPrimeRepo,
            database.eduPrimeAttendanceDao(),
            database.subjectMappingDao(),
            database.notificationDao(),
            settingsRepo,
            applicationContext
        )

        return try {
            Log.d(WORK_TAG, "Synchronization started")
            val syncResult = syncRepo.performSync()
            if (syncResult.isSuccess) {
                Log.d(WORK_TAG, "Synchronization completed successfully")
                settingsRepo.setLastAutoSyncAt(System.currentTimeMillis())
                
                // 3. Trigger Verification Engine
                performVerificationUpdate(database, eduPrimeRepo, syncRepo)
                
                Log.d(WORK_TAG, "Worker finished successfully")
                Result.success()
            } else {
                Log.d(WORK_TAG, "Synchronization failed: ${syncResult.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(WORK_TAG, "Worker failed with exception", e)
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

        val result = windows.any { window -> currentMinutes in window..(window + 30) }
        Log.d(WORK_TAG, "Time check: currentMinutes=$currentMinutes, inWindow=$result")
        return result
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
                                NotificationHelper.showNotification(applicationContext, "Attendance Granted", "You received attendance for $subjectName.", new.id.toInt())
                            } else {
                                notificationRepo.addNotification("Attendance Verified", "Your attendance for $subjectName has been verified.", "VERIFIED")
                                NotificationHelper.showNotification(applicationContext, "Attendance Verified", "Your attendance for $subjectName has been verified.", new.id.toInt())
                            }
                        }
                        old.verificationState == VerificationState.PENDING && new.verificationState == VerificationState.MISMATCH -> {
                            notificationRepo.addNotification("Attendance Mismatch", "Your attendance for $subjectName differs from EduPrime.", "MISMATCH")
                            NotificationHelper.showNotification(applicationContext, "Attendance Mismatch", "Your attendance for $subjectName differs from EduPrime.", new.id.toInt())
                        }
                    }
                }
            }
        }
    }
}
