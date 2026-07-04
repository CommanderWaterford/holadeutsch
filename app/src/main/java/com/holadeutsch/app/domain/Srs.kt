package com.holadeutsch.app.domain

import com.holadeutsch.app.data.local.ProgressEntity
import kotlin.math.roundToInt

/**
 * SuperMemo-2 scheduling.
 *
 * Quality grades: 5 = correct, 3 = correct with hesitation/typo (partial), 1 = wrong.
 * A grade >= 3 advances the repetition chain (intervals 1, 6, then interval * EF days);
 * below 3 resets the chain to a 1-day interval. The easiness factor is always updated
 * and floored at 1.3, per the original algorithm.
 */
object Sm2 {

    fun onAnswer(progress: ProgressEntity, quality: Int, today: Long): ProgressEntity {
        val q = quality.coerceIn(0, 5)
        val easiness = maxOf(
            1.3,
            progress.easiness + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        )
        val now = System.currentTimeMillis()
        return if (q >= 3) {
            val repetitions = progress.repetitions + 1
            val interval = when (repetitions) {
                1 -> 1
                2 -> 6
                else -> (progress.intervalDays * easiness).roundToInt().coerceAtLeast(1)
            }
            progress.copy(
                easiness = easiness,
                repetitions = repetitions,
                intervalDays = interval,
                dueEpochDay = today + interval,
                timesSeen = progress.timesSeen + 1,
                timesCorrect = progress.timesCorrect + 1,
                lastReviewedEpoch = now
            )
        } else {
            progress.copy(
                easiness = easiness,
                repetitions = 0,
                intervalDays = 1,
                dueEpochDay = today + 1,
                timesSeen = progress.timesSeen + 1,
                lastReviewedEpoch = now
            )
        }
    }

    fun qualityOf(result: AnswerResult): Int = when (result) {
        AnswerResult.CORRECT -> 5
        AnswerResult.PARTIAL -> 3
        AnswerResult.WRONG -> 1
    }
}

/** Pure streak arithmetic over epoch days, kept separate for unit testing. */
object Streak {
    fun next(lastActiveDay: Long, today: Long, current: Int): Int = when {
        lastActiveDay == today -> maxOf(current, 1)
        lastActiveDay == today - 1 -> current + 1
        else -> 1
    }
}
