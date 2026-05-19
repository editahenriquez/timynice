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
                    // New day with no saved activities: leave empty (user adds rows or duplicates).
                    _activities.value = emptyList()
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

                persistNormalizedActivities(working)
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
            val chain = normalizeActivitiesContinuity(_activities.value)
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
            persistNormalizedActivities(chain + tail)
            newId
        }
    }

    /**
     * Applies a user-defined row order from drag-and-drop, then recalculates the gapless timeline.
     * Order is persisted only via updated start/end times (no extra DB columns).
     */
    suspend fun applyActivityOrder(ordered: List<ActivityEntity>) {
        if (ordered.isEmpty()) return
        activityMutationMutex.withLock {
            val byId = _activities.value.associateBy { it.id }
            val merged = ordered.map { byId[it.id] ?: it }
            persistNormalizedActivities(merged)
        }
    }

    private suspend fun persistNormalizedActivities(ordered: List<ActivityEntity>) {
        val normalized = normalizeActivitiesContinuity(ordered)
        normalized.forEach { database.activityDao().insertActivity(it) }
        _activities.value = normalized
        updateAccomplishment()
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
                persistNormalizedActivities(copies)
            }
        }
    }
}
