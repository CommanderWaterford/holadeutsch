package com.holadeutsch.app.domain

import com.holadeutsch.app.data.local.ProgressEntity

/** Lightweight Leitner logic: correct moves the word up a box (max 5), wrong resets to box 1. */
object Srs {
    fun onAnswer(progress: ProgressEntity, correct: Boolean): ProgressEntity = progress.copy(
        box = if (correct) minOf(5, progress.box + 1) else 1,
        timesSeen = progress.timesSeen + 1,
        timesCorrect = progress.timesCorrect + if (correct) 1 else 0,
        lastReviewedEpoch = System.currentTimeMillis()
    )
}

/** Pure streak arithmetic over epoch days, kept separate for unit testing. */
object Streak {
    fun next(lastActiveDay: Long, today: Long, current: Int): Int = when {
        lastActiveDay == today -> maxOf(current, 1)
        lastActiveDay == today - 1 -> current + 1
        else -> 1
    }
}
