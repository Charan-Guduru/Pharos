package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

interface TimetableRepository {
    fun getTimetableForDay(day: Int): Flow<List<TimetableEntryEntity>>
    fun getAllTimetableEntries(): Flow<List<TimetableEntryEntity>>
    suspend fun insertTimetableEntry(entry: TimetableEntryEntity)
    suspend fun deleteTimetableEntry(entry: TimetableEntryEntity)
}
