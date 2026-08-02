package com.vnrvjiet.attendancemonitor.data.repository

import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectMappingDao
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectMappingEntity
import kotlinx.coroutines.flow.Flow

class SubjectMappingRepository(private val dao: SubjectMappingDao) {
    val allMappings: Flow<List<SubjectMappingEntity>> = dao.getAllMappings()

    suspend fun saveMappings(mappings: List<SubjectMappingEntity>) {
        dao.insertAll(mappings)
    }

    suspend fun getMapping(code: String): SubjectMappingEntity? {
        return dao.getBySubjectCode(code)
    }
}
