package com.vnrvjiet.attendancemonitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.vnrvjiet.attendancemonitor.data.local.dao.AttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.dao.AttendanceSnapshotDao
import com.vnrvjiet.attendancemonitor.data.local.dao.EduPrimeAttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.dao.NotificationDao
import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectDao
import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectMappingDao
import com.vnrvjiet.attendancemonitor.data.local.dao.TimetableDao
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceSnapshotEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.EduPrimeAttendanceEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.NotificationEntity
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
        SubjectMappingEntity::class,
        NotificationEntity::class,
        AttendanceSnapshotEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun eduPrimeAttendanceDao(): EduPrimeAttendanceDao
    abstract fun subjectMappingDao(): SubjectMappingDao
    abstract fun notificationDao(): NotificationDao
    abstract fun attendanceSnapshotDao(): AttendanceSnapshotDao

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
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `attendance_snapshots` (" +
                            "`subjectCode` TEXT NOT NULL, " +
                            "`conductedClasses` INTEGER NOT NULL, " +
                            "`attendedClasses` INTEGER NOT NULL, " +
                            "`lastUpdated` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`subjectCode`))"
                )
            }
        }
        
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `attendance_records` ADD COLUMN `verificationMessage` TEXT")
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
