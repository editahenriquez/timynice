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
    @Query("SELECT * FROM activities WHERE dayId = :dayId ORDER BY start ASC, id ASC")
    suspend fun getActivitiesForDay(dayId: String): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE substr(dayId, 1, 7) = :yearMonth ORDER BY dayId ASC, start ASC, id ASC")
    suspend fun getActivitiesForMonth(yearMonth: String): List<ActivityEntity>

    // Delete an activity
    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)

    // Reset all activities for a day
    @Query("DELETE FROM activities WHERE dayId = :dayId")
    suspend fun resetActivitiesForDay(dayId: String)

    // Get recent activities for a given weekday before currentDate, ordered by most recent date.
    /*@Query("""
    SELECT * FROM activities
    WHERE strftime('%w', dayId) = :weekday 
    AND dayId < :currentDate
    order by dayId desc,name asc
    """)*/
    @Query("""
    SELECT * FROM activities
    WHERE dayId = (
        SELECT MAX(dayId) FROM activities
        WHERE strftime('%w', dayId) = :weekday
          AND dayId < :currentDate
    )
    ORDER BY start ASC, id ASC
    """)
    suspend fun getRecentActivitiesByWeekday(currentDate: String, weekday: String): List<ActivityEntity>
    //ORDER BY start asc
}