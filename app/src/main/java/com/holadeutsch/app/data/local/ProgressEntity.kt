package com.holadeutsch.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Per-word SuperMemo-2 state: easiness factor, repetition count, interval and due date. */
@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val wordId: Int,
    val easiness: Double = 2.5,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val dueEpochDay: Long = 0,
    val timesSeen: Int = 0,
    val timesCorrect: Int = 0,
    val lastReviewedEpoch: Long = 0
) {
    /** 0..5 dots shown in the UI. */
    val mastery: Int get() = minOf(5, repetitions)

    /** A word is considered mastered once its review interval reaches three weeks. */
    val isMastered: Boolean get() = intervalDays >= 21
}
