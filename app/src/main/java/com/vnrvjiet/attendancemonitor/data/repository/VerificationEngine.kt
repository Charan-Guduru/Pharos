package com.vnrvjiet.attendancemonitor.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.VerificationState
import com.vnrvjiet.attendancemonitor.util.NotificationHelper

class VerificationEngine(private val db: AppDatabase, private val context: Context) {
    private val TAG = "VerificationEngine"

    suspend fun run() {
        Log.d(TAG, "Starting verification engine")
        val comparisonRepo = AttendanceComparisonRepository()
        val notificationRepo = NotificationRepository(db.notificationDao())

        // Fetch all data using direct suspend methods to avoid Flow synchronization issues
        val localRecords = db.attendanceDao().getAllRecordsList()
        val timetable = db.timetableDao().getAllTimetableEntriesList()
        val subjects = db.subjectDao().getAllSubjectsList()
        val remoteData = db.eduPrimeAttendanceDao().getAllAttendanceList()
        val mappings = db.subjectMappingDao().getAllMappingsList()
        
        val lastSyncedAt = remoteData.firstOrNull()?.lastSyncedAt ?: 0L
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

        val (_, updatedRecords) = comparisonRepo.compare(
            localRecords, 
            timetable, 
            subjects, 
            remoteRecords,
            lastSyncedAt
        )
        
        val timetableToSubjectCode = timetable.associate { it.id to (subjects.find { s -> s.id == it.subjectId }?.subjectCode ?: "UNKNOWN") }
        
        db.withTransaction {
            updatedRecords.forEach { new ->
                val old = localRecords.find { it.id == new.id }
                if (old != null && old.verificationState != new.verificationState) {
                    Log.d(TAG, "Updating record ${new.id}: ${old.verificationState} -> ${new.verificationState}")
                    
                    // Critical: Update the record with new state AND refresh timestamp
                    db.attendanceDao().updateRecord(new.copy(lastModified = System.currentTimeMillis()))
                    
                    val subjectCode = timetableToSubjectCode[new.timetableEntryId] ?: "UNKNOWN"
                    val subjectName = mappingMap[subjectCode] ?: subjectCode
                    
                    when {
                        old.verificationState == VerificationState.PENDING && new.verificationState == VerificationState.VERIFIED -> {
                            if (new.status == AttendanceStatus.BUNK) {
                                notificationRepo.addNotification("Attendance Granted", "You received attendance for $subjectName.", "UNEXPECTED")
                                NotificationHelper.showNotification(context, "Attendance Granted", "You received attendance for $subjectName.", new.id.toInt())
                            } else {
                                notificationRepo.addNotification("Attendance Verified", "Your attendance for $subjectName has been verified.", "VERIFIED")
                                NotificationHelper.showNotification(context, "Attendance Verified", "Your attendance for $subjectName has been verified.", new.id.toInt())
                            }
                        }
                        old.verificationState == VerificationState.PENDING && new.verificationState == VerificationState.MISMATCH -> {
                            notificationRepo.addNotification("Attendance Mismatch", "Your attendance for $subjectName differs from EduPrime.", "MISMATCH")
                            NotificationHelper.showNotification(context, "Attendance Mismatch", "Your attendance for $subjectName differs from EduPrime.", new.id.toInt())
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Verification engine finished")
    }
}
