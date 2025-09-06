package com.example.timynice.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// Data Access Object for ActivityEntity
@Dao
interface ActivityDao {
    // Insert or update an activity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    // Get activities for a specific day, ordered by start time
    @Query("SELECT * FROM activities WHERE dayId = :dayId ORDER BY start")
    suspend fun getActivitiesForDay(dayId: String): List<ActivityEntity>

    // Delete an activity
    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)

    // Reset all activities for a day
    @Query("DELETE FROM activities WHERE dayId = :dayId")
    suspend fun resetActivitiesForDay(dayId: String)

    // Get recent activities for a given weekday before currentDate, ordered by most recent date.
    @Query("""
    SELECT * FROM activities 
    WHERE strftime('%w', dayId) = :weekday 
    AND dayId < :currentDate 
    ORDER BY dayId DESC 
    """)
    suspend fun getRecentActivitiesByWeekday(currentDate: String, weekday: String): List<ActivityEntity>

    // Get activities of Monday before currentDate for initial fill.
    @Query("""
    SELECT * FROM activities 
    WHERE strftime('%w', dayId) = "1"
    AND dayId < :currentDate 
    ORDER BY dayId DESC
    """)
    suspend fun getMondayActivitiesForInitialLoad(currentDate: String, weekday: String): List<ActivityEntity>

}