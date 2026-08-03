package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectDao
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

class RoomSubjectRepository(private val subjectDao: SubjectDao) {
    fun getAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()

    suspend fun insertSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(subject: SubjectEntity) = subjectDao.deleteSubject(subject)
}
