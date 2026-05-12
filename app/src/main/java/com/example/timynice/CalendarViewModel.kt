package com.example.timynice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timynice.room.ActivityEntity
import com.example.timynice.room.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class ActivityConsistencyKpi(
    val displayName: String,
    /** Distinct days in the month where at least one row of this activity is checked (completed). */
    val distinctCheckedDays: Int,
    /** Denominator: full month (past), or 1…today (current month); ≥1. */
    val denominatorDays: Int,
    /** distinctCheckedDays / denominatorDays × 100 */
    val consistencyPercent: Float,
)

class CalendarViewModel(private val database: AppDatabase) : ViewModel() {
    private val _calendarState = MutableStateFlow(CalendarState())
    val calendarState: StateFlow<CalendarState> = _calendarState

    init {
        // Load current month's data
        loadMonth(YearMonth.now())
    }

    fun loadMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
            val yearMonthStr = yearMonth.format(formatter)

            // Load day entities instead of just date strings
            val dayEntities = database.dayDao().getDayEntitiesInMonth(yearMonthStr)

            val days = dayEntities.map { it.date }

            val dayAccomplishments = dayEntities.associate { it.date to it.accomplishment }

            val totalActivities = dayEntities.size
            val totalChecked = dayEntities.sumOf { it.accomplishment.toInt() }

            val calAccomplish = if (totalActivities == 0) 0f else (totalChecked.toFloat() / totalActivities)

            val monthActivities = database.activityDao().getActivitiesForMonth(yearMonthStr)
            val denominatorDays = eligibleDaysForConsistencyDenominator(yearMonth, LocalDate.now())
            val (daysWithAnyActivity, consistencyRows) = buildActivityConsistencyKpis(
                monthActivities,
                denominatorDays,
            )

            _calendarState.value = CalendarState(
                yearMonth = yearMonth,
                days = days,
                dayAccomplishments = dayAccomplishments,
                calAccomplish = calAccomplish,
                daysWithAnyActivityInMonth = daysWithAnyActivity,
                consistencyDenominatorDays = denominatorDays,
                activityConsistency = consistencyRows,
            )
        }
    }

    companion object {
        /**
         * Past months: full calendar length.
         * Current month: days from the 1st through today (inclusive).
         * Future months: 1 (avoids div-by-zero; rarely used).
         */
        fun eligibleDaysForConsistencyDenominator(yearMonth: YearMonth, today: LocalDate): Int {
            val len = yearMonth.lengthOfMonth()
            val ymToday = YearMonth.from(today)
            val denom = when {
                yearMonth.isAfter(ymToday) -> 1
                yearMonth.isBefore(ymToday) -> len
                else -> today.dayOfMonth.coerceIn(1, len)
            }
            return denom.coerceAtLeast(1)
        }

        private fun buildActivityConsistencyKpis(
            activities: List<ActivityEntity>,
            denominatorDays: Int,
        ): Pair<Int, List<ActivityConsistencyKpi>> {
            if (activities.isEmpty()) return 0 to emptyList()
            val daysWithAnyActivity = activities.map { it.dayId }.distinct().size
            val checkedDayIdsByNameKey = linkedMapOf<String, MutableSet<String>>()
            val labelByKey = linkedMapOf<String, String>()
            for (a in activities) {
                val raw = a.name.trim()
                val key = raw.lowercase()
                if (key.isEmpty()) continue
                if (!labelByKey.containsKey(key)) labelByKey[key] = raw
                if (a.checked) {
                    checkedDayIdsByNameKey.getOrPut(key) { mutableSetOf() }.add(a.dayId)
                }
            }
            val rows = labelByKey.keys.map { key ->
                val checkedDays = checkedDayIdsByNameKey[key]?.size ?: 0
                val pct = checkedDays * 100f / denominatorDays.toFloat().coerceAtLeast(1f)
                ActivityConsistencyKpi(
                    displayName = labelByKey[key] ?: key,
                    distinctCheckedDays = checkedDays,
                    denominatorDays = denominatorDays,
                    consistencyPercent = pct,
                )
            }.sortedWith(
                compareByDescending<ActivityConsistencyKpi> { it.consistencyPercent }
                    .thenByDescending { it.distinctCheckedDays }
                    .thenBy { it.displayName.lowercase() },
            )
            return daysWithAnyActivity to rows
        }
    }
}

data class CalendarState constructor(
    val yearMonth: YearMonth = YearMonth.now(),
    val days: List<String> = emptyList(),
    val dayAccomplishments: Map<String, Float> = emptyMap(),
    val calAccomplish: Float = 0f,
    /** Distinct days in the month with ≥1 activity row (any name). */
    val daysWithAnyActivityInMonth: Int = 0,
    /** Same denominator used for consistency % (see [CalendarViewModel.eligibleDaysForConsistencyDenominator]). */
    val consistencyDenominatorDays: Int = 31,
    val activityConsistency: List<ActivityConsistencyKpi> = emptyList(),
)