package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTimetableRepository : TimetableRepository {
    private val timetable = listOf(
        TimetableEntryEntity(1, 1, "09:30 AM", "10:30 AM", 2), // Web Dev
        TimetableEntryEntity(2, 1, "10:30 AM", "11:30 AM", 1), // Compiler Design
        TimetableEntryEntity(3, 1, "11:30 AM", "12:30 PM", 3), // DBMS
        TimetableEntryEntity(4, 1, "01:30 PM", "02:30 PM", 5), // Library
        TimetableEntryEntity(5, 1, "02:30 PM", "04:00 PM", 4)  // OS
    )

    override fun getTimetableForDay(day: Int): Flow<List<TimetableEntryEntity>> = flowOf(timetable)

    override fun getAllTimetableEntries(): Flow<List<TimetableEntryEntity>> = flowOf(timetable)

    override suspend fun insertTimetableEntry(entry: TimetableEntryEntity) {}
    override suspend fun deleteTimetableEntry(entry: TimetableEntryEntity) {}
}
