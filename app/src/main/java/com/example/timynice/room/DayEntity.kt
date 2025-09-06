package com.example.timynice.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey val date: String,  // Date in yyyy-MM-dd format
    val message: String = "Today Do your best!",
    val accomplishment: Float = 0f
)