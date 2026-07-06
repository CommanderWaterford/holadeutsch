package com.holadeutsch.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holadeutsch.app.core.notifications.ReminderScheduler
import com.holadeutsch.app.core.tts.GermanTts
import com.holadeutsch.app.data.local.ProgressDao
import com.holadeutsch.app.data.model.Category
import com.holadeutsch.app.data.repo.Stats
import com.holadeutsch.app.data.repo.StatsRepository
import com.holadeutsch.app.data.repo.WordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class CategoryMastery(val category: Category, val fraction: Float)

data class ProgressUiState(
    val stats: Stats = Stats(),
    val masteredCount: Int = 0,
    val totalWords: Int = 0,
    val accuracyPercent: Int? = null,
    val perCategory: List<CategoryMastery> = emptyList()
)

class ProgressViewModel(
    wordRepository: WordRepository,
    private val progressDao: ProgressDao,
    private val statsRepository: StatsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val tts: GermanTts
) : ViewModel() {

    private val words = flow { emit(wordRepository.getWords()) }

    val ui: StateFlow<ProgressUiState> =
        combine(words, progressDao.observeAll(), statsRepository.stats) { all, progress, stats ->
            val w = all.filter { it.nivel == stats.selectedNivel }
            val nivelIds = w.map { it.id }.toSet()
            val nivelProgress = progress.filter { it.wordId in nivelIds }
            val mastery = nivelProgress.associate { it.wordId to it.mastery }
            val seen = nivelProgress.sumOf { it.timesSeen }
            val correct = nivelProgress.sumOf { it.timesCorrect }
            ProgressUiState(
                stats = stats,
                masteredCount = nivelProgress.count { it.isMastered },
                totalWords = w.size,
                accuracyPercent = if (seen > 0) (correct * 100f / seen).roundToInt() else null,
                perCategory = Category.entries.mapNotNull { cat ->
                    val catWords = w.filter { it.category == cat }
                    if (catWords.isEmpty()) null
                    else CategoryMastery(
                        cat,
                        catWords.map { (mastery[it.id] ?: 0) / 5f }.average().toFloat()
                    )
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun setDailyGoal(goal: Int) = viewModelScope.launch { statsRepository.setDailyGoal(goal) }

    fun setTtsEnabled(enabled: Boolean) = viewModelScope.launch {
        statsRepository.setTtsEnabled(enabled)
        tts.enabled = enabled
    }

    fun setHapticsEnabled(enabled: Boolean) =
        viewModelScope.launch { statsRepository.setHapticsEnabled(enabled) }

    fun setReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        statsRepository.setReminderEnabled(enabled)
        val s = statsRepository.stats.first()
        reminderScheduler.apply(enabled, s.reminderHour, s.reminderMinute)
    }

    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        statsRepository.setReminderTime(hour, minute)
        val s = statsRepository.stats.first()
        if (s.reminderEnabled) reminderScheduler.schedule(hour, minute)
    }

    fun resetProgress() = viewModelScope.launch {
        progressDao.clearAll()
        statsRepository.resetProgress()
    }
}
