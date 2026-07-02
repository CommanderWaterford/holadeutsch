package com.holadeutsch.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holadeutsch.app.core.tts.GermanTts
import com.holadeutsch.app.data.local.ProgressDao
import com.holadeutsch.app.data.local.ProgressEntity
import com.holadeutsch.app.data.repo.SessionOutcome
import com.holadeutsch.app.data.repo.StatsRepository
import com.holadeutsch.app.data.repo.WordRepository
import com.holadeutsch.app.domain.AnswerResult
import com.holadeutsch.app.domain.Question
import com.holadeutsch.app.domain.QuizEngine
import com.holadeutsch.app.domain.Srs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val loading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selectedIndex: Int? = null,
    val typedInput: String = "",
    val lastResult: AnswerResult? = null,
    val correctCount: Int = 0,
    val xp: Int = 0,
    val outcome: SessionOutcome? = null,
    val hapticsEnabled: Boolean = true
) {
    val current: Question? get() = questions.getOrNull(index)
    val answered: Boolean get() = lastResult != null
    val isLast: Boolean get() = index == questions.lastIndex
}

class QuizViewModel(
    private val wordRepository: WordRepository,
    private val progressDao: ProgressDao,
    private val statsRepository: StatsRepository,
    val tts: GermanTts
) : ViewModel() {

    private val engine = QuizEngine()
    private val _ui = MutableStateFlow(QuizUiState())
    val ui: StateFlow<QuizUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val words = wordRepository.getWords()
            val boxes = progressDao.getAllOnce().associate { it.wordId to it.box }
            val haptics = statsRepository.stats.first().hapticsEnabled
            _ui.update {
                it.copy(
                    loading = false,
                    hapticsEnabled = haptics,
                    questions = engine.buildSession(words, boxes, SESSION_SIZE)
                )
            }
        }
    }

    fun onTypedChange(value: String) = _ui.update { it.copy(typedInput = value) }

    fun answerChoice(optionIndex: Int) {
        val state = _ui.value
        if (state.answered) return
        val correctIndex = when (val q = state.current) {
            is Question.MultipleChoice -> q.correctIndex
            is Question.ArticleChoice -> q.correctIndex
            else -> return
        }
        applyResult(
            if (optionIndex == correctIndex) AnswerResult.CORRECT else AnswerResult.WRONG,
            optionIndex
        )
    }

    fun submitTyped() {
        val state = _ui.value
        if (state.answered) return
        val q = state.current as? Question.Typed ?: return
        applyResult(engine.checkTyped(q.word, state.typedInput), null)
    }

    private fun applyResult(result: AnswerResult, selectedIndex: Int?) {
        val q = _ui.value.current ?: return
        val counted = result != AnswerResult.WRONG
        _ui.update {
            it.copy(
                lastResult = result,
                selectedIndex = selectedIndex,
                correctCount = it.correctCount + if (counted) 1 else 0,
                xp = it.xp + when (result) {
                    AnswerResult.CORRECT -> 10
                    AnswerResult.PARTIAL -> 5
                    AnswerResult.WRONG -> 0
                }
            )
        }
        viewModelScope.launch {
            val existing = progressDao.get(q.word.id) ?: ProgressEntity(wordId = q.word.id)
            progressDao.upsert(Srs.onAnswer(existing, counted))
        }
    }

    fun next() {
        val state = _ui.value
        if (!state.answered) return
        if (state.isLast) {
            viewModelScope.launch {
                val outcome = statsRepository.completeSession(
                    correct = state.correctCount,
                    total = state.questions.size,
                    baseXp = state.xp
                )
                _ui.update { it.copy(outcome = outcome) }
            }
        } else {
            _ui.update {
                it.copy(index = it.index + 1, selectedIndex = null, typedInput = "", lastResult = null)
            }
        }
    }

    fun speak(text: String) = tts.speak(text)

    companion object {
        const val SESSION_SIZE = 10
    }
}
