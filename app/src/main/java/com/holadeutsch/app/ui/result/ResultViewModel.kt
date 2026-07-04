package com.holadeutsch.app.ui.result

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holadeutsch.app.core.tts.GermanTts
import com.holadeutsch.app.data.model.Word
import com.holadeutsch.app.data.repo.WordRepository
import kotlinx.coroutines.launch

/** Resolves the ids of wrongly answered words (passed via navigation) back into words. */
class ResultViewModel(
    savedStateHandle: SavedStateHandle,
    wordRepository: WordRepository,
    val tts: GermanTts
) : ViewModel() {

    var wrongWords by mutableStateOf<List<Word>>(emptyList())
        private set

    init {
        val ids = savedStateHandle.get<String>("wrongIds")
            .orEmpty()
            .split(",")
            .mapNotNull { it.toIntOrNull() }
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                val byId = wordRepository.getWords().associateBy { it.id }
                wrongWords = ids.mapNotNull { byId[it] }
            }
        }
    }

    fun speak(text: String) = tts.speak(text)
}
