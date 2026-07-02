package com.holadeutsch.app

import com.holadeutsch.app.domain.Streak
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {

    @Test
    fun `first session ever starts a streak of one`() {
        assertEquals(1, Streak.next(lastActiveDay = 0, today = 100, current = 0))
    }

    @Test
    fun `consecutive day increments the streak`() {
        assertEquals(4, Streak.next(lastActiveDay = 99, today = 100, current = 3))
    }

    @Test
    fun `second session on the same day keeps the streak`() {
        assertEquals(3, Streak.next(lastActiveDay = 100, today = 100, current = 3))
    }

    @Test
    fun `missed day resets the streak to one`() {
        assertEquals(1, Streak.next(lastActiveDay = 90, today = 100, current = 5))
    }
}
