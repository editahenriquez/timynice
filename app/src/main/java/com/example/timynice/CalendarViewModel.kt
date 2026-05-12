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
    /** Distinct days in the window where at least one row of this activity is checked (completed). */
    val distinctCheckedDays: Int,
    /**
     * Distinct days in the month through today where this activity has any row (registered),
     * intersected with the same calendar window as the KPI (past month = full month).
     */
    val denominatorDays: Int,
    /** distinctCheckedDays / denominatorDays × 100; 0% if denominatorDays == 0. */
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
                yearMonth,
                LocalDate.now(),
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

        /** Inclusive range of dates that count for “month through today” KPIs. Empty if the month is entirely in the future. */
        private fun consistencyKpiDateWindow(yearMonth: YearMonth, today: LocalDate): ClosedRange<LocalDate>? {
            val start = yearMonth.atDay(1)
            val endOfMonth = yearMonth.atEndOfMonth()
            val end = minOf(endOfMonth, today)
            return if (end.isBefore(start)) null else start..end
        }

        private fun LocalDate.isInKpiWindow(window: ClosedRange<LocalDate>?): Boolean =
            window != null && this in window

        private fun buildActivityConsistencyKpis(
            activities: List<ActivityEntity>,
            yearMonth: YearMonth,
            today: LocalDate,
        ): Pair<Int, List<ActivityConsistencyKpi>> {
            if (activities.isEmpty()) return 0 to emptyList()
            val window = consistencyKpiDateWindow(yearMonth, today)
            val daysWithAnyActivity = activities
                .mapNotNull { runCatching { LocalDate.parse(it.dayId) }.getOrNull() }
                .filter { it.isInKpiWindow(window) }
                .distinct()
                .size
            val registeredDayIdsByNameKey = linkedMapOf<String, MutableSet<String>>()
            val checkedDayIdsByNameKey = linkedMapOf<String, MutableSet<String>>()
            val labelByKey = linkedMapOf<String, String>()
            for (a in activities) {
                val raw = a.name.trim()
                val key = raw.lowercase()
                if (key.isEmpty()) continue
                if (!labelByKey.containsKey(key)) labelByKey[key] = raw
                val day = runCatching { LocalDate.parse(a.dayId) }.getOrNull() ?: continue
                if (!day.isInKpiWindow(window)) continue
                registeredDayIdsByNameKey.getOrPut(key) { mutableSetOf() }.add(a.dayId)
                if (a.checked) {
                    checkedDayIdsByNameKey.getOrPut(key) { mutableSetOf() }.add(a.dayId)
                }
            }
            val rows = labelByKey.keys.map { key ->
                val denom = registeredDayIdsByNameKey[key]?.size ?: 0
                val checkedDays = checkedDayIdsByNameKey[key]?.size ?: 0
                val pct = if (denom == 0) 0f else checkedDays * 100f / denom.toFloat()
                ActivityConsistencyKpi(
                    displayName = labelByKey[key] ?: key,
                    distinctCheckedDays = checkedDays,
                    denominatorDays = denom,
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
    /** Distinct days in the KPI window (month through today) with ≥1 activity row (any name). */
    val daysWithAnyActivityInMonth: Int = 0,
    /** Calendar days in the month through today (past month = full length); used for the “días con registro / …” summary line. */
    val consistencyDenominatorDays: Int = 31,
    val activityConsistency: List<ActivityConsistencyKpi> = emptyList(),
)