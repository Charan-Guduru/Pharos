package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectDao
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

class RoomSubjectRepository(private val subjectDao: SubjectDao) : SubjectRepository {
    override fun getAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()

    override suspend fun insertSubject(subject: SubjectEntity) = subjectDao.insertSubject(subject)
    override suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)
    override suspend fun deleteSubject(subject: SubjectEntity) = subjectDao.deleteSubject(subject)
}
