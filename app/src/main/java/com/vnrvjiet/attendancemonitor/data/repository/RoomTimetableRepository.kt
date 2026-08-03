package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.TimetableDao
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

class RoomTimetableRepository(private val timetableDao: TimetableDao) {
    fun getTimetableForDay(day: Int): Flow<List<TimetableEntryEntity>> = timetableDao.getTimetableForDay(day)

    fun getAllTimetableEntries(): Flow<List<TimetableEntryEntity>> = timetableDao.getAllTimetableEntries()

    suspend fun insertTimetableEntry(entry: TimetableEntryEntity): Long = timetableDao.insertTimetableEntry(entry)
    suspend fun updateTimetableEntry(entry: TimetableEntryEntity) = timetableDao.updateTimetableEntry(entry)
    suspend fun deleteTimetableEntry(entry: TimetableEntryEntity) = timetableDao.deleteTimetableEntry(entry)
}
