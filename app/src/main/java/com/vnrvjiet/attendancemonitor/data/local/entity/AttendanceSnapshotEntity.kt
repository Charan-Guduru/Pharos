package com.vnrvjiet.attendancemonitor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_snapshots")
data class AttendanceSnapshotEntity(
    @PrimaryKey
    val subjectCode: String,
    val conductedClasses: Int,
    val attendedClasses: Int,
    val lastUpdated: Long
)
