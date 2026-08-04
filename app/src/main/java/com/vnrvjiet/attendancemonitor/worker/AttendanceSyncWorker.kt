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
}