package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.EduPrimeAttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.dao.NotificationDao
import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectMappingDao
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SyncRepository(
    private val eduPrimeRepo: EduPrimeRepository,
    private val attendanceDao: EduPrimeAttendanceDao,
    private val mappingDao: SubjectMappingDao,
    private val notificationDao: NotificationDao,
    private val settingsRepo: SettingsRepository
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
            notificationDao.insertNotification(NotificationEntity(
                title = "Synchronization Failed",
                message = "Unable to synchronize with EduPrime: $errorMsg",
                type = "SYNC_FAILED",
                timestamp = System.currentTimeMillis()
            ))
            Result.failure(fetchResult.exceptionOrNull() ?: Exception("Sync failed"))
        }
    }
}
