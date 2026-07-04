package com.holadeutsch.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holadeutsch.app.core.tts.GermanTts
import com.holadeutsch.app.data.local.ProgressDao
import com.holadeutsch.app.data.model.Category
import com.holadeutsch.app.data.model.Word
import com.holadeutsch.app.data.repo.StatsRepository
import com.holadeutsch.app.data.repo.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class BrowseUiState(
    val words: List<Word> = emptyList(),
    val mastery: Map<Int, Int> = emptyMap(),
    val query: String = "",
    val category: Category? = null,
    val nivel: Int = 1
)

class BrowseViewModel(
    wordRepository: WordRepository,
    progressDao: ProgressDao,
    statsRepository: StatsRepository,
    val tts: GermanTts
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<Category?>(null)
    private val words = flow { emit(wordRepository.getWords()) }

    val ui: StateFlow<BrowseUiState> = combine(
        words,
        progressDao.observeAll(),
        query,
        category,
        statsRepository.stats
    ) { w, progress, q, c, stats ->
        BrowseUiState(
            words = w
                .filter { it.nivel == stats.selectedNivel }
                .filter { c == null || it.category == c }
                .filter {
                    q.isBlank() ||
                        it.german.contains(q, ignoreCase = true) ||
                        it.spanish.contains(q, ignoreCase = true)
                },
            mastery = progress.associate { it.wordId to it.mastery },
            query = q,
            category = c,
            nivel = stats.selectedNivel
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setCategory(value: Category?) {
        category.value = value
    }

    fun speak(text: String) = tts.speak(text)
}
