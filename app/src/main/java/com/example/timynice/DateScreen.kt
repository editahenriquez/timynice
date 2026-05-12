package com.example.timynice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScreen(date: String, calendarViewModel: CalendarViewModel, onBackToCalendar: () -> Unit) {
    val database = AppDatabase.getDatabase(LocalContext.current)
    val viewModel = remember(date) { DateViewModel(database, date) }
    val coroutineScope = rememberCoroutineScope()
    val activities by viewModel.activities.collectAsState()
    val dayMessage by viewModel.dayMessage.collectAsState()
    val accomplishment by viewModel.dateAccomplish.collectAsState()
    var editableMessage by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ActivityEntity?>(null) }
    var showDuplicateDatePicker by remember { mutableStateOf(false) }
    val duplicateDatePickerState = rememberDatePickerState()
    var duplicateSourceIso by remember { mutableStateOf<String?>(null) }
    var duplicateActivityCount by remember { mutableStateOf(0) }
    var showDuplicateReplaceConfirm by remember { mutableStateOf(false) }
    var showDuplicateEmptyMessage by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    val dateDisplayFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    }
    val destLabel = remember(date, dateDisplayFormatter) {
        runCatching { LocalDate.parse(date).format(dateDisplayFormatter) }.getOrDefault(date)
    }

    LaunchedEffect(dayMessage) {
        editableMessage = dayMessage
    }

    Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier.weight(2.4f),
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
        }
        Spacer(modifier = Modifier.height(1.8.dp))

        // Activity list: swipe-left to delete (Compose SwipeToDismissBox; no RecyclerView/ItemTouchHelper in this app).
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(
                items = activities,
                key = { _, item -> item.id }
            ) { _, activity ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { target ->
                        if (target == SwipeToDismissBoxValue.EndToStart) {
                            pendingDelete = activity
                            false
                        } else {
                            true
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val dismissTowardEnd = layoutDirection == LayoutDirection.Ltr
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = if (dismissTowardEnd) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    },
                    content = {
                        ActivityRow(
                            activity = activity,
                            onActivityChange = { updated ->
                                viewModel.insertOrUpdateActivity(updated)
                            }
                        )
                    }
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
                onClick = { showDuplicateDatePicker = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate day from date")
            }
            Spacer(modifier = Modifier.width(16.dp))

            FloatingActionButton(
                onClick = { showDeleteAllConfirm = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Reset Activities")
            }
        }
    }

        if (showDeleteAllConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirm = false },
                title = {
                    Text(
                        "¿Eliminar todas las actividades del $destLabel?\n\n" +
                            "Esta acción no se puede deshacer."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.resetActivities()
                            }
                            showDeleteAllConfirm = false
                        }
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllConfirm = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        pendingDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("¿Eliminar esta actividad?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteActivity(target)
                            pendingDelete = null
                        }
                    ) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
                }
            )
        }

        if (showDuplicateDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDuplicateDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val millis = duplicateDatePickerState.selectedDateMillis ?: return@TextButton
                            showDuplicateDatePicker = false
                            // DatePicker millis = UTC midnight for the selected calendar day; use UTC here
                            // so the source date matches the picker (systemDefault() can shift ±1 day).
                            val pickedIso = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE)
                            coroutineScope.launch {
                                val sourceActivities = viewModel.fetchActivitiesForDay(pickedIso)
                                if (sourceActivities.isEmpty()) {
                                    showDuplicateEmptyMessage = true
                                } else {
                                    duplicateSourceIso = pickedIso
                                    duplicateActivityCount = sourceActivities.size
                                    showDuplicateReplaceConfirm = true
                                }
                            }
                        }
                    ) { Text("Aceptar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDuplicateDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = duplicateDatePickerState)
            }
        }

        if (showDuplicateEmptyMessage) {
            AlertDialog(
                onDismissRequest = { showDuplicateEmptyMessage = false },
                confirmButton = {
                    TextButton(onClick = { showDuplicateEmptyMessage = false }) { Text("OK") }
                },
                title = { Text("No hay actividades en esa fecha") }
            )
        }

        if (showDuplicateReplaceConfirm) {
            val sourceIso = duplicateSourceIso
            val srcLabel = sourceIso?.let {
                runCatching { LocalDate.parse(it).format(dateDisplayFormatter) }.getOrDefault(it)
            } ?: ""
            AlertDialog(
                onDismissRequest = {
                    showDuplicateReplaceConfirm = false
                    duplicateSourceIso = null
                },
                title = { Text("Duplicar día") },
                text = {
                    Text(
                        "Se copiarán $duplicateActivityCount actividades desde el $srcLabel hacia el $destLabel (día de esta pantalla).\n\n" +
                            "Las actividades que ya existían en el $destLabel serán eliminadas. ¿Continuar?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (sourceIso != null) {
                                viewModel.applyDuplicateFromSourceDate(sourceIso)
                            }
                            showDuplicateReplaceConfirm = false
                            duplicateSourceIso = null
                        }
                    ) { Text("Continuar") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDuplicateReplaceConfirm = false
                            duplicateSourceIso = null
                        }
                    ) { Text("Cancelar") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityRow(
    activity: ActivityEntity,
    onActivityChange: (ActivityEntity) -> Unit,
) {
    var name by remember(activity.id) { mutableStateOf(activity.name) }
    var duration by remember(activity.id) { mutableStateOf(activity.duration) }
    var start by remember(activity.id) { mutableStateOf(activity.start) }
    var checked by remember(activity.id) { mutableStateOf(activity.checked) }

    var nameFieldFocused by remember(activity.id) { mutableStateOf(false) }
    var showDurationTimePicker by remember(activity.id) { mutableStateOf(false) }
    var showStartTimePicker by remember(activity.id) { mutableStateOf(false) }

    // Sync from ViewModel; skip duration/start while a time picker is open.
    LaunchedEffect(
        activity.id,
        activity.name,
        activity.start,
        activity.end,
        activity.duration,
        activity.checked,
        nameFieldFocused,
        showDurationTimePicker,
        showStartTimePicker
    ) {
        if (!nameFieldFocused) name = activity.name
        if (!showDurationTimePicker) duration = activity.duration
        if (!showStartTimePicker) start = activity.start
        checked = activity.checked
    }

    val rowFieldHeight = 20.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
            .padding(horizontal = 0.dp, vertical = 0.dp)
            .shadow(2.dp, RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Reusable compact field builder
        @Composable
        fun CompactTextField(
            value: String,
            onValueChange: (String) -> Unit,
            modifier: Modifier = Modifier,
            enabled: Boolean = true,
            centerText: Boolean = false,
        ) {
            Box(
                modifier = modifier
                    .border(0.1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 0.1.dp, vertical = 0.1.dp)
                    .height(rowFieldHeight)
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

        /** hh:mm: same footprint as CompactTextField; Text + clickable (no BasicTextField). */
        @Composable
        fun HhMmPickerCell(
            value: String,
            modifier: Modifier = Modifier,
            onClick: () -> Unit,
        ) {
            Box(
                modifier = modifier
                    .border(0.1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 0.1.dp, vertical = 0.1.dp)
                    .height(rowFieldHeight)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }

        CompactTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier
                .weight(2.4f)
                .onFocusChanged { fs ->
                    if (nameFieldFocused && !fs.isFocused) {
                        onActivityChange(activity.copy(name = name))
                    }
                    nameFieldFocused = fs.isFocused
                }
        )
        Spacer(modifier = Modifier.width(0.dp))

        HhMmPickerCell(
            value = duration,
            modifier = Modifier.weight(0.5f),
            onClick = { showDurationTimePicker = true },
        )
        Spacer(modifier = Modifier.width(0.dp))

        HhMmPickerCell(
            value = start,
            modifier = Modifier.weight(0.5f),
            onClick = { showStartTimePicker = true },
        )
        Spacer(modifier = Modifier.width(0.dp))

        CompactTextField(
            value = activity.end,
            onValueChange = { },
            modifier = Modifier.weight(0.5f),
            enabled = false,
            centerText = true
        )
        Spacer(modifier = Modifier.width(0.dp))

        Box(
            modifier = Modifier
                .weight(0.4f)
                .height(rowFieldHeight)
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
    }

    if (showDurationTimePicker) {
        val (ih, im) = parseHourMinuteForPicker(duration)
        val timeState = rememberTimePickerState(
            initialHour = ih,
            initialMinute = im,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showDurationTimePicker = false },
            title = { Text("Duración (hh:mm)") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formatted = formatHourMinute(timeState.hour, timeState.minute)
                        duration = formatted
                        onActivityChange(
                            activity.copy(
                                duration = formatted,
                                start = formatToTime(start),
                            )
                        )
                        showDurationTimePicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDurationTimePicker = false }) { Text("Cancelar") }
            },
        )
    }

    if (showStartTimePicker) {
        val (ih, im) = parseHourMinuteForPicker(start)
        val timeState = rememberTimePickerState(
            initialHour = ih,
            initialMinute = im,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Inicio (hh:mm)") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formatted = formatHourMinute(timeState.hour, timeState.minute)
                        start = formatted
                        onActivityChange(
                            activity.copy(
                                start = formatted,
                                duration = formatToTime(duration),
                            )
                        )
                        showStartTimePicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancelar") }
            },
        )
    }
}

fun formatToTime(digits: String): String {
    val clean = digits.filter { it.isDigit() }.take(4).padEnd(4, '0')
    val hours = clean.take(2)
    val minutes = clean.drop(2)
    return "$hours:$minutes"
}

/** Parses "H:mm" or "HH:mm" for the Material time picker (hour 0–23). */
private fun parseHourMinuteForPicker(hhMm: String): Pair<Int, Int> {
    val parts = hhMm.trim().split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}

private fun formatHourMinute(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))