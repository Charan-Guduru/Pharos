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
        val settingsRepo = SettingsRepository.getInstance(applicationContext)
        
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
            NotificationRepository(database.notificationDao()),
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
                VerificationEngine(database, applicationContext).run()
                
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
}
