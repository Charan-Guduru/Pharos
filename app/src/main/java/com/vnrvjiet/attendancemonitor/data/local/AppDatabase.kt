package com.vnrvjiet.attendancemonitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vnrvjiet.attendancemonitor.data.local.dao.AttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.dao.EduPrimeAttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectDao
import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectMappingDao
import com.vnrvjiet.attendancemonitor.data.local.dao.TimetableDao
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectMappingEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        TimetableEntryEntity::class,
        AttendanceRecordEntity::class,
        EduPrimeAttendanceEntity::class,
        SubjectMappingEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun eduPrimeAttendanceDao(): EduPrimeAttendanceDao
    abstract fun subjectMappingDao(): SubjectMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // No prepopulation for production/user-configurable timetable
            }
        }
    }
}
