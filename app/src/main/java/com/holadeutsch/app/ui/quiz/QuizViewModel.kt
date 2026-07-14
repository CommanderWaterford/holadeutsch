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
import com.holadeutsch.app.domain.RewardPolicy
import com.holadeutsch.app.domain.SessionAnswer
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
    val hint: String? = null,
    val lastResult: AnswerResult? = null,
    val correctCount: Int = 0,
    val answers: List<SessionAnswer> = emptyList(),
    val rewardedWordIdsToday: Set<Int> = emptySet(),
    val lastAwardedXp: Int = 0,
    val sentenceTokenIds: List<Int> = emptyList(),
    val sentenceHadError: Boolean = false,
    val sentenceError: Boolean = false,
    val successSoundEvent: Int = 0,
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
                    rewardedWordIdsToday = stats.rewardedWordIdsToday,
                    questions = engine.buildSession(
                        words = sessionWords,
                        progress = progress,
                        today = LocalDate.now().toEpochDay(),
                        size = if (onlyIds.isEmpty()) normalSessionSize(stats) else sessionWords.size,
                        distractorPool = nivelWords.ifEmpty { sessionWords }
                    )
                )
            }
        }
    }

    fun onTypedChange(value: String) = _ui.update { it.copy(typedInput = value) }

    /** Reveals a few letters of the current typed answer; one hint per question. */
    fun revealHint() {
        val state = _ui.value
        if (state.answered || state.hint != null) return
        val q = state.current as? Question.Typed ?: return
        _ui.update { it.copy(hint = engine.buildHint(q.word)) }
    }

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

    fun addSentenceToken(tokenId: Int) = _ui.update { state ->
        val question = state.current as? Question.SentenceBuilder ?: return@update state
        if (state.answered || tokenId in state.sentenceTokenIds ||
            question.tokens.none { it.id == tokenId }
        ) {
            state
        } else {
            state.copy(
                sentenceTokenIds = state.sentenceTokenIds + tokenId,
                sentenceError = false
            )
        }
    }

    fun removeSentenceToken(tokenId: Int) = _ui.update { state ->
        if (state.answered) state else state.copy(
            sentenceTokenIds = state.sentenceTokenIds - tokenId,
            sentenceError = false
        )
    }

    fun moveSentenceToken(from: Int, to: Int) = _ui.update { state ->
        if (state.answered || from !in state.sentenceTokenIds.indices ||
            to !in state.sentenceTokenIds.indices || from == to
        ) {
            state
        } else {
            val reordered = state.sentenceTokenIds.toMutableList().apply {
                add(to, removeAt(from))
            }
            state.copy(sentenceTokenIds = reordered, sentenceError = false)
        }
    }

    fun submitSentence() {
        val state = _ui.value
        val question = state.current as? Question.SentenceBuilder ?: return
        if (state.answered || state.sentenceTokenIds.size != question.tokens.size) return

        val result = engine.checkSentence(question, state.sentenceTokenIds, state.sentenceHadError)
        if (result == AnswerResult.WRONG) {
            _ui.update { it.copy(sentenceHadError = true, sentenceError = true) }
            return
        }

        val firstAttempt = result == AnswerResult.CORRECT
        if (firstAttempt) {
            _ui.update { it.copy(successSoundEvent = it.successSoundEvent + 1) }
        }
        applyResult(result = result, selectedIndex = null)
    }

    private fun applyResult(result: AnswerResult, selectedIndex: Int?) {
        val q = _ui.value.current ?: return
        val counted = result != AnswerResult.WRONG
        val alreadyRewarded = q.word.id in _ui.value.rewardedWordIdsToday ||
            _ui.value.answers.any {
                it.wordId == q.word.id && it.result != AnswerResult.WRONG
            }
        val awardedXp = if (alreadyRewarded) 0 else RewardPolicy.answerXp(result)
        _ui.update {
            it.copy(
                lastResult = result,
                selectedIndex = selectedIndex,
                lastAwardedXp = awardedXp,
                correctCount = it.correctCount + if (counted) 1 else 0,
                wrongWordIds = if (counted) it.wrongWordIds else it.wrongWordIds + q.word.id,
                answers = it.answers + SessionAnswer(q.word.id, result)
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
                    answers = state.answers
                )
                _ui.update { it.copy(outcome = outcome) }
            }
        } else {
            _ui.update {
                it.copy(
                    index = it.index + 1,
                    selectedIndex = null,
                    typedInput = "",
                    hint = null,
                    lastResult = null,
                    lastAwardedXp = 0,
                    sentenceTokenIds = emptyList(),
                    sentenceHadError = false,
                    sentenceError = false
                )
            }
        }
    }

    fun speak(text: String) = tts.speak(text)

    companion object {
        const val DEFAULT_SESSION_SIZE = 10

        /** Finishes the remaining daily target without creating marathon sessions. */
        fun normalSessionSize(stats: com.holadeutsch.app.data.repo.Stats): Int {
            val remaining = (stats.dailyGoal - stats.wordsToday).coerceAtLeast(0)
            return if (remaining == 0) DEFAULT_SESSION_SIZE else minOf(DEFAULT_SESSION_SIZE, remaining)
        }
    }
}
