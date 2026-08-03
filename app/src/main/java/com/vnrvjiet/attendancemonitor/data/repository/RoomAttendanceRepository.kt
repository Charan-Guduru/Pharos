package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.AttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

class RoomAttendanceRepository(private val attendanceDao: AttendanceDao) {
    fun getAllRecords(): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getAllRecords()
    }

    fun getRecordsForDate(date: Long): Flow<List<AttendanceRecordEntity>> {
        return attendanceDao.getRecordsForDate(date)
    }

    suspend fun getRecordForEntryAndDate(entryId: Long, date: Long): AttendanceRecordEntity? {
        return attendanceDao.getRecordForEntryAndDate(entryId, date)
    }

    suspend fun insertRecord(record: AttendanceRecordEntity): Long {
        return attendanceDao.insertRecord(record)
    }

    suspend fun updateRecord(record: AttendanceRecordEntity) {
        attendanceDao.updateRecord(record)
    }

    suspend fun clearAllRecords() {
        attendanceDao.clearAllRecords()
    }
}
