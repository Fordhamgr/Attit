package com.example.attit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.attit.model.AttendanceLog
import com.example.attit.model.Friend
import com.example.attit.model.Subject

@Database(entities = [Subject::class, AttendanceLog::class, Friend::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun attendanceLogDao(): AttendanceLogDao
    abstract fun friendDao(): FriendDao // <--- NEW

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attit_database"
                )
                    // START FRESH: Delete old database if version changes (Prevents crashes during dev)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}