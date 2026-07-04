package com.holadeutsch.app.data.repo

import android.content.Context
import com.holadeutsch.app.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads the bundled word decks (one asset file per nivel) once, then caches them.
 * The nivel is assigned from the file a word came from.
 */
class WordRepository(private val context: Context) {

    private val files = mapOf(
        1 to "words_n1.json",
        2 to "words_n2.json",
        3 to "words_n3.json"
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: List<Word>? = null

    suspend fun getWords(): List<Word> = cache ?: mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            files.flatMap { (nivel, file) ->
                context.assets.open(file).bufferedReader().use { it.readText() }
                    .let { json.decodeFromString<List<Word>>(it) }
                    .map { it.copy(nivel = nivel) }
            }
        }.also { cache = it }
    }

    suspend fun getWords(nivel: Int): List<Word> = getWords().filter { it.nivel == nivel }
}
