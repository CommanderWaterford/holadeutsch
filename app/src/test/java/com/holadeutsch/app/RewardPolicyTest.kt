package com.holadeutsch.app

import com.holadeutsch.app.domain.AnswerResult
import com.holadeutsch.app.domain.RewardPolicy
import com.holadeutsch.app.domain.SessionAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardPolicyTest {

    @Test
    fun `each successfully answered word awards xp only once per day`() {
        val answers = listOf(
            SessionAnswer(1, AnswerResult.CORRECT),
            SessionAnswer(2, AnswerResult.PARTIAL),
            SessionAnswer(3, AnswerResult.WRONG)
        )

        val first = RewardPolicy.evaluate(answers, alreadyRewardedWordIds = emptySet())
        assertEquals(15, first.answerXp)
        assertEquals(setOf(1, 2), first.newlyRewardedWordIds)

        val repeated = RewardPolicy.evaluate(answers, alreadyRewardedWordIds = setOf(1, 2))
        assertEquals(0, repeated.answerXp)
        assertTrue(repeated.newlyRewardedWordIds.isEmpty())
    }

    @Test
    fun `daily practice contains distinct words including mistakes`() {
        val reward = RewardPolicy.evaluate(
            answers = listOf(
                SessionAnswer(1, AnswerResult.WRONG),
                SessionAnswer(1, AnswerResult.CORRECT),
                SessionAnswer(2, AnswerResult.WRONG)
            ),
            alreadyRewardedWordIds = emptySet()
        )

        assertEquals(setOf(1, 2), reward.practicedWordIds)
        assertEquals(10, reward.answerXp)
    }

    @Test
    fun `a wrong answer prevents a perfect session`() {
        assertTrue(
            RewardPolicy.evaluate(
                listOf(SessionAnswer(1, AnswerResult.CORRECT)),
                emptySet()
            ).perfect
        )
        assertFalse(
            RewardPolicy.evaluate(
                listOf(
                    SessionAnswer(1, AnswerResult.CORRECT),
                    SessionAnswer(2, AnswerResult.WRONG)
                ),
                emptySet()
            ).perfect
        )
    }
}
