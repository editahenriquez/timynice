package com.example.timynice
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.shadow
@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel,
    onDayClick: (String) -> Unit
) {
    val calendarState by calendarViewModel.calendarState.collectAsState()
    val SoftBlue = Color(0xFF82B1FF)
    Column(modifier = Modifier.padding(16.dp)) {
        val SoftBlue = Color(0xFF82B1FF) // already defined
        Text(
            text = "TimyNice: Your Daily Wins! 😉 📈",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 22.sp, // modify1: smaller, softer
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                Text(
                    text = it,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp),
                    maxLines = 1
                )
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
                        .size(44.dp)
                        .padding(2.dp)
                        .shadow(elevation = if (isToday) 6.dp else 2.dp, shape = RoundedCornerShape(8.dp))
                        .background(
                            color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
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
        Spacer(modifier = Modifier.height(18.dp))
        Column(
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            // Top line: Year - month in lowercase
            Text(
                text = calendarState.yearMonth.format(DateTimeFormatter.ofPattern("yyyy - MMMM")),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 25.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(1.dp))
            // Second line: Motivational phrase with emoji
            Text(
                text = "Small Wins, Big Impact!🚀",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp,
                //fontWeight = FontWeight.SemiBold,
                color = SoftBlue,
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
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
    val dataPoints = (1..daysInMonth).map { day ->
        val dayStr = String.format("%04d-%02d-%02d", yearMonth.year, yearMonth.monthValue, day)
        dayAccomplishments[dayStr] ?: 0f
    }
    val maxHeight = 150.dp // Increased height for y-axis labels
    val yAxisSteps = listOf(100f, 75f, 50f, 25f, 0f) // Y-axis values
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(maxHeight)) {
            val chartHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            Canvas(modifier = Modifier
                .fillMaxSize()
                .padding(start = 30.dp)) { // Leave space for y-axis labels
                //val widthPerDay = (size.width - 30.dp.toPx()) / daysInMonth
                val widthPerDay = size.width / daysInMonth

                for (i in 0 until dataPoints.size - 1) {
                    val startX = i * widthPerDay + widthPerDay / 2f
                    val startY = chartHeightPx * (1f - dataPoints[i] / 100f)
                    val endX = (i + 1) * widthPerDay + widthPerDay / 2f
                    val endY = chartHeightPx * (1f - dataPoints[i + 1] / 100f)
                    drawLine(
                        color = Color.Blue,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.1.dp.toPx()
                    )
                }


                // Draw horizontal grid lines and Y-axis values
                yAxisSteps.forEach { yValue ->
                    val yPos = chartHeightPx * (1f - yValue / 100f)
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, yPos),
                        end = Offset(size.width, yPos),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw vertical grid lines (cuadrícula)
                for (i in 0..daysInMonth) {
                    val x = i * widthPerDay
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 0.5.dp.toPx() // lighter vertical line
                    )
                }

                // Add circle markers on top of line segments
                dataPoints.forEachIndexed { index, value ->
                    val x = index * widthPerDay + widthPerDay / 2f
                    val y = chartHeightPx * (1f - value / 100f)
                    drawCircle(
                        color = Color.Blue,
                        radius = 1.5.dp.toPx(),
                        center = Offset(x, y),
                    )
                }
            }
            // Y-axis percentage labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 0.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                yAxisSteps.forEach { yValue ->
                    Text(
                        text = "${yValue.toInt()}%",
                        fontSize = 10.sp,
                        modifier = Modifier.height(maxHeight / (yAxisSteps.size - 1)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // X-axis day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp), // aligns with canvas left margin
            horizontalArrangement = Arrangement.Start
        ) {
            val labelWidth = (1f / daysInMonth)
            for (day in 1..daysInMonth) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.toString(),
                        fontSize = 7.2.sp,
                        color = Color.DarkGray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}