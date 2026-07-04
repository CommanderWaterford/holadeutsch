package com.holadeutsch.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holadeutsch.app.core.tts.GermanTts
import com.holadeutsch.app.data.model.Word
import com.holadeutsch.app.data.repo.Stats
import com.holadeutsch.app.data.repo.StatsRepository
import com.holadeutsch.app.data.repo.WordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val statsRepository: StatsRepository,
    private val wordRepository: WordRepository,
    val tts: GermanTts
) : ViewModel() {

    val stats: StateFlow<Stats> = statsRepository.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())

    var wordOfDay by mutableStateOf<Word?>(null)
        private set

    init {
        viewModelScope.launch {
            val words = wordRepository.getWords()
            if (words.isNotEmpty()) {
                // Deterministic "word of the day": rotates through the deck daily.
                wordOfDay = words[(LocalDate.now().toEpochDay() % words.size).toInt()]
            }
        }
    }

    fun setNivel(nivel: Int) = viewModelScope.launch {
        statsRepository.setSelectedNivel(nivel)
    }

    fun speak(text: String) = tts.speak(text)
}
