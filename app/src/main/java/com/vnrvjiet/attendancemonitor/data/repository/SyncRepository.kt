package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.EduPrimeAttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import kotlinx.coroutines.flow.Flow

class SyncRepository(
    private val eduPrimeRepo: EduPrimeRepository,
    private val attendanceDao: EduPrimeAttendanceDao,
    private val settingsRepo: SettingsRepository
) {
    val syncedAttendance: Flow<List<EduPrimeAttendanceEntity>> = attendanceDao.getAllAttendance()

    suspend fun performSync(): Result<Unit> {
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
            Result.success(Unit)
        } else {
            Result.failure(fetchResult.exceptionOrNull() ?: Exception("Sync failed"))
        }
    }
}
