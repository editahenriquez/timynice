package com.example.timynice.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),  // Unique ID for activity
    val dayId: String,  // Links to DayEntity (date as foreign key)
    val name: String,   // Activity name (e.g., "Work", "Gym")
    val duration: String,  // Duration in hh:mm:ss
    val start: String,     // Start time in hh:mm:ss
    val end: String,       // End time in hh:mm:ss
    val checked: Boolean = false  // Completion status
)