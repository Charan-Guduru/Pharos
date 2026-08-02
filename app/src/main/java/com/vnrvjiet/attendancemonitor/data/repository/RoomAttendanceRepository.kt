package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.AttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

class RoomAttendanceRepository(private val attendanceDao: AttendanceDao) : AttendanceRepository {
    override fun getAllRecords(): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getAllRecords()
    }

    override fun getRecordsForDate(date: Long): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getRecordsForDate(date)
    }

    suspend fun getRecordForEntryAndDate(entryId: Long, date: Long): AttendanceRecordEntity? {
        return attendanceDao.getRecordForEntryAndDate(entryId, date)
    }

    override suspend fun insertRecord(record: AttendanceRecordEntity): Long {
        return attendanceDao.insertRecord(record)
    }

    override suspend fun updateRecord(record: AttendanceRecordEntity) {
        attendanceDao.updateRecord(record)
    }

    override suspend fun clearAllRecords() {
        attendanceDao.clearAllRecords()
    }
}
