package com.vnrvjiet.attendancemonitor.data.local.dao

import androidx.room.*
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EduPrimeAttendanceDao {
    @Query("SELECT * FROM eduprime_attendance")
    fun getAllAttendance(): Flow<List<EduPrimeAttendanceEntity>>

    @Query("SELECT * FROM eduprime_attendance")
    suspend fun getAllAttendanceList(): List<EduPrimeAttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<EduPrimeAttendanceEntity>)

    @Query("DELETE FROM eduprime_attendance")
    suspend fun deleteAll()

    @Transaction
    suspend fun syncAttendance(records: List<EduPrimeAttendanceEntity>) {
        deleteAll()
        insertAll(records)
    }
}
