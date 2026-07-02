package com.holadeutsch.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Per-word learning state (Leitner box 1..5, 5 = mastered). */
@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val wordId: Int,
    val box: Int = 1,
    val timesSeen: Int = 0,
    val timesCorrect: Int = 0,
    val lastReviewedEpoch: Long = 0
)
