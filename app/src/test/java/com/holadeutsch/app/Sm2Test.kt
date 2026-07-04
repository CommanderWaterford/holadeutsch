package com.holadeutsch.app

import com.holadeutsch.app.data.local.ProgressEntity
import com.holadeutsch.app.domain.Sm2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2Test {

    private val fresh = ProgressEntity(wordId = 1)

    @Test
    fun `first correct answer schedules a one day interval`() {
        val p = Sm2.onAnswer(fresh, quality = 5, today = 100)
        assertEquals(1, p.repetitions)
        assertEquals(1, p.intervalDays)
        assertEquals(101L, p.dueEpochDay)
    }

    @Test
    fun `second correct answer schedules six days`() {
        val once = Sm2.onAnswer(fresh, quality = 5, today = 100)
        val twice = Sm2.onAnswer(once, quality = 5, today = 101)
        assertEquals(2, twice.repetitions)
        assertEquals(6, twice.intervalDays)
        assertEquals(107L, twice.dueEpochDay)
    }

    @Test
    fun `third correct answer multiplies the interval by the easiness factor`() {
        var p = fresh
        p = Sm2.onAnswer(p, 5, 100)
        p = Sm2.onAnswer(p, 5, 101)
        val third = Sm2.onAnswer(p, 5, 107)
        assertEquals(3, third.repetitions)
        // EF grows with each grade-5 answer, so the interval is > 6 * 2.5 base? At minimum > 6.
        assertTrue("interval should grow beyond 6 days, was ${third.intervalDays}", third.intervalDays > 6)
        assertEquals(107L + third.intervalDays, third.dueEpochDay)
    }

    @Test
    fun `wrong answer resets repetitions and schedules tomorrow`() {
        var p = fresh
        p = Sm2.onAnswer(p, 5, 100)
        p = Sm2.onAnswer(p, 5, 101)
        val failed = Sm2.onAnswer(p, quality = 1, today = 107)
        assertEquals(0, failed.repetitions)
        assertEquals(1, failed.intervalDays)
        assertEquals(108L, failed.dueEpochDay)
        assertTrue("easiness should drop after a failure", failed.easiness < p.easiness)
    }

    @Test
    fun `easiness factor never drops below 1_3`() {
        var p = fresh
        repeat(20) { p = Sm2.onAnswer(p, quality = 0, today = 100L + it) }
        assertTrue("EF floor is 1.3, was ${p.easiness}", p.easiness >= 1.3)
    }

    @Test
    fun `partial credit advances the chain but slows easiness growth`() {
        val p = Sm2.onAnswer(fresh, quality = 3, today = 100)
        assertEquals(1, p.repetitions)
        assertTrue("EF should not grow on quality 3", p.easiness <= fresh.easiness)
    }
}
