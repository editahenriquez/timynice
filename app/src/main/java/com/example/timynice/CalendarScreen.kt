package com.example.timynice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel,
    onDayClick: (String) -> Unit
) {
    val calendarState by calendarViewModel.calendarState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        // Header row: Year-Month and monthly accomplishment %
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = calendarState.yearMonth.format(DateTimeFormatter.ofPattern("yyyy - MMMM")),
                style = MaterialTheme.typography.headlineMedium
            )
            /*Text(
                text = "%.0f%%".format(calendarState.calAccomplish),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Blue
            )*/
            val monthlyAccomplishmentText = if (calendarState.calAccomplish >= 100f) {
                "🎉 %.0f%%".format(calendarState.calAccomplish)
            } else {
                "📈 %.0f%%".format(calendarState.calAccomplish)
            }

            Text(
                text = monthlyAccomplishmentText,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Blue,
                fontSize = 20.sp
            )

        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                Text(text = it, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Days grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            val firstDayOfMonth = calendarState.yearMonth.atDay(1).dayOfWeek.value % 7 // Sunday=0
            val daysInMonth = calendarState.yearMonth.lengthOfMonth()

            // Empty cells to offset first day according to weekday
            items(firstDayOfMonth) {
                Box(modifier = Modifier.size(40.dp)) {}
            }

            // Day cells with accomplishment percentage and click handler
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val dayStr = String.format("%04d-%02d-%02d", calendarState.yearMonth.year, calendarState.yearMonth.monthValue, day)
                val accomplish = calendarState.dayAccomplishments[dayStr] ?: 0f
                val isToday = dayStr == LocalDate.now().toString()
                val displayText = if (accomplish > 0f) "$day(${accomplish.toInt()}%)" else "$day"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(2.dp)
                        .background(if (isToday) Color.LightGray else Color.Transparent)
                        .clickable { onDayClick(dayStr) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        fontSize = 11.sp, //10
                        fontWeight = FontWeight.Normal,
                        color = if (accomplish > 0f) Color.Blue else Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        AccomplishmentChartLine(
            dayAccomplishments = calendarState.dayAccomplishments,
            yearMonth = calendarState.yearMonth
        )
    }
}

@Composable
fun AccomplishmentChartLine(
    dayAccomplishments: Map<String, Float>,
    yearMonth: java.time.YearMonth
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val maxHeight = 60.dp // height for chart vertical scale

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxHeight)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 100% label and chart points row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = "100%", fontSize = 10.sp, modifier = Modifier.width(30.dp))
            Spacer(modifier = Modifier.width(4.dp))

            // Draw stars "*" for each day where accomplishment >= 80%
            for (day in 1..daysInMonth) {
                val dayStr = String.format("%04d-%02d-%02d", yearMonth.year, yearMonth.monthValue, day)
                val acc = dayAccomplishments[dayStr] ?: 0f
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (acc >= 80f) {
                        Text(text = "*", color = Color.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Middle percentage rows: 30%, 20%, 10% with stars for respective thresholds //modify1:
        val thresholds = listOf(30, 20, 10)
        for (threshold in thresholds) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(text = "${threshold}%", fontSize = 10.sp, modifier = Modifier.width(30.dp))
                Spacer(modifier = Modifier.width(4.dp))
                for (day in 1..daysInMonth) {
                    val dayStr = String.format("%04d-%02d-%02d", yearMonth.year, yearMonth.monthValue, day)
                    val acc = dayAccomplishments[dayStr] ?: 0f
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(12.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (acc >= threshold && acc < threshold + 10) {
                            Text(text = "*", color = Color.Blue, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // X axis labels row (days) //modify1:
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "0%", fontSize = 10.sp, modifier = Modifier.width(30.dp))
            Spacer(modifier = Modifier.width(4.dp))

            for (day in 1..daysInMonth) {
                Text(
                    text = day.toString(),
                    fontSize = 10.sp,
                    modifier = Modifier.width(20.dp),
                    maxLines = 1
                )
            }
        }
    }
}