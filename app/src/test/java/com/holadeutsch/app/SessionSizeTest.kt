package com.holadeutsch.app

import com.holadeutsch.app.data.repo.Stats
import com.holadeutsch.app.ui.quiz.QuizViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSizeTest {

    @Test
    fun `normal sessions are capped at ten questions`() {
        assertEquals(10, QuizViewModel.normalSessionSize(Stats(dailyGoal = 50, wordsToday = 0)))
        assertEquals(10, QuizViewModel.normalSessionSize(Stats(dailyGoal = 25, wordsToday = 10)))
    }

    @Test
    fun `last session uses only the remaining daily words`() {
        assertEquals(5, QuizViewModel.normalSessionSize(Stats(dailyGoal = 25, wordsToday = 20)))
    }

    @Test
    fun `practice remains available after completing the goal`() {
        assertEquals(10, QuizViewModel.normalSessionSize(Stats(dailyGoal = 10, wordsToday = 10)))
        assertEquals(10, QuizViewModel.normalSessionSize(Stats(dailyGoal = 10, wordsToday = 20)))
    }
}
