package com.example.timynice


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timynice.room.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

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

            _calendarState.value = CalendarState(
                yearMonth = yearMonth,
                days = days,
                dayAccomplishments = dayAccomplishments,
                calAccomplish = calAccomplish
            )
        }
    }
}

data class CalendarState  constructor(
    val yearMonth: YearMonth = YearMonth.now(), //here
    val days: List<String> = emptyList(),
    val dayAccomplishments: Map<String, Float> = emptyMap(),
    val calAccomplish: Float = 0f
)