package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.TimetableDao
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

class RoomTimetableRepository(private val timetableDao: TimetableDao) : TimetableRepository {
    override fun getTimetableForDay(day: Int): Flow<List<TimetableEntryEntity>> = timetableDao.getTimetableForDay(day)

    override fun getAllTimetableEntries(): Flow<List<TimetableEntryEntity>> = timetableDao.getAllTimetableEntries()

    override suspend fun insertTimetableEntry(entry: TimetableEntryEntity) = timetableDao.insertTimetableEntry(entry)
    override suspend fun deleteTimetableEntry(entry: TimetableEntryEntity) = timetableDao.deleteTimetableEntry(entry)
}
