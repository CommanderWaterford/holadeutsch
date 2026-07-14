package com.holadeutsch.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.holadeutsch.app.HolaDeutschApp
import com.holadeutsch.app.ui.browse.BrowseViewModel
import com.holadeutsch.app.ui.home.HomeViewModel
import com.holadeutsch.app.ui.onboarding.OnboardingViewModel
import com.holadeutsch.app.ui.progress.ProgressViewModel
import com.holadeutsch.app.ui.quiz.QuizViewModel
import com.holadeutsch.app.ui.result.ResultViewModel
import com.holadeutsch.app.ui.vocab.VocabViewModel

/** Central ViewModel factory wired to the app's manual DI container. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val c = holaApp().container
            HomeViewModel(c.statsRepository, c.wordRepository, c.germanTts)
        }
        initializer {
            val c = holaApp().container
            OnboardingViewModel(c.statsRepository, c.reminderScheduler)
        }
        initializer {
            val c = holaApp().container
            VocabViewModel(c.wordRepository, c.germanTts)
        }
        initializer {
            val c = holaApp().container
            QuizViewModel(createSavedStateHandle(), c.wordRepository, c.progressDao, c.statsRepository, c.germanTts)
        }
        initializer {
            val c = holaApp().container
            BrowseViewModel(c.wordRepository, c.progressDao, c.statsRepository, c.germanTts)
        }
        initializer {
            val c = holaApp().container
            ProgressViewModel(
                c.wordRepository,
                c.progressDao,
                c.statsRepository,
                c.reminderScheduler,
                c.germanTts
            )
        }
        initializer {
            val c = holaApp().container
            ResultViewModel(createSavedStateHandle(), c.wordRepository, c.germanTts)
        }
    }
}

private fun CreationExtras.holaApp(): HolaDeutschApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HolaDeutschApp
