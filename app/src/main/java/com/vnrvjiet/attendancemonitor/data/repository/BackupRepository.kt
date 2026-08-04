package com.vnrvjiet.attendancemonitor.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.vnrvjiet.attendancemonitor.data.local.AppDatabase
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.data.model.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(
    private val database: AppDatabase,
    private val settingsRepo: SettingsRepository
) {
    private val gson = Gson()
    private val subjectDao = database.subjectDao()
    private val timetableDao = database.timetableDao()
    private val attendanceDao = database.attendanceDao()
    private val mappingDao = database.subjectMappingDao()

    suspend fun createBackupJson(): String {
        val subjects = subjectDao.getAllSubjects().first()
        val timetable = timetableDao.getAllTimetableEntries().first()
        val attendance = attendanceDao.getAllRecords().first()
        val mappings = mappingDao.getAllMappings().first()

        val portableTimetable = timetable.map { entry ->
            val subject = subjects.find { it.id == entry.subjectId }
            PortableTimetableEntry(
                dayOfWeek = entry.dayOfWeek,
                startTime = entry.startTime,
                endTime = entry.endTime,
                subjectCode = subject?.subjectCode ?: "UNKNOWN",
                subjectName = subject?.subjectName ?: subject?.subjectCode ?: "Unknown"
            )
        }

        val portableAttendance = attendance.map { record ->
            val entry = timetable.find { it.id == record.timetableEntryId }
            val subject = subjects.find { it.id == entry?.subjectId }
            PortableAttendanceRecord(
                date = record.date,
                dayOfWeek = entry?.dayOfWeek ?: 0,
                startTime = entry?.startTime ?: "",
                subjectCode = subject?.subjectCode ?: "UNKNOWN",
                status = record.status,
                syncStatus = record.syncStatus,
                remarks = record.remarks,
                lastModified = record.lastModified
            )
        }

        val backup = BackupData(
            version = 2,
            createdAt = System.currentTimeMillis(),
            subjectMappings = mappings,
            settings = AppSettingsBackup(
                autoSync = settingsRepo.autoSync.first(),
                attendanceAlerts = settingsRepo.attendanceAlerts.first(),
                milestoneAlerts = settingsRepo.milestoneAlerts.first(),
                mismatchAlerts = settingsRepo.mismatchAlerts.first(),
                theme = settingsRepo.theme.first(),
                username = settingsRepo.getUsername(),
                dob = settingsRepo.getDob()
            ),
            portableTimetable = portableTimetable,
            portableAttendanceHistory = portableAttendance
        )
        return gson.toJson(backup)
    }

    suspend fun restoreFromJson(json: String): Result<Unit> {
        return try {
            val backup = gson.fromJson(json, BackupData::class.java)
            
            // Validation
            if (backup.version > 2) {
                return Result.failure(Exception("Unknown backup version: ${backup.version}"))
            }
            
            database.withTransaction {
                // Clear existing data
                attendanceDao.clearAllRecords()
                timetableDao.deleteAll()
                mappingDao.deleteAll()
                subjectDao.deleteAll()

                // 1. Restore Mappings
                mappingDao.insertAll(backup.subjectMappings)

                if (backup.version == 1) {
                    // Version 1 Import (ID dependent)
                    subjectDao.insertSubjects(backup.subjects ?: emptyList())
                    timetableDao.insertTimetableEntries(backup.timetable ?: emptyList())
                    backup.attendanceHistory?.forEach { attendanceDao.insertRecord(it) }
                } else {
                    // Version 2 Import (Portable)
                    val timetableLookup = mutableMapOf<String, Long>() // Key: "day_start_code"
                    val subjectCodeToId = mutableMapOf<String, Long>()

                    // A. Restore Subjects & Timetable
                    backup.portableTimetable?.forEach { pt ->
                        // Find or Create Subject
                        val subjectId = subjectCodeToId.getOrPut(pt.subjectCode) {
                            subjectDao.insertSubject(
                                SubjectEntity(
                                    subjectCode = pt.subjectCode,
                                    subjectName = pt.subjectName,
                                    facultyName = "",
                                    roomNumber = "",
                                    color = 0xFF2196F3.toInt(),
                                    isAttendanceSubject = true
                                )
                            )
                        }

                        val entryId = timetableDao.insertTimetableEntry(
                            TimetableEntryEntity(
                                dayOfWeek = pt.dayOfWeek,
                                startTime = pt.startTime,
                                endTime = pt.endTime,
                                subjectId = subjectId
                            )
                        )
                        
                        val key = "${pt.dayOfWeek}_${pt.startTime}_${pt.subjectCode}"
                        timetableLookup[key] = entryId
                    }

                    // B. Restore Attendance History
                    backup.portableAttendanceHistory?.forEach { pa ->
                        val key = "${pa.dayOfWeek}_${pa.startTime}_${pa.subjectCode}"
                        val timetableEntryId = timetableLookup[key]
                        
                        if (timetableEntryId != null) {
                            attendanceDao.insertRecord(
                                AttendanceRecordEntity(
                                    date = pa.date,
                                    timetableEntryId = timetableEntryId,
                                    status = pa.status,
                                    syncStatus = pa.syncStatus,
                                    remarks = pa.remarks,
                                    lastModified = pa.lastModified
                                )
                            )
                        }
                    }
                }

                // Restore settings
                settingsRepo.saveCredentials(backup.settings.username, "", backup.settings.dob)
                settingsRepo.setAutoSync(backup.settings.autoSync)
                settingsRepo.setAttendanceAlerts(backup.settings.attendanceAlerts)
                settingsRepo.setMilestoneAlerts(backup.settings.milestoneAlerts)
                settingsRepo.setMismatchAlerts(backup.settings.mismatchAlerts)
                settingsRepo.setTheme(backup.settings.theme)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
