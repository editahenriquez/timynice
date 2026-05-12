package com.example.timynice

import com.example.timynice.room.ActivityEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun normalize_snaps_gap_sleep_start_to_previous_end() {
        val funA = ActivityEntity(
            id = "1", dayId = "2026-05-11", name = "Fun",
            duration = "00:40", start = "04:00", end = "04:40"
        )
        val eat = ActivityEntity(
            id = "2", dayId = "2026-05-11", name = "Eat",
            duration = "00:20", start = "04:40", end = "05:00"
        )
        val sleep = ActivityEntity(
            id = "3", dayId = "2026-05-11", name = "Sleep",
            duration = "00:20", start = "05:10", end = "05:30"
        )
        val out = normalizeActivitiesContinuity(listOf(sleep, funA, eat))
        assertEquals("04:00", out[0].start)
        assertEquals("04:40", out[1].start)
        assertEquals("05:00", out[2].start)
        assertEquals("05:20", out[2].end)
    }
}