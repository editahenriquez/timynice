package com.example.timynice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timynice.room.ActivityEntity
import com.example.timynice.room.AppDatabase
import com.example.timynice.room.DayEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class DateViewModel(private val database: AppDatabase, val date: String) : ViewModel() {

    private val _dayMessage = MutableStateFlow("Today Do your best!")
    val dayMessage: StateFlow<String> = _dayMessage

    private val _activities = MutableStateFlow<List<ActivityEntity>>(emptyList())
    val activities: StateFlow<List<ActivityEntity>> = _activities

    private val _dateAccomplish = MutableStateFlow(0f)
    val dateAccomplish: StateFlow<Float> = _dateAccomplish

    init {
        loadDateData()

        viewModelScope.launch {
            activities.collect { updatedActivities ->
                calculateAccomplishment(updatedActivities)
            }
        }
    }

    fun loadDateData() {
        viewModelScope.launch {
            val day = database.dayDao().getDay(date)
            _dayMessage.value = day?.message ?: "Today Do your best!"

            val existingActivities = database.activityDao().getActivitiesForDay(date)
            if (existingActivities.isNotEmpty()) {
                _activities.value = existingActivities
            } else {
                // Auto-fill from most recent same weekday
                val weekday = LocalDate.parse(date).dayOfWeek.value % 7  // 0=Sunday
                val weekdayStr = weekday.toString()  // Convert to string since SQLite returns '0'-'6'

                val recentActivities = database.activityDao()
                    .getRecentActivitiesByWeekday(date, weekdayStr)
                    .groupBy { it.dayId }
                    .values
                    .firstOrNull()  // Most recent group of same-day activities

                if (recentActivities != null) {
                    val copied = recentActivities.map {
                        it.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            dayId = date,
                            checked = false
                        )
                    }
                    copied.forEach { database.activityDao().insertActivity(it) }
                    _activities.value = copied
                } else {
                    _activities.value = emptyList()
                }
            }

            updateAccomplishment()
        }
    }

    private fun updateAccomplishment() {
        val acts = _activities.value
        val total = acts.size
        val checked = acts.count { it.checked }
        val acc = if (total == 0) 0f else (checked.toFloat() / total) * 100f
        _dateAccomplish.value = acc

        // Also store in DB
        viewModelScope.launch {
            val currentMsg = _dayMessage.value
            database.dayDao().insertDay(DayEntity(date, currentMsg, acc))  // 🆕
        }
    }

    private fun calculateAccomplishment(activities: List<ActivityEntity>) {
        val total = activities.size
        val checked = activities.count { it.checked }
        _dateAccomplish.value = if (total == 0) 0f else (checked.toFloat() / total) * 100f
    }

    fun updateDayMessage(newMessage: String) {
        _dayMessage.value = newMessage
        viewModelScope.launch {
            val accomplishment = _dateAccomplish.value
            database.dayDao().insertDay(DayEntity(date, newMessage, accomplishment))
        }
    }


    fun insertOrUpdateActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            database.activityDao().insertActivity(activity)

            // Update local copy immediately
            val updatedList = _activities.value.toMutableList()
            val index = updatedList.indexOfFirst { it.id == activity.id }

            if (index != -1) {
                updatedList[index] = activity
            } else {
                updatedList.add(activity)
            }

            _activities.value = updatedList
            updateAccomplishment()
        }
    }

    fun deleteActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            database.activityDao().deleteActivity(activity)
            //_activities.value = _activities.value.filter { it.id != activity.id }
            _activities.value = database.activityDao().getActivitiesForDay(date)
            updateAccomplishment()
        }
    }

    fun resetActivities() {
        viewModelScope.launch {
            database.activityDao().resetActivitiesForDay(date)
            _activities.value = database.activityDao().getActivitiesForDay(date)
            updateAccomplishment()
        }
    }

}