package com.vnrvjiet.attendancemonitor.data.model

import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectMappingEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity

data class BackupData(
    val version: Int,
    val createdAt: Long,
    val subjectMappings: List<SubjectMappingEntity>,
    val settings: AppSettingsBackup,
    
    // Old format (v1)
    val subjects: List<SubjectEntity>? = null,
    val timetable: List<TimetableEntryEntity>? = null,
    val attendanceHistory: List<AttendanceRecordEntity>? = null,
    
    // New portable format (v2)
    val portableTimetable: List<PortableTimetableEntry>? = null,
    val portableAttendanceHistory: List<PortableAttendanceRecord>? = null
)

data class PortableTimetableEntry(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val subjectCode: String,
    val subjectName: String
)

data class PortableAttendanceRecord(
    val date: Long,
    val dayOfWeek: Int,
    val startTime: String,
    val subjectCode: String,
    val status: AttendanceStatus,
    val syncStatus: SyncStatus,
    val remarks: String?,
    val lastModified: Long
)

data class AppSettingsBackup(
    val autoSync: Boolean,
    val attendanceAlerts: Boolean,
    val milestoneAlerts: Boolean,
    val mismatchAlerts: Boolean,
    val theme: String,
    val username: String,
    val dob: String
)
