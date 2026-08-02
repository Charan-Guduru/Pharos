package com.vnrvjiet.attendancemonitor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subject_mappings")
data class SubjectMappingEntity(
    @PrimaryKey
    val subjectCode: String,
    val subjectName: String,
    val createdAt: Long,
    val updatedAt: Long
)
