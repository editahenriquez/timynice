package com.example.timynice

import com.example.timynice.room.ActivityEntity
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun parseLocalTimeOrMidnight(value: String): LocalTime =
    runCatching { LocalTime.parse(value.trim(), timeFormatter) }.getOrDefault(LocalTime.MIDNIGHT)

private fun durationToSeconds(duration: String): Long {
    val parts = duration.split(":")
    if (parts.size < 2) return 0L
    val h = parts[0].toIntOrNull() ?: 0
    val m = parts[1].toIntOrNull() ?: 0
    return h * 3600L + m * 60L
}

private fun formatTime(t: LocalTime): String = t.format(timeFormatter)

/**
 * Recomputes a gapless timeline in the given list order (no reordering by start time).
 *
 * - First row: anchor — keeps its current [ActivityEntity.start] as the chain origin.
 * - Every other row: start = previous row's end; end = start + duration.
 *
 * Used after manual drag-and-drop; persisted start/end values imply display order on reload
 * (activities are loaded ORDER BY start ASC).
 */
fun normalizeActivitiesContinuity(activities: List<ActivityEntity>): List<ActivityEntity> {
    if (activities.isEmpty()) return emptyList()
    val out = ArrayList<ActivityEntity>(activities.size)
    var prevEnd: LocalTime? = null
    activities.forEachIndexed { index, act ->
        val startTime = if (index == 0) {
            parseLocalTimeOrMidnight(act.start)
        } else {
            prevEnd!!
        }
        val endTime = startTime.plusSeconds(durationToSeconds(act.duration))
        out.add(
            act.copy(
                start = formatTime(startTime),
                end = formatTime(endTime),
            ),
        )
        prevEnd = endTime
    }
    return out
}
