package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.AttendanceSnapshotDao
import com.vnrvjiet.attendancemonitor.data.local.dao.EduPrimeAttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectMappingDao
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceSnapshotEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import com.vnrvjiet.attendancemonitor.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    val syncedAttendance: Flow<List<EduPrimeAttendanceEntity>> = attendanceDao.getAllAttendance()

    /**
     * Performs synchronization and returns a Result.
     * The Boolean in Result indicates whether there are missing subject mappings.
     */
    suspend fun performSync(): Result<Boolean> {
        val user = settingsRepo.getUsername()
        val pass = settingsRepo.getPassword()
        val dob = settingsRepo.getDob()

        val fetchResult = eduPrimeRepo.fetchAttendance(user, pass, dob)
        
        return if (fetchResult.isSuccess) {
            val remoteRecords = fetchResult.getOrNull() ?: emptyList()
            val timestamp = System.currentTimeMillis()
            
            // 1. Snapshot logic: Only update snapshot if it's a NEW day and user has viewed previous changes
            val currentSnapshots = snapshotDao.getAllSnapshotsList()
            val hasUnviewed = settingsRepo.hasUnviewedChanges.first()
            
            if (currentSnapshots.isEmpty()) {
                // Initial snapshot creation
                val initialSnapshots = remoteRecords.map {
                    AttendanceSnapshotEntity(it.subjectCode, it.conductedClasses, it.attendedClasses, timestamp)
                }
                snapshotDao.updateSnapshot(initialSnapshots)
                settingsRepo.setHasUnviewedChanges(false)
            } else {
                val lastUpdate = currentSnapshots.firstOrNull()?.lastUpdated ?: 0L
                if (!isSameDay(lastUpdate, timestamp) && !hasUnviewed) {
                    // It's a new day and previous changes were acknowledged, so take a new snapshot of "Yesterday's final state"
                    // (Actually, if we are syncing NOW, this IS today's first state. The "Snapshot" is what we compare AGAINST)
                    // Requirement: "During the first successful synchronization of a new day: Compare Previous Snapshot ↓ Current synchronized values"
                    // "After the comparison has been acknowledged... replace the snapshot with today's synchronized values."
                    
                    // So if it's a new day, we check if there are changes.
                    val changed = remoteRecords.any { remote ->
                        val snap = currentSnapshots.find { it.subjectCode == remote.subjectCode }
                        snap == null || snap.conductedClasses != remote.conductedClasses || snap.attendedClasses != remote.attendedClasses
                    }
                    
                    if (changed) {
                        settingsRepo.setHasUnviewedChanges(true)
                    }
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
            
            // Check for missing mappings
            val existingMappings = mappingDao.getAllMappings().first()
            val mappedCodes = existingMappings.map { it.subjectCode }.toSet()
            val hasMissing = remoteRecords.any { it.subjectCode !in mappedCodes }
            
            Result.success(hasMissing)
        } else {
            val errorMsg = fetchResult.exceptionOrNull()?.message ?: "Sync failed"
            
            // Use repository for deduplication
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
        val currentAttendance = attendanceDao.getAllAttendance().first()
        val timestamp = System.currentTimeMillis()
        val newSnapshots = currentAttendance.map {
            AttendanceSnapshotEntity(it.subjectCode, it.conductedClasses, it.attendedClasses, timestamp)
        }
        snapshotDao.updateSnapshot(newSnapshots)
        settingsRepo.setHasUnviewedChanges(false)
    }
}
