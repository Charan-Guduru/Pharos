package com.vnrvjiet.attendancemonitor.data.repository

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.dao.AttendanceSnapshotDao
import com.vnrvjiet.attendancemonitor.data.local.dao.EduPrimeAttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectMappingDao
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceSnapshotEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import com.vnrvjiet.attendancemonitor.util.NotificationHelper
import com.vnrvjiet.attendancemonitor.worker.AttendanceSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SyncRepository(
    private val eduPrimeRepo: EduPrimeRepository,
    private val attendanceDao: EduPrimeAttendanceDao,
    private val mappingDao: SubjectMappingDao,
    private val notificationRepo: NotificationRepository,
    private val snapshotDao: AttendanceSnapshotDao,
    private val settingsRepo: SettingsRepository,
    private val context: android.content.Context? = null
) {
    private val TAG = "SyncRepository"
    val syncedAttendance: Flow<List<EduPrimeAttendanceEntity>> = attendanceDao.getAllAttendance()
    
    companion object {
        private val syncMutex = Mutex()
    }

    /**
     * Performs synchronization and returns a Result.
     * The Boolean in Result indicates whether there are missing subject mappings.
     */
    suspend fun performSync(): Result<Boolean> = syncMutex.withLock {
        Log.d(TAG, "Starting synchronization...")
        val user = settingsRepo.getUsername()
        val pass = settingsRepo.getPassword()
        val dob = settingsRepo.getDob()

        if (user.isEmpty() || pass.isEmpty()) {
            Log.w(TAG, "Sync aborted: Missing credentials")
            return Result.failure(Exception("Missing credentials"))
        }

        val timestamp = System.currentTimeMillis()

        // 1. Day Transition Logic: If it's a new day, reset snapshot to represent "Yesterday's final state"
        val existingSnapshots = snapshotDao.getAllSnapshotsList()
        if (existingSnapshots.isNotEmpty()) {
            val lastSnapshotUpdate = existingSnapshots.firstOrNull()?.lastUpdated ?: 0L
            if (!isSameDay(lastSnapshotUpdate, timestamp)) {
                Log.i(TAG, "New day detected. Wiping yesterday's comparison.")
                val yesterdayFinal = attendanceDao.getAllAttendanceList()
                val freshSnapshots = yesterdayFinal.map {
                    AttendanceSnapshotEntity(it.subjectCode, it.conductedClasses, it.attendedClasses, timestamp)
                }
                snapshotDao.updateSnapshot(freshSnapshots)
                settingsRepo.setHasUnviewedChanges(false)
            }
        }

        val fetchResult = eduPrimeRepo.fetchAttendance(user, pass, dob)
        
        return if (fetchResult.isSuccess) {
            val remoteRecords = fetchResult.getOrNull() ?: emptyList()
            
            // 2. Change Detection logic
            val snapshots = snapshotDao.getAllSnapshotsList()
            if (snapshots.isEmpty()) {
                val initialSnapshots = remoteRecords.map {
                    AttendanceSnapshotEntity(it.subjectCode, it.conductedClasses, it.attendedClasses, timestamp)
                }
                snapshotDao.updateSnapshot(initialSnapshots)
                settingsRepo.setHasUnviewedChanges(false)
            } else {
                val hasNewChanges = remoteRecords.any { remote ->
                    val snap = snapshots.find { it.subjectCode == remote.subjectCode }
                    snap == null || snap.conductedClasses != remote.conductedClasses || snap.attendedClasses != remote.attendedClasses
                }
                
                if (hasNewChanges) {
                    Log.i(TAG, "New attendance changes detected.")
                    settingsRepo.setHasUnviewedChanges(true)
                }
            }

            val entities = remoteRecords.map { remote ->
                EduPrimeAttendanceEntity(
                    subjectCode = remote.subjectCode,
                    subjectName = remote.subjectName,
                    conductedClasses = remote.conductedClasses,
                    attendedClasses = remote.attendedClasses,
                    attendancePercentage = remote.attendancePercentage,
                    lastSyncedAt = timestamp
                )
            }
            
            attendanceDao.syncAttendance(entities)
            settingsRepo.setLastVerified(timestamp)
            
            val existingMappings = mappingDao.getAllMappings().first()
            val mappedCodes = existingMappings.map { it.subjectCode }.toSet()
            val hasMissing = remoteRecords.any { it.subjectCode !in mappedCodes }
            
            Log.d(TAG, "Sync successful (Timestamp updated)")
            Result.success(hasMissing)
        } else {
            val errorMsg = fetchResult.exceptionOrNull()?.message ?: "Sync failed"
            Log.e(TAG, "Sync failed: $errorMsg")
            
            notificationRepo.addNotification(
                title = "Synchronization Failed",
                message = "Unable to synchronize with EduPrime: $errorMsg",
                type = "SYNC_FAILED"
            )
            
            context?.let {
                NotificationHelper.showNotification(it, "Synchronization Failed", "Unable to synchronize with EduPrime.", 999)
            }
            Result.failure(fetchResult.exceptionOrNull() ?: Exception("Sync failed"))
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    suspend fun acknowledgeChanges() {
        val currentAttendance = attendanceDao.getAllAttendanceList()
        val timestamp = System.currentTimeMillis()
        val newSnapshots = currentAttendance.map {
            AttendanceSnapshotEntity(it.subjectCode, it.conductedClasses, it.attendedClasses, timestamp)
        }
        snapshotDao.updateSnapshot(newSnapshots)
        settingsRepo.setHasUnviewedChanges(false)
        Log.i(TAG, "Changes acknowledged. Snapshot updated.")
    }

    suspend fun tryStartupSync() {
        val enabled = settingsRepo.autoSync.first()
        if (!enabled) return

        val lastManual = settingsRepo.lastManualSyncAt.first()
        val lastAuto = settingsRepo.lastAutoSyncAt.first()
        val lastStartup = settingsRepo.lastStartupSyncAt.first()
        val lastSync = maxOf(lastManual, maxOf(lastAuto, lastStartup))
        
        val fifteenMinutes = 15 * 60 * 1000L
        if (System.currentTimeMillis() - lastSync > fifteenMinutes) {
            Log.i(TAG, "Executing Smart Startup Sync...")
            val result = performSync()
            if (result.isSuccess) {
                settingsRepo.setLastStartupSyncAt(System.currentTimeMillis())
                context?.let {
                    val db = AppDatabase.getDatabase(it)
                    VerificationEngine(db, it).run()
                }
            }
        } else {
            Log.d(TAG, "Startup Sync skipped: Last sync was recent")
        }
    }

    /**
     * Ensures the periodic background worker is scheduled if enabled.
     */
    suspend fun initializeBackgroundWorker() {
        val enabled = settingsRepo.autoSync.first()
        if (!enabled) {
            Log.d(TAG, "Background worker check: Disabled in settings")
            return
        }

        context?.let { ctx ->
            val workManager = WorkManager.getInstance(ctx)
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag("AttendanceSync")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "AttendanceSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.i(TAG, "Background worker initialization check complete (KEEP policy used)")
        }
    }
}
