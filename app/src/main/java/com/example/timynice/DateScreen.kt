package com.example.timynice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timynice.room.ActivityEntity
import com.example.timynice.room.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import kotlin.math.roundToInt
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    var showDeleteAllEmptyMessage by remember { mutableStateOf(false) }
    var focusActivityId by remember { mutableStateOf<String?>(null) }
    val activityListState = rememberLazyListState()
    var displayActivities by remember { mutableStateOf<List<ActivityEntity>>(emptyList()) }
    var isDraggingReorder by remember { mutableStateOf(false) }
    var draggedActivityId by remember { mutableStateOf<String?>(null) }
    var dropTargetIndex by remember { mutableIntStateOf(-1) }
    var dragVisualOffsetPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val activityRowHeightPx = remember(density) { with(density) { 28.dp.toPx() } }
    val reorderSizeSpring = spring<IntSize>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    LaunchedEffect(activities) {
        if (!isDraggingReorder) {
            displayActivities = activities
        }
    }

    val dateDisplayFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    }
    val destLabel = remember(date, dateDisplayFormatter) {
        runCatching { LocalDate.parse(date).format(dateDisplayFormatter) }.getOrDefault(date)
    }

    LaunchedEffect(dayMessage) {
        editableMessage = dayMessage
    }

    LaunchedEffect(focusActivityId, activities) {
        val id = focusActivityId ?: return@LaunchedEffect
        val idx = activities.indexOfFirst { it.id == id }
        if (idx >= 0) {
            activityListState.animateScrollToItem(idx)
        }
    }

    Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackToCalendar) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver al calendario",
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Comentario del día",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(1.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.small,
                )
                .background(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.shapes.small,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            BasicTextField(
                value = editableMessage,
                onValueChange = {
                    editableMessage = it
                    viewModel.updateDayMessage(it)
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (accomplishment >= 100f) {
                "Progreso 100% Felicitaciones 🎉"
            } else {
                "Progreso: ${accomplishment.toInt()}%"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(2.dp))

        // Header row for field labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.width(28.dp))
            Text(
                text = "Activity",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(2.4f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Time",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Start",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "End",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center,
            )
            Box(modifier = Modifier.weight(0.4f)) // Placeholder for checkbox
        }
        Spacer(modifier = Modifier.height(1.8.dp))

        // Activity list: swipe right = toggle hábito, swipe left = delete (SwipeToDismissBox).
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = activityListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(
                items = displayActivities,
                key = { _, item -> item.id },
            ) { index, activity ->
                val isDragged = draggedActivityId == activity.id
                val showDropLine = draggedActivityId != null && dropTargetIndex == index
                val rowAlpha by animateFloatAsState(
                    targetValue = when {
                        isDragged -> 0.9f
                        draggedActivityId != null -> 0.98f
                        else -> 1f
                    },
                    animationSpec = tween(200),
                    label = "rowAlpha",
                )
                val rowShadow by animateDpAsState(
                    targetValue = if (isDragged) 5.dp else 0.dp,
                    animationSpec = tween(200),
                    label = "rowShadow",
                )
                var dragStepAccumulatedPx by remember(activity.id) { mutableFloatStateOf(0f) }
                var pendingSwipeReset by remember(activity.id) { mutableStateOf(false) }
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { target ->
                        when (target) {
                            SwipeToDismissBoxValue.EndToStart -> {
                                pendingDelete = activity
                                pendingSwipeReset = true
                                false
                            }
                            SwipeToDismissBoxValue.StartToEnd -> {
                                val latest = displayActivities.find { it.id == activity.id }
                                    ?: activity
                                viewModel.insertOrUpdateActivity(
                                    latest.copy(esHabito = !latest.esHabito),
                                )
                                pendingSwipeReset = true
                                false
                            }
                            else -> true
                        }
                    },
                )
                LaunchedEffect(pendingSwipeReset, activity.id) {
                    if (pendingSwipeReset) {
                        dismissState.reset()
                        pendingSwipeReset = false
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = reorderSizeSpring)
                        .zIndex(if (isDragged) 1f else 0f),
                ) {
                    if (showDropLine) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(View2Colors.dropLine),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = if (isDragged) dragVisualOffsetPx else 0f
                            }
                            .alpha(rowAlpha)
                            .then(
                                if (isDragged) {
                                    Modifier.shadow(rowShadow, RoundedCornerShape(5.dp), clip = false)
                                } else {
                                    Modifier
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(28.dp)
                                .pointerInput(activity.id, displayActivities.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            isDraggingReorder = true
                                            draggedActivityId = activity.id
                                            dragVisualOffsetPx = 0f
                                            dragStepAccumulatedPx = 0f
                                            dropTargetIndex =
                                                displayActivities.indexOfFirst { it.id == activity.id }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragVisualOffsetPx += dragAmount.y
                                            dragStepAccumulatedPx += dragAmount.y
                                            val steps = (dragStepAccumulatedPx / activityRowHeightPx).roundToInt()
                                            if (steps == 0) return@detectDragGesturesAfterLongPress
                                            val currentIndex =
                                                displayActivities.indexOfFirst { it.id == activity.id }
                                            if (currentIndex < 0) return@detectDragGesturesAfterLongPress
                                            val newIndex = (currentIndex + steps)
                                                .coerceIn(0, displayActivities.lastIndex)
                                            if (newIndex != currentIndex) {
                                                val mut = displayActivities.toMutableList()
                                                val moved = mut.removeAt(currentIndex)
                                                mut.add(newIndex, moved)
                                                displayActivities = mut
                                                dragStepAccumulatedPx -= steps * activityRowHeightPx
                                                dropTargetIndex = newIndex
                                            }
                                        },
                                        onDragEnd = {
                                            dragStepAccumulatedPx = 0f
                                            dragVisualOffsetPx = 0f
                                            draggedActivityId = null
                                            dropTargetIndex = -1
                                            isDraggingReorder = false
                                            coroutineScope.launch {
                                                viewModel.applyActivityOrder(displayActivities)
                                            }
                                        },
                                        onDragCancel = {
                                            dragStepAccumulatedPx = 0f
                                            dragVisualOffsetPx = 0f
                                            draggedActivityId = null
                                            dropTargetIndex = -1
                                            isDraggingReorder = false
                                            displayActivities = activities
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Reordenar",
                                tint = if (isDragged) {
                                    View2Colors.dropLine
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        SwipeToDismissBox(
                            modifier = Modifier.weight(1f),
                            state = dismissState,
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                            gesturesEnabled = draggedActivityId == null && !isDraggingReorder,
                            backgroundContent = {
                                val isLtr = layoutDirection == LayoutDirection.Ltr
                                val swipeDir = dismissState.dismissDirection
                                    ?: dismissState.targetValue
                                when (swipeDir) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        val willBeHabit = !activity.esHabito
                                        val label = if (willBeHabit) "Marcar hábito" else "Quitar hábito"
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = if (isLtr) {
                                                Alignment.CenterStart
                                            } else {
                                                Alignment.CenterEnd
                                            },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Icon(
                                                    imageVector = if (willBeHabit) {
                                                        Icons.Filled.Star
                                                    } else {
                                                        Icons.Outlined.StarOutline
                                                    },
                                                    contentDescription = label,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.errorContainer),
                                            contentAlignment = if (isLtr) {
                                                Alignment.CenterEnd
                                            } else {
                                                Alignment.CenterStart
                                            },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Text(
                                                    text = "Eliminar",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        Box(Modifier.fillMaxSize())
                                    }
                                }
                            },
                            content = {
                                ActivityRow(
                                    activity = activity,
                                    rowIndex = index,
                                    isDragged = isDragged,
                                    onActivityChange = { updated ->
                                        viewModel.insertOrUpdateActivity(updated)
                                    },
                                    requestFocusOnActivityName = focusActivityId == activity.id,
                                    onActivityNameFocusConsumed = { focusActivityId = null },
                                )
                            },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        val newId = viewModel.appendEmptyActivity()
                        focusActivityId = newId
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir actividad")
            }
            Spacer(modifier = Modifier.width(10.dp))

            FilledTonalIconButton(
                onClick = { showDuplicateDatePicker = true },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicar día")
            }
            Spacer(modifier = Modifier.width(10.dp))

            FilledTonalIconButton(
                onClick = {
                    if (activities.isEmpty()) {
                        showDeleteAllEmptyMessage = true
                    } else {
                        showDeleteAllConfirm = true
                    }
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar todas")
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
                                viewModel.resetAllActivitiesExclusive()
                                focusActivityId = null
                                showDeleteAllConfirm = false
                            }
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

        if (showDeleteAllEmptyMessage) {
            AlertDialog(
                onDismissRequest = { showDeleteAllEmptyMessage = false },
                title = { Text("Sin actividades") },
                text = {
                    Text("No hay actividades registradas en el $destLabel para eliminar.")
                },
                confirmButton = {
                    TextButton(onClick = { showDeleteAllEmptyMessage = false }) {
                        Text("OK")
                    }
                },
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
    rowIndex: Int = 0,
    isDragged: Boolean = false,
    requestFocusOnActivityName: Boolean = false,
    onActivityNameFocusConsumed: () -> Unit = {},
) {
    var name by remember(activity.id) { mutableStateOf(activity.name) }
    var duration by remember(activity.id) { mutableStateOf(activity.duration) }
    var start by remember(activity.id) { mutableStateOf(activity.start) }
    var checked by remember(activity.id) { mutableStateOf(activity.checked) }
    var esHabito by remember(activity.id) { mutableStateOf(activity.esHabito) }

    var nameFieldFocused by remember(activity.id) { mutableStateOf(false) }
    var showDurationTimePicker by remember(activity.id) { mutableStateOf(false) }
    var showStartTimePicker by remember(activity.id) { mutableStateOf(false) }
    val nameFocusRequester = remember(activity.id) { FocusRequester() }

    LaunchedEffect(requestFocusOnActivityName, activity.id) {
        if (requestFocusOnActivityName) {
            delay(200)
            runCatching { nameFocusRequester.requestFocus() }
            onActivityNameFocusConsumed()
        }
    }

    // Sync from ViewModel; skip duration/start while a time picker is open.
    LaunchedEffect(
        activity.id,
        activity.name,
        activity.start,
        activity.end,
        activity.duration,
        activity.checked,
        activity.esHabito,
        nameFieldFocused,
        showDurationTimePicker,
        showStartTimePicker
    ) {
        if (!nameFieldFocused) name = activity.name
        if (!showDurationTimePicker) duration = activity.duration
        if (!showStartTimePicker) start = activity.start
        checked = activity.checked
        esHabito = activity.esHabito
    }

    val rowFieldHeight = 20.dp
    val nameTextAlpha = if (esHabito) 1f else 0.55f

    val rowShape = RoundedCornerShape(5.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(View2Colors.stripeForIndex(rowIndex), rowShape)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = rowShape,
            )
            .then(
                if (isDragged) Modifier.shadow(4.dp, rowShape, clip = false) else Modifier,
            ),
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
            textAlpha: Float = 1f,
        ) {
            Box(
                modifier = modifier
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(2.dp))
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
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
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(2.dp))
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

        Box(
            modifier = Modifier
                .weight(2.4f)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(2.dp))
                .height(rowFieldHeight),
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = nameTextAlpha),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester)
                    .onFocusChanged { fs ->
                        if (nameFieldFocused && !fs.isFocused) {
                            onActivityChange(activity.copy(name = name, esHabito = esHabito))
                        }
                        nameFieldFocused = fs.isFocused
                    }
                    .padding(start = 2.dp, end = 16.dp, top = 1.dp, bottom = 1.dp),
            )
            Icon(
                imageVector = if (esHabito) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (esHabito) "Hábito (KPI)" else "No es hábito",
                tint = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (esHabito) 0.75f else 0.35f,
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .size(12.dp),
            )
        }
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
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
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