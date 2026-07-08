package com.holadeutsch.app.ui.vocab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holadeutsch.app.core.tts.GermanTts
import com.holadeutsch.app.data.model.Word
import com.holadeutsch.app.data.repo.WordRepository
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

class VocabViewModel(
    wordRepository: WordRepository,
    val tts: GermanTts
) : ViewModel() {

    /** All words per nivel, sorted alphabetically for a dictionary-style list. */
    var wordsByNivel by mutableStateOf<Map<Int, List<Word>>>(emptyMap())
        private set

    init {
        viewModelScope.launch {
            val collator = Collator.getInstance(Locale.GERMAN)
            wordsByNivel = wordRepository.getWords()
                .groupBy { it.nivel }
                .mapValues { (_, words) ->
                    words.sortedWith(compareBy(collator) { it.germanBare })
                }
        }
    }

    fun speak(text: String) = tts.speak(text)
}
