package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAttendanceRepository : AttendanceRepository {
    private val records = listOf(
        AttendanceRecordEntity(1, System.currentTimeMillis(), 1, AttendanceStatus.PRESENT, SyncStatus.SYNCED, null, System.currentTimeMillis()),
        AttendanceRecordEntity(2, System.currentTimeMillis(), 2, AttendanceStatus.PRESENT, SyncStatus.PENDING, null, System.currentTimeMillis()),
        AttendanceRecordEntity(3, System.currentTimeMillis() - 86400000, 5, AttendanceStatus.ABSENT, SyncStatus.MISMATCH, "EduPrime says Present", System.currentTimeMillis())
    )

    override fun getAllRecords(): Flow<List<AttendanceRecordEntity>> = flowOf(records)

    override fun getRecordsForDate(date: Long): Flow<List<AttendanceRecordEntity>> = flowOf(records)

    override suspend fun insertRecord(record: AttendanceRecordEntity) {}
    override suspend fun updateRecord(record: AttendanceRecordEntity) {}
    override suspend fun clearAllRecords() {}
}
