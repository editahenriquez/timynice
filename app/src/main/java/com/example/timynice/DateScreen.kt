package com.example.timynice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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


@Composable
fun DateScreen(date: String, calendarViewModel: CalendarViewModel, onBackToCalendar: () -> Unit) {
    val database = AppDatabase.getDatabase(LocalContext.current)
    val viewModel = remember(date) { DateViewModel(database, date) }
    val coroutineScope = rememberCoroutineScope()

    val activities by viewModel.activities.collectAsState()
    val dayMessage by viewModel.dayMessage.collectAsState()
    val accomplishment by viewModel.dateAccomplish.collectAsState()

    //var editableMessage by remember { mutableStateOf(dayMessage) }

    var editableMessage by remember { mutableStateOf("") }
    LaunchedEffect(dayMessage) {
        editableMessage = dayMessage
    }

    Column(modifier = Modifier.padding(16.dp)) {
        /*Text(
            text = date,
            style = MaterialTheme.typography.headlineSmall
        )*/
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
                fontSize = 15.sp, // modify1: reduced date size
                color = MaterialTheme.colorScheme.onBackground // optional: ensure it fits the theme
            )
        }
        Spacer(modifier = Modifier.height(4.dp))//8

        Text("Motivational Message", fontSize = 12.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(1.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 6.dp) // modify1: tight padding
        ) {
            BasicTextField(
                value = editableMessage,
                onValueChange = {
                    editableMessage = it
                    viewModel.updateDayMessage(it)
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(2.dp))//8
        //Text("Completion: %.0f%%".format(accomplishment), color = Color.Blue, fontSize = 15.sp)

        Text(
            text = if (accomplishment < 100) {
                "Progress: ${accomplishment.toInt()}% 📈"
            } else {
                "Congrats! Mission achieved 🎉"
            },
            color = Color.Blue,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(2.dp))//8

        // Activity List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
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
                //Spacer(modifier = Modifier.height(2.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row {
            FloatingActionButton(
                onClick = {
                    val newStart = if (activities.isNotEmpty()) {
                        activities.last().end
                    } else "00:00:00"
                    val newActivity = ActivityEntity(
                        dayId = date,
                        name = "",
                        duration = "00:00:00",
                        start = newStart,
                        end = newStart,
                        checked = false
                    )
                    viewModel.insertOrUpdateActivity(newActivity)
                },
                containerColor = MaterialTheme.colorScheme.primary
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
                containerColor = MaterialTheme.colorScheme.primary
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

    // Auto-set start if not first row
    LaunchedEffect(previousEndTime) {
        if (!isFirstRow && previousEndTime != null && start != previousEndTime) {
            start = previousEndTime
            onActivityChange(activity.copy(start = start))
        }
    }

    // Auto-calculate end time
    LaunchedEffect(start, duration) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        try {
            val startTime = LocalTime.parse(start, formatter)
            val parts = duration.split(":").map { it.toIntOrNull() ?: 0 }
            val durHours = parts.getOrElse(0) { 0 }
            val durMinutes = parts.getOrElse(1) { 0 }
            val durSeconds = parts.getOrElse(2) { 0 }
            val durTotalSeconds = durHours * 3600 + durMinutes * 60 + durSeconds
            val endTime = startTime.plusSeconds(durTotalSeconds.toLong()).format(formatter)
            if (activity.end != endTime) {
                onActivityChange(activity.copy(end = endTime))
            }
        } catch (_: Exception) {
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFEFEF)) // light gray background
            .padding(horizontal = 0.dp, vertical = 0.dp)
    ) {
        // Reusable compact field builder
        @Composable
        fun CompactTextField(
            value: String,
            onValueChange: (String) -> Unit,
            modifier: Modifier = Modifier,
            enabled: Boolean = true,
        ) {
            Box(
                modifier = modifier
                    .border(0.1.dp, Color.Gray)
                    .background(Color.White)
                    .padding(horizontal = 0.1.dp, vertical = 0.1.dp)
                    .height(20.dp)
                    .then(if (!enabled) Modifier else Modifier)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    enabled = enabled,
                    textStyle = LocalTextStyle.current.copy(fontSize = 10.sp),//9
                    modifier = Modifier.fillMaxWidth()
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

        Spacer(modifier = Modifier.width(1.dp))

        CompactTextField(
            value = duration,
            onValueChange = {
                duration = it
                onActivityChange(activity.copy(duration = it))
            },
            modifier = Modifier.weight(0.6f)
        )

        Spacer(modifier = Modifier.width(1.dp))

        CompactTextField(
            value = start,
            onValueChange = {
                start = it
                onActivityChange(activity.copy(start = it))
            },
            modifier = Modifier.weight(0.6f)
        )

        Spacer(modifier = Modifier.width(1.dp))

        CompactTextField(
            value = end,
            onValueChange = {
                end = it
                onActivityChange(activity.copy(end = it))
            },
            modifier = Modifier.weight(0.6f),
            enabled = false
        )

        Spacer(modifier = Modifier.width(1.dp))

        Box(
            modifier = Modifier
                .weight(0.4f)
                .height(20.dp)
                .border(0.1.dp, Color.Gray)
                .padding(horizontal = 0.1.dp, vertical = 0.1.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onActivityChange(activity.copy(checked = it))
                },
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(1.dp))

        Box(
            modifier = Modifier
                .weight(0.4f)
                .height(20.dp)
                .border(0.1.dp, Color.Gray)
                .background(Color.White)
                .padding(horizontal = 0.1.dp, vertical = 0.1.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Activity",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}