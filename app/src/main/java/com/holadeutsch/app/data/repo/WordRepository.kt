package com.holadeutsch.app.data.repo

import android.content.Context
import com.holadeutsch.app.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Loads the bundled 100-word deck from assets/words.json (once, then cached in memory). */
class WordRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: List<Word>? = null

    suspend fun getWords(): List<Word> = cache ?: mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            context.assets.open("words.json").bufferedReader().use { it.readText() }
                .let { json.decodeFromString<List<Word>>(it) }
        }.also { cache = it }
    }
}
