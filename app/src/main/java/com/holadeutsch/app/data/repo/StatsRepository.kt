package com.holadeutsch.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.holadeutsch.app.domain.RewardPolicy
import com.holadeutsch.app.domain.SessionAnswer
import com.holadeutsch.app.domain.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.sqrt

private val Context.statsStore by preferencesDataStore(name = "stats")

data class Stats(
    val totalXp: Int = 0,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val dailyGoal: Int = 10,
    val wordsToday: Int = 0,
    val selectedNivel: Int = 1,
    val ttsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val activeDays: Set<Long> = emptySet(),
    val rewardedWordIdsToday: Set<Int> = emptySet(),
    val userName: String = "",
    val onboardingDone: Boolean = false
) {
    val level: Int get() = floor(sqrt(totalXp / 100.0)).toInt() + 1
    private val levelStartXp: Int get() = (level - 1) * (level - 1) * 100
    private val nextLevelXp: Int get() = level * level * 100
    val levelProgress: Float
        get() = ((totalXp - levelStartXp).toFloat() / (nextLevelXp - levelStartXp)).coerceIn(0f, 1f)
}

data class SessionOutcome(
    val xpAwarded: Int,
    val goalJustMet: Boolean,
    val perfect: Boolean,
    val streak: Int
)

class StatsRepository(context: Context) {

    private val store = context.statsStore

    private object Keys {
        val TOTAL_XP = intPreferencesKey("total_xp")
        val STREAK = intPreferencesKey("streak")
        val LONGEST = intPreferencesKey("longest_streak")
        val LAST_ACTIVE = longPreferencesKey("last_active_day")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val WORDS_TODAY = intPreferencesKey("words_today")
        val WORDS_DAY = longPreferencesKey("words_day")
        val PRACTICED_WORD_IDS = stringSetPreferencesKey("practiced_word_ids")
        val REWARDED_WORD_IDS = stringSetPreferencesKey("rewarded_word_ids")
        val NIVEL = intPreferencesKey("selected_nivel")
        val TTS = booleanPreferencesKey("tts_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val REMINDER_ON = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val ACTIVE_DAYS = stringSetPreferencesKey("active_days")
        val USER_NAME = stringPreferencesKey("user_name")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val stats: Flow<Stats> = store.data.map { p ->
        val today = LocalDate.now().toEpochDay()
        val lastActive = p[Keys.LAST_ACTIVE] ?: 0L
        val isToday = (p[Keys.WORDS_DAY] ?: -1L) == today
        Stats(
            totalXp = p[Keys.TOTAL_XP] ?: 0,
            // A streak is only alive if the user practiced today or yesterday.
            streak = if (lastActive >= today - 1) (p[Keys.STREAK] ?: 0) else 0,
            longestStreak = p[Keys.LONGEST] ?: 0,
            dailyGoal = p[Keys.DAILY_GOAL] ?: 10,
            wordsToday = if (isToday) (p[Keys.WORDS_TODAY] ?: 0) else 0,
            selectedNivel = (p[Keys.NIVEL] ?: 1).coerceIn(1, 3),
            ttsEnabled = p[Keys.TTS] ?: true,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            reminderEnabled = p[Keys.REMINDER_ON] ?: false,
            reminderHour = (p[Keys.REMINDER_HOUR] ?: 19).coerceIn(0, 23),
            reminderMinute = (p[Keys.REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
            activeDays = (p[Keys.ACTIVE_DAYS] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet(),
            rewardedWordIdsToday = if (isToday) {
                (p[Keys.REWARDED_WORD_IDS] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
            } else {
                emptySet()
            },
            userName = p[Keys.USER_NAME] ?: "",
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false
        )
    }

    /**
     * Records a finished quiz session. Each distinct word counts toward today's goal once,
     * and each successfully answered word awards answer XP at most once per day.
     */
    suspend fun completeSession(answers: List<SessionAnswer>): SessionOutcome {
        var outcome = SessionOutcome(0, goalJustMet = false, perfect = false, streak = 0)
        store.edit { p ->
            val today = LocalDate.now().toEpochDay()
            val streak = Streak.next(p[Keys.LAST_ACTIVE] ?: 0L, today, p[Keys.STREAK] ?: 0)
            p[Keys.STREAK] = streak
            p[Keys.LONGEST] = maxOf(streak, p[Keys.LONGEST] ?: 0)
            p[Keys.LAST_ACTIVE] = today

            val isToday = (p[Keys.WORDS_DAY] ?: -1L) == today
            val practicedBefore = if (isToday) {
                (p[Keys.PRACTICED_WORD_IDS] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
            } else {
                emptySet()
            }
            val rewardedBefore = if (isToday) {
                (p[Keys.REWARDED_WORD_IDS] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
            } else {
                emptySet()
            }
            val reward = RewardPolicy.evaluate(answers, rewardedBefore)
            val practicedAfter = practicedBefore + reward.practicedWordIds
            val rewardedAfter = rewardedBefore + reward.newlyRewardedWordIds

            p[Keys.PRACTICED_WORD_IDS] = practicedAfter.mapTo(mutableSetOf()) { it.toString() }
            p[Keys.REWARDED_WORD_IDS] = rewardedAfter.mapTo(mutableSetOf()) { it.toString() }
            p[Keys.WORDS_TODAY] = practicedAfter.size
            p[Keys.WORDS_DAY] = today

            val goal = p[Keys.DAILY_GOAL] ?: 10
            val before = practicedBefore.size
            val after = practicedAfter.size
            val goalJustMet = before < goal && after >= goal
            var xp = reward.answerXp
            // A repeated perfect round over already-rewarded words cannot farm this bonus.
            if (reward.perfect && reward.newlyRewardedWordIds.isNotEmpty()) xp += 25
            if (goalJustMet) xp += 50
            p[Keys.TOTAL_XP] = (p[Keys.TOTAL_XP] ?: 0) + xp

            // Keep a rolling window of active days for the streak calendar.
            p[Keys.ACTIVE_DAYS] = ((p[Keys.ACTIVE_DAYS] ?: emptySet()) + today.toString())
                .mapNotNull { it.toLongOrNull() }
                .filter { it >= today - 60 }
                .map { it.toString() }
                .toSet()

            outcome = SessionOutcome(xp, goalJustMet, reward.perfect, streak)
        }
        return outcome
    }

    suspend fun setDailyGoal(goal: Int) {
        store.edit { it[Keys.DAILY_GOAL] = goal }
    }

    suspend fun setUserName(name: String) {
        store.edit { it[Keys.USER_NAME] = name.trim() }
    }

    /**
     * Saves the first-run setup and the notification choice in one transaction.
     * Reminders are enabled only after Android has granted notification permission.
     */
    suspend fun completeOnboarding(
        name: String,
        dailyGoal: Int,
        reminderEnabled: Boolean = false
    ) {
        store.edit {
            it[Keys.USER_NAME] = name.trim()
            it[Keys.DAILY_GOAL] = dailyGoal
            it[Keys.REMINDER_ON] = reminderEnabled
            it[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun setSelectedNivel(nivel: Int) {
        store.edit { it[Keys.NIVEL] = nivel.coerceIn(1, 3) }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        store.edit { it[Keys.TTS] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        store.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        store.edit { it[Keys.REMINDER_ON] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        store.edit {
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    /** Clears XP, streak and activity, but keeps user settings (goal, TTS, haptics). */
    suspend fun resetProgress() {
        store.edit { p ->
            p -= Keys.TOTAL_XP
            p -= Keys.STREAK
            p -= Keys.LONGEST
            p -= Keys.LAST_ACTIVE
            p -= Keys.WORDS_TODAY
            p -= Keys.WORDS_DAY
            p -= Keys.PRACTICED_WORD_IDS
            p -= Keys.REWARDED_WORD_IDS
            p -= Keys.ACTIVE_DAYS
        }
    }
}
