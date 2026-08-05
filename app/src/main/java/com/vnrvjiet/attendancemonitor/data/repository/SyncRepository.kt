package com.vnrvjiet.attendancemonitor.data.repository

import android.util.Log
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.dao.AttendanceSnapshotDao
import com.vnrvjiet.attendancemonitor.data.local.dao.EduPrimeAttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectMappingDao
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceSnapshotEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import com.vnrvjiet.attendancemonitor.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

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
                // Take snapshot of current local data (yesterday's final state)
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
                // Initial bootstrap: Store current portal data as baseline
                val initialSnapshots = remoteRecords.map {
                    AttendanceSnapshotEntity(it.subjectCode, it.conductedClasses, it.attendedClasses, timestamp)
                }
                snapshotDao.updateSnapshot(initialSnapshots)
                settingsRepo.setHasUnviewedChanges(false)
            } else {
                // Detect if any subject has changed relative to the CURRENT snapshot
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

    /**
     * Acknowledges today's changes by updating the snapshot to the current state.
     */
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

    /**
     * Performs a silent sync on app startup if conditions are met.
     */
    suspend fun tryStartupSync() {
        val enabled = settingsRepo.autoSync.first()
        if (!enabled) return

        val lastManual = settingsRepo.lastManualSyncAt.first()
        val lastAuto = settingsRepo.lastAutoSyncAt.first()
        val lastSync = if (lastManual > lastAuto) lastManual else lastAuto
        
        val fifteenMinutes = 15 * 60 * 1000L
        if (System.currentTimeMillis() - lastSync > fifteenMinutes) {
            Log.i(TAG, "Executing Smart Startup Sync...")
            val result = performSync()
            if (result.isSuccess) {
                settingsRepo.setLastAutoSyncAt(System.currentTimeMillis())
                // We also need to run verification if it was a startup sync
                context?.let {
                    val db = AppDatabase.getDatabase(it)
                    VerificationEngine(db, it).run()
                }
            }
        } else {
            Log.d(TAG, "Startup Sync skipped: Last sync was recent")
        }
    }
}
