package com.example.timynice.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room database for timynice
@Database(entities = [DayEntity::class, ActivityEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // DAO accessors
    abstract fun dayDao(): DayDao
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton database instance
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timynice_db"
                )
                    .fallbackToDestructiveMigration(false)  // allows quick dev testing
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}