package com.holadeutsch.app.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holadeutsch.app.core.notifications.ReminderScheduler
import com.holadeutsch.app.data.repo.StatsRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val statsRepository: StatsRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    var name by mutableStateOf("")
    var dailyGoal by mutableIntStateOf(10)

    val canFinish: Boolean get() = name.isNotBlank()

    fun finish(notificationPermissionGranted: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        statsRepository.completeOnboarding(
            name = name,
            dailyGoal = dailyGoal,
            reminderEnabled = notificationPermissionGranted
        )
        if (notificationPermissionGranted) {
            reminderScheduler.schedule(DEFAULT_REMINDER_HOUR, DEFAULT_REMINDER_MINUTE)
        }
        onDone()
    }

    private companion object {
        const val DEFAULT_REMINDER_HOUR = 19
        const val DEFAULT_REMINDER_MINUTE = 0
    }
}
