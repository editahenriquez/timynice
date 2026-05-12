package com.example.timynice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timynice.room.ActivityEntity
import com.example.timynice.room.AppDatabase
import com.example.timynice.room.DayEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID

class DateViewModel(private val database: AppDatabase, val date: String) : ViewModel() {

    private val _dayMessage = MutableStateFlow("Today Do your best!")
    val dayMessage: StateFlow<String> = _dayMessage

    private val _activities = MutableStateFlow<List<ActivityEntity>>(emptyList())
    val activities: StateFlow<List<ActivityEntity>> = _activities

    private val _dateAccomplish = MutableStateFlow(0f)
    val dateAccomplish: StateFlow<Float> = _dateAccomplish

    /** Serializes activity reads/writes so delete-all cannot race with in-flight inserts. */
    private val activityMutationMutex = Mutex()

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
            activityMutationMutex.withLock {
                val day = database.dayDao().getDay(date)
                _dayMessage.value = day?.message ?: "Today Do your best!"

                val existingActivities = database.activityDao().getActivitiesForDay(date)
                if (existingActivities.isNotEmpty()) {
                    val normalized = normalizeActivitiesContinuity(existingActivities)
                    if (normalized != existingActivities) {
                        normalized.forEach { database.activityDao().insertActivity(it) }
                    }
                    _activities.value = normalized
                } else {
                    val weekday = LocalDate.parse(date).dayOfWeek.value % 7
                    val weekdayStr = weekday.toString()

                    val recentActivities = database.activityDao()
                        .getRecentActivitiesByWeekday(date, weekdayStr)
                        .groupBy { it.dayId }
                        .toSortedMap(compareByDescending { it })
                        .values
                        .firstOrNull()

                    if (recentActivities != null) {
                        val copied = recentActivities.map {
                            it.copy(
                                id = UUID.randomUUID().toString(),
                                dayId = date,
                                checked = false,
                            )
                        }
                        val normalized = normalizeActivitiesContinuity(copied)
                        normalized.forEach { database.activityDao().insertActivity(it) }
                        _activities.value = database.activityDao().getActivitiesForDay(date)
                    } else {
                        _activities.value = emptyList()
                    }
                }

                updateAccomplishment()
            }
        }
    }

    fun updateAccomplishment() {
        val acts = _activities.value
        val total = acts.size
        val checked = acts.count { it.checked }
        val acc = if (total == 0) 0f else (checked.toFloat() / total) * 100f
        _dateAccomplish.value = acc

        viewModelScope.launch {
            val currentMsg = _dayMessage.value
            database.dayDao().insertDay(DayEntity(date, currentMsg, acc))
        }
    }

    fun calculateAccomplishment(activities: List<ActivityEntity>) {
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
            activityMutationMutex.withLock {
                if (activity.dayId != date) return@withLock

                var working = _activities.value.toMutableList()
                var index = working.indexOfFirst { it.id == activity.id }
                if (index == -1) {
                    val fromDb = database.activityDao().getActivitiesForDay(date)
                    if (fromDb.none { it.id == activity.id }) {
                        // Typical case: name field blur after delete-all / row removed — do not re-insert.
                        return@withLock
                    }
                    working = fromDb.toMutableList()
                    index = working.indexOfFirst { it.id == activity.id }
                    if (index == -1) return@withLock
                }
                working[index] = activity

                val normalized = normalizeActivitiesContinuity(working)
                normalized.forEach { database.activityDao().insertActivity(it) }
                _activities.value = database.activityDao().getActivitiesForDay(date)
                updateAccomplishment()
            }
        }
    }

    fun deleteActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            activityMutationMutex.withLock {
                database.activityDao().deleteActivity(activity)
                _activities.value = database.activityDao().getActivitiesForDay(date)
                updateAccomplishment()
            }
        }
    }

    /**
     * Deletes every activity for this day. Call from a coroutine and await before dismissing UI
     * so no concurrent insert replays stale rows.
     */
    suspend fun resetAllActivitiesExclusive() {
        activityMutationMutex.withLock {
            database.activityDao().resetActivitiesForDay(date)
            _activities.value = database.activityDao().getActivitiesForDay(date)
            updateAccomplishment()
        }
    }

    /**
     * Appends one empty row at the end of the timeline (after normalization), persisted to DB.
     * @return id of the new row (for focus).
     */
    suspend fun appendEmptyActivity(): String {
        return activityMutationMutex.withLock {
            val fromDb = database.activityDao().getActivitiesForDay(date)
            val chain = normalizeActivitiesContinuity(fromDb)
            val newStart = chain.lastOrNull()?.end ?: "00:00"
            val newId = UUID.randomUUID().toString()
            val tail = ActivityEntity(
                id = newId,
                dayId = date,
                name = "",
                duration = "00:00",
                start = newStart,
                end = newStart,
                checked = false,
            )
            val merged = chain + tail
            val normalized = normalizeActivitiesContinuity(merged)
            normalized.forEach { database.activityDao().insertActivity(it) }
            _activities.value = database.activityDao().getActivitiesForDay(date)
            updateAccomplishment()
            newId
        }
    }

    suspend fun fetchActivitiesForDay(dayId: String): List<ActivityEntity> =
        database.activityDao().getActivitiesForDay(dayId)

    fun applyDuplicateFromSourceDate(sourceDateIso: String) {
        viewModelScope.launch {
            activityMutationMutex.withLock {
                val source = database.activityDao().getActivitiesForDay(sourceDateIso)
                if (source.isEmpty()) return@withLock

                database.activityDao().resetActivitiesForDay(date)
                val copies = source.map { src ->
                    ActivityEntity(
                        id = UUID.randomUUID().toString(),
                        dayId = date,
                        name = src.name,
                        duration = src.duration,
                        start = src.start,
                        end = src.end,
                        checked = false,
                    )
                }
                val normalized = normalizeActivitiesContinuity(copies)
                normalized.forEach { database.activityDao().insertActivity(it) }
                _activities.value = database.activityDao().getActivitiesForDay(date)
                updateAccomplishment()
            }
        }
    }
}
