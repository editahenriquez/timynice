package com.example.timynice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timynice.room.ActivityEntity
import com.example.timynice.room.AppDatabase
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.onFocusChanged

@Composable
fun DateScreen(date: String, calendarViewModel: CalendarViewModel, onBackToCalendar: () -> Unit) {
    val database = AppDatabase.getDatabase(LocalContext.current)
    val viewModel = remember(date) { DateViewModel(database, date) }
    val coroutineScope = rememberCoroutineScope()
    val activities by viewModel.activities.collectAsState()
    val dayMessage by viewModel.dayMessage.collectAsState()
    val accomplishment by viewModel.dateAccomplish.collectAsState()
    var editableMessage by remember { mutableStateOf("") }

    LaunchedEffect(dayMessage) {
        editableMessage = dayMessage
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxHeight() ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackToCalendar, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Calendar",
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = date,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Text("Motivational Message / Comment", fontSize = 15.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(1.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = editableMessage,
                onValueChange = {
                    editableMessage = it
                    viewModel.updateDayMessage(it)
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (accomplishment < 100) {
                "Progress: ${accomplishment.toInt()}% 📈"
            } else {
                "Congrats! Mission achieved: ${accomplishment.toInt()}% 🎉"
            },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Format: hh:mm (e.g., 1 ->10:00, 12 ->12:00, 123 →12:30, 1235 ->12:35)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(1.5.dp))

        // Header row for field labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Activity",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Time",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "End",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.weight(0.4f)) // Placeholder for checkbox
            Box(modifier = Modifier.weight(0.4f)) // Placeholder for delete button
        }
        Spacer(modifier = Modifier.height(1.8.dp))

        // Activity List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(activities) { index, activity ->
                ActivityRow(
                    activity = activity,
                    onActivityChange = { updated ->
                        viewModel.insertOrUpdateActivity(updated)
                    },
                    onDelete = {
                        viewModel.deleteActivity(activity)
                    },
                    isFirstRow = (index == 0),
                    previousEndTime = if (index > 0) activities[index - 1].end else null
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        )  {
            FloatingActionButton(
                onClick = {
                    val newStart = if (activities.isNotEmpty()) {
                        activities.last().end
                    } else "00:00"
                    val newActivity = ActivityEntity(
                        dayId = date,
                        name = "",
                        duration = "00:00",
                        start = newStart,
                        end = newStart,
                        checked = false
                    )
                    viewModel.insertOrUpdateActivity(newActivity)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Activity")
            }
            Spacer(modifier = Modifier.width(16.dp))

            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        viewModel.resetActivities()
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Reset Activities")
            }
        }
    }
}

@Composable
fun ActivityRow(
    activity: ActivityEntity,
    onActivityChange: (ActivityEntity) -> Unit,
    onDelete: () -> Unit,
    isFirstRow: Boolean,
    previousEndTime: String?
) {
    var name by remember { mutableStateOf(activity.name) }
    var duration by remember { mutableStateOf(activity.duration) }
    var start by remember { mutableStateOf(activity.start) }
    //var end by remember { mutableStateOf(activity.end) }
    var end = activity.end
    var checked by remember { mutableStateOf(activity.checked) }
    var skipInitialCalculation by remember { mutableStateOf(true) }
    var hasFocus by remember { mutableStateOf(false) }

    // Auto-set start if not first row
    LaunchedEffect(previousEndTime) {
        if (!isFirstRow && previousEndTime != null && start != previousEndTime) {
            start = previousEndTime
            onActivityChange(activity.copy(start = start))
        }
    }

    // Auto-calculate end time
    LaunchedEffect(start, duration) {
        if (skipInitialCalculation) {
            skipInitialCalculation = false
            return@LaunchedEffect
        }

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        try {
            val startTime = LocalTime.parse(start, formatter)
            val formattedDuration = formatToTime(duration)
            val parts = formattedDuration.split(":").map { it.toIntOrNull() ?: 0 }
            val durHours = parts.getOrElse(0) { 0 }
            val durMinutes = parts.getOrElse(1) { 0 }
            val durTotalSeconds = durHours * 3600 + durMinutes * 60
            val endTime = startTime.plusSeconds(durTotalSeconds.toLong()).format(formatter)
            if (activity.end != endTime) {
                end = endTime // Update the local variable
                onActivityChange(activity.copy(end = endTime))
            }
        } catch (_: Exception) {
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
            .padding(horizontal = 0.dp, vertical = 0.dp)
            .shadow(2.dp, RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
    ) {
        // Reusable compact field builder
        @Composable
        fun CompactTextField(
            value: String,
            onValueChange: (String) -> Unit,
            modifier: Modifier = Modifier,
            enabled: Boolean = true,
            centerText: Boolean = false
        ) {
            Box(
                modifier = modifier
                    .border(0.1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 0.1.dp, vertical = 0.1.dp)
                    .height(20.dp)
                    .then(if (!enabled) Modifier else Modifier),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    enabled = enabled,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 13.sp,
                        textAlign = if (centerText) TextAlign.Center else TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        CompactTextField(
            value = name,
            onValueChange = {
                name = it
                onActivityChange(activity.copy(name = it))
            },
            modifier = Modifier.weight(2f)
        )
        Spacer(modifier = Modifier.width(0.dp))

        CompactTextField(
            value = duration,
            onValueChange = {
                duration = it
                onActivityChange(activity.copy(duration = formatToTime(it)))
            },
            modifier = Modifier
                .weight(0.5f)
                .onFocusChanged { focusState ->
                    if (hasFocus && !focusState.isFocused)
                    {
                        // Focus lost: format now
                        val formatted = formatToTime(duration)
                        duration = formatted
                        onActivityChange(activity.copy(duration = duration))
                    }
                    hasFocus = focusState.isFocused
                },
            centerText = true,
        )
        Spacer(modifier = Modifier.width(0.dp))

        CompactTextField(
            value = start,
            onValueChange = {
                start = it
                onActivityChange(activity.copy(start = formatToTime(it)))
            },
            modifier = Modifier
                .weight(0.5f)
                .onFocusChanged { focusState ->
                    if (hasFocus && !focusState.isFocused)
                    {
                        // Focus lost: format now
                        val formatted = formatToTime(start)
                        start = formatted
                        onActivityChange(activity.copy(start = start))
                    }
                    hasFocus = focusState.isFocused
                },
            centerText = true
        )
        Spacer(modifier = Modifier.width(0.dp))

        CompactTextField(
            value = end,
            onValueChange = {
                end = it
                onActivityChange(activity.copy(end = formatToTime(it)))
            },
            modifier = Modifier.weight(0.5f),
            enabled = false,
            centerText = true
        )
        Spacer(modifier = Modifier.width(0.dp))

        Box(
            modifier = Modifier
                .weight(0.4f)
                .height(20.dp)
                .border(0.1.dp, MaterialTheme.colorScheme.outline)
                .padding(horizontal = 0.1.dp, vertical = 0.1.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onActivityChange(activity.copy(checked = it))
                },
                modifier = Modifier.size(14.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline)
            )
        }
        Spacer(modifier = Modifier.width(0.dp))

        Box(
            modifier = Modifier
                .weight(0.4f)
                .height(20.dp)
                .border(0.1.dp, MaterialTheme.colorScheme.outline)
                .padding(horizontal = 0.1.dp, vertical = 0.1.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Activity",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

fun formatToTime(digits: String): String {
    val clean = digits.filter { it.isDigit() }.take(4).padEnd(4, '0')
    val hours = clean.take(2)
    val minutes = clean.drop(2)
    return "$hours:$minutes"
}