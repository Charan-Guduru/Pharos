package com.vnrvjiet.attendancemonitor.data.local.dao

import androidx.room.*
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable WHERE dayOfWeek = :day")
    fun getTimetableForDay(day: Int): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable")
    fun getAllTimetableEntries(): Flow<List<TimetableEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableEntries(entries: List<TimetableEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableEntry(entry: TimetableEntryEntity): Long

    @Update
    suspend fun updateTimetableEntry(entry: TimetableEntryEntity)

    @Delete
    suspend fun deleteTimetableEntry(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable")
    suspend fun deleteAll()
}
