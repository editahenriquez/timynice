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
 * Sorts by start time ascending (stable: equal starts keep input order).
 * Enforces strict continuity: for every row after the first, start == previous end;
 * each end == start + duration. The first row keeps its (merged) start time as the chain anchor.
 */
fun normalizeActivitiesContinuity(activities: List<ActivityEntity>): List<ActivityEntity> {
    if (activities.isEmpty()) return emptyList()
    // Stable sort by start only: ties keep list order so a row appended at the end
    // (e.g. new activity with same start as previous end / zero duration) stays last.
    val sorted = activities.sortedWith(
        compareBy<ActivityEntity> { parseLocalTimeOrMidnight(it.start) },
    )
    val out = ArrayList<ActivityEntity>(sorted.size)
    var prevEnd: LocalTime? = null
    sorted.forEachIndexed { index, act ->
        val startTime = if (index == 0) {
            parseLocalTimeOrMidnight(act.start)
        } else {
            prevEnd!!
        }
        val endTime = startTime.plusSeconds(durationToSeconds(act.duration))
        out.add(
            act.copy(
                start = formatTime(startTime),
                end = formatTime(endTime)
            )
        )
        prevEnd = endTime
    }
    return out
}
