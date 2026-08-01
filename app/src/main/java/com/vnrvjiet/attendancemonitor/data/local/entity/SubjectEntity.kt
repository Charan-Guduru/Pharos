package com.vnrvjiet.attendancemonitor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectCode: String,
    val subjectName: String,
    val facultyName: String,
    val roomNumber: String,
    val color: Int,
    val isAttendanceSubject: Boolean
)
