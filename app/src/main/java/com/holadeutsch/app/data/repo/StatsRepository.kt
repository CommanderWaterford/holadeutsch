package com.holadeutsch.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    val activeDays: Set<Long> = emptySet()
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
        val NIVEL = intPreferencesKey("selected_nivel")
        val TTS = booleanPreferencesKey("tts_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val ACTIVE_DAYS = stringSetPreferencesKey("active_days")
    }

    val stats: Flow<Stats> = store.data.map { p ->
        val today = LocalDate.now().toEpochDay()
        val lastActive = p[Keys.LAST_ACTIVE] ?: 0L
        Stats(
            totalXp = p[Keys.TOTAL_XP] ?: 0,
            // A streak is only alive if the user practiced today or yesterday.
            streak = if (lastActive >= today - 1) (p[Keys.STREAK] ?: 0) else 0,
            longestStreak = p[Keys.LONGEST] ?: 0,
            dailyGoal = p[Keys.DAILY_GOAL] ?: 10,
            wordsToday = if ((p[Keys.WORDS_DAY] ?: -1L) == today) (p[Keys.WORDS_TODAY] ?: 0) else 0,
            selectedNivel = (p[Keys.NIVEL] ?: 1).coerceIn(1, 3),
            ttsEnabled = p[Keys.TTS] ?: true,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            activeDays = (p[Keys.ACTIVE_DAYS] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
        )
    }

    /**
     * Records a finished quiz session: streak, daily goal and XP (incl. bonuses).
     * @param baseXp XP earned from individual answers (10 per correct, 5 per partial).
     */
    suspend fun completeSession(correct: Int, total: Int, baseXp: Int): SessionOutcome {
        var outcome = SessionOutcome(0, goalJustMet = false, perfect = false, streak = 0)
        store.edit { p ->
            val today = LocalDate.now().toEpochDay()
            val streak = Streak.next(p[Keys.LAST_ACTIVE] ?: 0L, today, p[Keys.STREAK] ?: 0)
            p[Keys.STREAK] = streak
            p[Keys.LONGEST] = maxOf(streak, p[Keys.LONGEST] ?: 0)
            p[Keys.LAST_ACTIVE] = today

            val goal = p[Keys.DAILY_GOAL] ?: 10
            val before = if ((p[Keys.WORDS_DAY] ?: -1L) == today) (p[Keys.WORDS_TODAY] ?: 0) else 0
            val after = before + total
            p[Keys.WORDS_TODAY] = after
            p[Keys.WORDS_DAY] = today

            val goalJustMet = before < goal && after >= goal
            val perfect = total > 0 && correct == total
            var xp = baseXp
            if (perfect) xp += 25
            if (goalJustMet) xp += 50
            p[Keys.TOTAL_XP] = (p[Keys.TOTAL_XP] ?: 0) + xp

            // Keep a rolling window of active days for the streak calendar.
            p[Keys.ACTIVE_DAYS] = ((p[Keys.ACTIVE_DAYS] ?: emptySet()) + today.toString())
                .mapNotNull { it.toLongOrNull() }
                .filter { it >= today - 60 }
                .map { it.toString() }
                .toSet()

            outcome = SessionOutcome(xp, goalJustMet, perfect, streak)
        }
        return outcome
    }

    suspend fun setDailyGoal(goal: Int) {
        store.edit { it[Keys.DAILY_GOAL] = goal }
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

    /** Clears XP, streak and activity, but keeps user settings (goal, TTS, haptics). */
    suspend fun resetProgress() {
        store.edit { p ->
            p -= Keys.TOTAL_XP
            p -= Keys.STREAK
            p -= Keys.LONGEST
            p -= Keys.LAST_ACTIVE
            p -= Keys.WORDS_TODAY
            p -= Keys.WORDS_DAY
            p -= Keys.ACTIVE_DAYS
        }
    }
}
