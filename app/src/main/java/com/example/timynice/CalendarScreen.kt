package com.example.timynice
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.draw.shadow
import kotlin.system.exitProcess

@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel,
    onDayClick: (String) -> Unit
) {
    val calendarState by calendarViewModel.calendarState.collectAsState()
    val kpiScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top row with title and close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Consistency is key! 😉 📈",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 25.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { exitProcess(0) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Close App",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Header row: Year-Month and monthly accomplishment %
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = calendarState.yearMonth.format(DateTimeFormatter.ofPattern("yyyy - MMMM")),
                fontSize = 22.sp,
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

                val dayTextColor = when {
                    accomplish >= 100f -> MaterialTheme.colorScheme.primary
                    accomplish >= 0f -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .padding(horizontal = 2.dp, vertical = 2.dp)
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
                        color = dayTextColor//MaterialTheme.colorScheme.onSurfaceVariant,//if (accomplish > 0f) Color.Blue else Color.Black,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        AccomplishmentChartLine(
            dayAccomplishments = calendarState.dayAccomplishments,
            yearMonth = calendarState.yearMonth
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(kpiScrollState),
        ) {
            ActivityConsistencyKpiArea(calendarState = calendarState)
        }
    }
}

@Composable
private fun ActivityConsistencyKpiArea(
    calendarState: CalendarState,
) {
    val denom = calendarState.consistencyDenominatorDays
    val ymNow = java.time.YearMonth.from(LocalDate.now())
    val isCurrentMonth = calendarState.yearMonth == ymNow
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Consistencia por actividad",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Días con algún registro este mes: ${calendarState.daysWithAnyActivityInMonth}/$denom",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = buildString {
                append("Consistencia (%) = días en que la actividad está marcada (completada) ÷ ")
                append(
                    if (isCurrentMonth) {
                        "días del mes hasta hoy × 100. "
                    } else {
                        "días del mes × 100. "
                    }
                )
                append("Un día cuenta si al menos una fila de esa actividad tiene el check ese día.")
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Actividad",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Consist.",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(56.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Spacer(modifier = Modifier.height(6.dp))

        if (calendarState.activityConsistency.isEmpty()) {
            Text(
                text = "Sin actividades registradas en este mes.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            calendarState.activityConsistency.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(
                            text = row.displayName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                        Text(
                            text = "${row.distinctCheckedDays}/${row.denominatorDays} días ✓",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Text(
                        text = "${row.consistencyPercent.roundToInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(56.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
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

    val maxHeight = 140.dp
    val yAxisSteps = listOf(100f, 75f, 50f, 25f, 0f)
    val xAxisFontSize = when {
        daysInMonth <= 28 -> 10.sp
        daysInMonth <= 30 -> 9.sp
        else -> 8.sp
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val yAxisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val xAxisTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        // Fixed Y-axis labels (narrow to give more space to chart)
        Column(
            modifier = Modifier
                .width(26.dp)
                .height(maxHeight),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yAxisSteps.forEach {
                Text(
                    text = "${it.toInt()}%",
                    fontSize = 10.sp,
                    color = yAxisTextColor,
                    modifier = Modifier.height(maxHeight / (yAxisSteps.size - 1))
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxHeight)
                ) {
                    val chartHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val widthPerDay = size.width / daysInMonth

                        for (i in 0 until dataPoints.size - 1) {
                            val startX = i * widthPerDay + widthPerDay / 2f
                            val startY = chartHeightPx * (1f - dataPoints[i] / 100f)
                            val endX = (i + 1) * widthPerDay + widthPerDay / 2f
                            val endY = chartHeightPx * (1f - dataPoints[i + 1] / 100f)
                            drawLine(
                                color = lineColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 1.1.dp.toPx()
                            )
                        }

                        yAxisSteps.forEach { yValue ->
                            val yPos = chartHeightPx * (1f - yValue / 100f)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, yPos),
                                end = Offset(size.width, yPos),
                                strokeWidth = 0.5.dp.toPx()
                            )
                        }

                        for (i in 0..daysInMonth) {
                            val x = i * widthPerDay
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 0.5.dp.toPx()
                            )
                        }

                        dataPoints.forEachIndexed { index, value ->
                            val x = index * widthPerDay + widthPerDay / 2f
                            val y = chartHeightPx * (1f - value / 100f)
                            drawCircle(
                                color = dotColor,
                                radius = 2.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    for (day in 1..daysInMonth) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = day.toString(),
                                fontSize = xAxisFontSize,
                                color = xAxisTextColor,
                                maxLines = 1,
                                lineHeight = xAxisFontSize,
                            )
                        }
                    }
                }
            }
        }
    }
}