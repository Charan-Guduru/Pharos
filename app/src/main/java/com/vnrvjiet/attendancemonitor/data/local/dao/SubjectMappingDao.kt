package com.vnrvjiet.attendancemonitor.data.local.dao

import androidx.room.*
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectMappingDao {
    @Query("SELECT * FROM subject_mappings")
    fun getAllMappings(): Flow<List<SubjectMappingEntity>>

    @Query("SELECT * FROM subject_mappings WHERE subjectCode = :code LIMIT 1")
    suspend fun getBySubjectCode(code: String): SubjectMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<SubjectMappingEntity>)

    @Update
    suspend fun update(mapping: SubjectMappingEntity)

    @Delete
    suspend fun delete(mapping: SubjectMappingEntity)

    @Query("DELETE FROM subject_mappings")
    suspend fun deleteAll()
}
