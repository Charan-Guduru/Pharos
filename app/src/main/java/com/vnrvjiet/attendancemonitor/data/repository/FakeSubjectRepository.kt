package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeSubjectRepository : SubjectRepository {
    private val subjects = listOf(
        SubjectEntity(1, "CS301", "Compiler Design", "Dr. K. Srinivas", "B-402", 0xFF0D47A1.toInt(), true),
        SubjectEntity(2, "CS302", "Web Development", "Dr. Rao", "Room 302", 0xFF2196F3.toInt(), true),
        SubjectEntity(3, "CS303", "DBMS", "Mrs. P. Radhika", "Room 101", 0xFFD7CCC8.toInt(), true),
        SubjectEntity(4, "CS304", "Operating Systems", "Dr. A. Kumar", "Room 205", 0xFF9E9E9E.toInt(), true),
        SubjectEntity(5, "LIB", "Library", "-", "Central Library", 0xFFE0E0E0.toInt(), false)
    )

    override fun getAllSubjects(): Flow<List<SubjectEntity>> = flowOf(subjects)

    override suspend fun insertSubject(subject: SubjectEntity) {}
    override suspend fun updateSubject(subject: SubjectEntity) {}
    override suspend fun deleteSubject(subject: SubjectEntity) {}
}
