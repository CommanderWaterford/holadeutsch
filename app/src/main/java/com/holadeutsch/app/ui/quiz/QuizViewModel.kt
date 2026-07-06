package com.holadeutsch.app.ui.quiz

import androidx.lifecycle.SavedStateHandle
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
import com.holadeutsch.app.domain.Sm2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class QuizUiState(
    val loading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selectedIndex: Int? = null,
    val typedInput: String = "",
    val lastResult: AnswerResult? = null,
    val correctCount: Int = 0,
    val xp: Int = 0,
    val wrongWordIds: List<Int> = emptyList(),
    val outcome: SessionOutcome? = null,
    val hapticsEnabled: Boolean = true
) {
    val current: Question? get() = questions.getOrNull(index)
    val answered: Boolean get() = lastResult != null
    val isLast: Boolean get() = index == questions.lastIndex
}

class QuizViewModel(
    savedStateHandle: SavedStateHandle,
    private val wordRepository: WordRepository,
    private val progressDao: ProgressDao,
    private val statsRepository: StatsRepository,
    val tts: GermanTts
) : ViewModel() {

    private val engine = QuizEngine()
    private val _ui = MutableStateFlow(QuizUiState())
    val ui: StateFlow<QuizUiState> = _ui.asStateFlow()

    init {
        // Optional "wordIds" nav argument: practice exactly these words (e.g. mistakes).
        val onlyIds = savedStateHandle.get<String>("wordIds")
            .orEmpty()
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .toSet()
        viewModelScope.launch {
            val stats = statsRepository.stats.first()
            val nivelWords = wordRepository.getWords(stats.selectedNivel)
            val sessionWords =
                if (onlyIds.isEmpty()) nivelWords
                else wordRepository.getWords().filter { it.id in onlyIds }
            val progress = progressDao.getAllOnce().associateBy { it.wordId }
            _ui.update {
                it.copy(
                    loading = false,
                    hapticsEnabled = stats.hapticsEnabled,
                    questions = engine.buildSession(
                        words = sessionWords,
                        progress = progress,
                        today = LocalDate.now().toEpochDay(),
                        size = if (onlyIds.isEmpty()) stats.dailyGoal else sessionWords.size,
                        distractorPool = nivelWords.ifEmpty { sessionWords }
                    )
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
                wrongWordIds = if (counted) it.wrongWordIds else it.wrongWordIds + q.word.id,
                xp = it.xp + when (result) {
                    AnswerResult.CORRECT -> 10
                    AnswerResult.PARTIAL -> 5
                    AnswerResult.WRONG -> 0
                }
            )
        }
        viewModelScope.launch {
            val existing = progressDao.get(q.word.id) ?: ProgressEntity(wordId = q.word.id)
            progressDao.upsert(
                Sm2.onAnswer(existing, Sm2.qualityOf(result), LocalDate.now().toEpochDay())
            )
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
}
