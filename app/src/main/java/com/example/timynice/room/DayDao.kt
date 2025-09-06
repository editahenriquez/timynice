package com.example.timynice.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DayDao {
    // Insert or update a day
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: DayEntity)

    // Get a day by date
    @Query("SELECT * FROM days WHERE date = :date LIMIT 1")
    suspend fun getDay(date: String): DayEntity?

    // Get all days in a month (e.g., "2025-09")
    @Query("SELECT date FROM days WHERE date LIKE :yearMonth || '%'")
    suspend fun getDaysInMonth(yearMonth: String): List<String>

    // Get all DayEntity objects in a month (e.g., all data for "2025-09")
    @Query("SELECT * FROM days WHERE date LIKE :yearMonth || '%'")
    suspend fun getDayEntitiesInMonth(yearMonth: String): List<DayEntity>
}