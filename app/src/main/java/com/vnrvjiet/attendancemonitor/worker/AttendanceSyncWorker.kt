package com.vnrvjiet.attendancemonitor.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.repository.*
import kotlinx.coroutines.flow.first

class AttendanceSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val WORK_TAG = "BackgroundSync"

    override suspend fun doWork(): Result {
        Log.i(WORK_TAG, "Worker Started")
        val database = AppDatabase.getDatabase(applicationContext)
        val settingsRepo = SettingsRepository.getInstance(applicationContext)
        
        // 1. Check if Auto Sync is enabled
        if (!settingsRepo.autoSync.first()) {
            Log.i(WORK_TAG, "Worker Finished: Auto Sync disabled")
            return Result.success()
        }

        val eduPrimeRepo = EduPrimeRepository()
        val syncRepo = SyncRepository(
            eduPrimeRepo,
            database.eduPrimeAttendanceDao(),
            database.subjectMappingDao(),
            NotificationRepository(database.notificationDao()),
            database.attendanceSnapshotDao(),
            settingsRepo,
            applicationContext
        )

        return try {
            val syncResult = syncRepo.performSync()
            if (syncResult.isSuccess) {
                Log.i(WORK_TAG, "Sync Success")
                settingsRepo.setLastAutoSyncAt(System.currentTimeMillis())
                
                // Trigger Verification Engine
                VerificationEngine(database, applicationContext).run()
                
                Log.i(WORK_TAG, "Worker Finished")
                Result.success()
            } else {
                Log.w(WORK_TAG, "Sync Failed: ${syncResult.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(WORK_TAG, "Worker Finished: Failed with exception", e)
            Result.retry()
        }
    }
}

