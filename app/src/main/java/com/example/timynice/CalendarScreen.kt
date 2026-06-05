package com.example.timynice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timynice.ui.components.TimyniceSectionCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel,
    onDayClick: (String) -> Unit,
    onClose: () -> Unit,
) {
    val calendarState by calendarViewModel.calendarState.collectAsState()
    val kpiScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Timynice",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Consistencia es clave 🗝️",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Salir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = calendarState.yearMonth.format(DateTimeFormatter.ofPattern("yyyy - MMMM")),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp),
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val firstDayOfMonth = calendarState.yearMonth.atDay(1).dayOfWeek.value % 7
            val daysInMonth = calendarState.yearMonth.lengthOfMonth()
            items(firstDayOfMonth) {
                Box(modifier = Modifier.size(40.dp))
            }
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val dayStr = String.format(
                    "%04d-%02d-%02d",
                    calendarState.yearMonth.year,
                    calendarState.yearMonth.monthValue,
                    day,
                )
                val accomplish = calendarState.dayAccomplishments[dayStr] ?: 0f
                val isToday = dayStr == LocalDate.now().toString()
                val displayText = if (accomplish > 0f) "$day(${accomplish.toInt()}%)" else "$day"

                val dayTextColor = when {
                    accomplish >= 100f -> MaterialTheme.colorScheme.primary
                    accomplish > 0f -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.outline
                }

                val cellShape = MaterialTheme.shapes.small
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .clip(cellShape)
                        .background(
                            color = if (isToday) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = cellShape,
                        )
                        .then(
                            if (isToday) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = cellShape,
                                )
                            } else {
                                Modifier.border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = cellShape,
                                )
                            },
                        )
                        .clickable { onDayClick(dayStr) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayText,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.labelSmall,
                        color = dayTextColor,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        AccomplishmentChartLine(
            dayAccomplishments = calendarState.dayAccomplishments,
            yearMonth = calendarState.yearMonth,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
    var showFormula by remember { mutableStateOf(false) }
    val formulaText = if (isCurrentMonth) {
        "Consistencia (%) = días marcados ÷ días con registro de la actividad (a la fecha) × 100."
    } else {
        "Consistencia (%) = días marcados ÷ días con registro de la actividad en el mes × 100."
    }

    TimyniceSectionCard {
        Text(
            text = "Consistencia por actividad (Sólo hábitos★)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Días con algún registro este mes: ${calendarState.daysWithAnyActivityInMonth}/$denom",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (showFormula) "Ocultar fórmula" else "Ver fórmula",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { showFormula = !showFormula },
        )
        if (showFormula) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formulaText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Actividad",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Consist.",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.End,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(2.dp))

        if (calendarState.activityConsistency.isEmpty()) {
            Text(
                text = "Sin hábitos registrados en este mes.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            val kpiRows = calendarState.activityConsistency
            val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            kpiRows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.5.dp,
                        color = dividerColor,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    Text(
                        text = "${row.consistencyPercent.roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(52.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun AccomplishmentChartLine(
    dayAccomplishments: Map<String, Float>,
    yearMonth: java.time.YearMonth,
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
            .padding(horizontal = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .width(26.dp)
                .height(maxHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            yAxisSteps.forEach {
                Text(
                    text = "${it.toInt()}%",
                    fontSize = 10.sp,
                    color = yAxisTextColor,
                    modifier = Modifier.height(maxHeight / (yAxisSteps.size - 1)),
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxHeight),
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
                                strokeWidth = 1.1.dp.toPx(),
                            )
                        }

                        yAxisSteps.forEach { yValue ->
                            val yPos = chartHeightPx * (1f - yValue / 100f)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, yPos),
                                end = Offset(size.width, yPos),
                                strokeWidth = 0.5.dp.toPx(),
                            )
                        }

                        for (i in 0..daysInMonth) {
                            val x = i * widthPerDay
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 0.5.dp.toPx(),
                            )
                        }

                        dataPoints.forEachIndexed { index, value ->
                            val x = index * widthPerDay + widthPerDay / 2f
                            val y = chartHeightPx * (1f - value / 100f)
                            drawCircle(
                                color = dotColor,
                                radius = 2.dp.toPx(),
                                center = Offset(x, y),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    for (day in 1..daysInMonth) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter,
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
