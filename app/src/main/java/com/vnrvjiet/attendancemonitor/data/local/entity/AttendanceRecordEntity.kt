package com.vnrvjiet.attendancemonitor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.SyncStatus

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = TimetableEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["timetableEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["timetableEntryId"])]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long, // Epoch millis
    val timetableEntryId: Long,
    val status: AttendanceStatus,
    val syncStatus: SyncStatus,
    val remarks: String?,
    val lastModified: Long
)
