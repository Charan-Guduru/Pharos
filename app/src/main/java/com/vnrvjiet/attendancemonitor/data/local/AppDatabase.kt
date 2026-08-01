package com.vnrvjiet.attendancemonitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vnrvjiet.attendancemonitor.data.local.dao.AttendanceDao
import com.vnrvjiet.attendancemonitor.data.local.dao.SubjectDao
import com.vnrvjiet.attendancemonitor.data.local.dao.TimetableDao
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        TimetableEntryEntity::class,
        AttendanceRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun attendanceDao(): AttendanceDao

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
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        prepopulateDatabase(database)
                    }
                }
            }

            suspend fun prepopulateDatabase(db: AppDatabase) {
                val subjects = listOf(
                    SubjectEntity(1, "CS301", "Compiler Design", "Dr. K. Srinivas", "B-402", 0xFF0D47A1.toInt(), true),
                    SubjectEntity(2, "CS302", "Web Development", "Dr. Rao", "Room 302", 0xFF2196F3.toInt(), true),
                    SubjectEntity(3, "CS303", "DBMS", "Mrs. P. Radhika", "Room 101", 0xFFD7CCC8.toInt(), true),
                    SubjectEntity(4, "CS304", "Operating Systems", "Dr. A. Kumar", "Room 205", 0xFF9E9E9E.toInt(), true),
                    SubjectEntity(5, "LIB", "Library", "-", "Central Library", 0xFFE0E0E0.toInt(), false)
                )
                db.subjectDao().insertSubjects(subjects)

                val timetable = listOf(
                    TimetableEntryEntity(1, 1, "09:30 AM", "10:30 AM", 2),
                    TimetableEntryEntity(2, 1, "10:30 AM", "11:30 AM", 1),
                    TimetableEntryEntity(3, 1, "11:30 AM", "12:30 PM", 3),
                    TimetableEntryEntity(4, 1, "01:30 PM", "02:30 PM", 5),
                    TimetableEntryEntity(5, 1, "02:30 PM", "04:00 PM", 4)
                )
                db.timetableDao().insertTimetableEntries(timetable)
            }
        }
    }
}
