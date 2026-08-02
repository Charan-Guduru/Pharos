package com.vnrvjiet.attendancemonitor.data.local.dao

import androidx.room.*
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY date DESC, lastModified DESC")
    fun getAllRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getRecordsForDate(date: Long): Flow<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity): Long

    @Update
    suspend fun updateRecord(record: AttendanceRecordEntity)

    @Query("SELECT * FROM attendance_records WHERE date = :date AND timetableEntryId = :entryId LIMIT 1")
    suspend fun getRecordForEntryAndDate(entryId: Long, date: Long): AttendanceRecordEntity?

    @Query("DELETE FROM attendance_records")
    suspend fun clearAllRecords()
}
