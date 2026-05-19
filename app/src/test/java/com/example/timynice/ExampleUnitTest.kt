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
    fun normalize_first_row_is_anchor_others_chain_without_reordering_by_start() {
        val piano = ActivityEntity(
            id = "1", dayId = "2026-05-11", name = "play piano",
            duration = "01:00", start = "06:00", end = "07:00",
        )
        val german = ActivityEntity(
            id = "2", dayId = "2026-05-11", name = "learn german",
            duration = "00:30", start = "07:00", end = "07:30",
        )
        val read = ActivityEntity(
            id = "3", dayId = "2026-05-11", name = "read ai",
            duration = "00:30", start = "07:30", end = "08:00",
        )
        val run = ActivityEntity(
            id = "4", dayId = "2026-05-11", name = "run 5k",
            duration = "00:45", start = "08:00", end = "08:45",
        )
        // Scenario 1: run 5k dragged to top — anchor is run's original start
        val scenario1 = normalizeActivitiesContinuity(listOf(run, piano, german, read))
        assertEquals(
            listOf("run 5k", "play piano", "learn german", "read ai"),
            scenario1.map { it.name },
        )
        assertEquals("08:00", scenario1[0].start)
        assertEquals("08:45", scenario1[1].start)
        assertEquals("09:45", scenario1[2].start)
        assertEquals("10:15", scenario1[3].start)

        // Scenario 2: learn german second (after run)
        val scenario2 = normalizeActivitiesContinuity(listOf(run, german, piano, read))
        assertEquals(
            listOf("run 5k", "learn german", "play piano", "read ai"),
            scenario2.map { it.name },
        )
        assertEquals("08:45", scenario2[1].start)

        // Scenario 3: play piano at end
        val scenario3 = normalizeActivitiesContinuity(listOf(run, german, read, piano))
        assertEquals(
            listOf("run 5k", "learn german", "read ai", "play piano"),
            scenario3.map { it.name },
        )
        assertEquals("09:45", scenario3[2].end)
        assertEquals("09:45", scenario3[3].start)
    }
}