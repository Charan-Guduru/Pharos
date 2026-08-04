package com.vnrvjiet.attendancemonitor.data.local.dao

import androidx.room.*
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceSnapshotDao {
    @Query("SELECT * FROM attendance_snapshots")
    fun getAllSnapshots(): Flow<List<AttendanceSnapshotEntity>>

    @Query("SELECT * FROM attendance_snapshots")
    suspend fun getAllSnapshotsList(): List<AttendanceSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<AttendanceSnapshotEntity>)

    @Query("DELETE FROM attendance_snapshots")
    suspend fun deleteAll()

    @Transaction
    suspend fun updateSnapshot(snapshots: List<AttendanceSnapshotEntity>) {
        deleteAll()
        insertAll(snapshots)
    }
}
