package com.holadeutsch.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.holadeutsch.app.core.tts.GermanTts
import com.holadeutsch.app.data.local.HolaDeutschDatabase
import com.holadeutsch.app.data.repo.StatsRepository
import com.holadeutsch.app.data.repo.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HolaDeutschApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Keep the TTS gate in sync with the user setting.
        appScope.launch {
            container.statsRepository.stats.collect { container.germanTts.enabled = it.ttsEnabled }
        }
    }
}

/** Simple manual dependency container (no DI framework needed at this size). */
class AppContainer(context: Context) {

    private val database = Room.databaseBuilder(
        context,
        HolaDeutschDatabase::class.java,
        "holadeutsch.db"
    ).build()

    val progressDao = database.progressDao()
    val wordRepository = WordRepository(context)
    val statsRepository = StatsRepository(context)
    val germanTts = GermanTts(context)
}
