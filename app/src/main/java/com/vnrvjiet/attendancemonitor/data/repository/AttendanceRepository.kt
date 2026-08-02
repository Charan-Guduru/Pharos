package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    fun getAllRecords(): Flow<List<AttendanceRecordEntity>>
    fun getRecordsForDate(date: Long): Flow<List<AttendanceRecordEntity>>
    suspend fun insertRecord(record: AttendanceRecordEntity): Long
    suspend fun updateRecord(record: AttendanceRecordEntity)
    suspend fun clearAllRecords()
}
