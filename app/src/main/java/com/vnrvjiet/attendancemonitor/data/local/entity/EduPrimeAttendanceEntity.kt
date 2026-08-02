package com.vnrvjiet.attendancemonitor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eduprime_attendance")
data class EduPrimeAttendanceEntity(
    @PrimaryKey
    val subjectCode: String,
    val subjectName: String?,
    val conductedClasses: Int,
    val attendedClasses: Int,
    val attendancePercentage: Double,
    val lastSyncedAt: Long
)
