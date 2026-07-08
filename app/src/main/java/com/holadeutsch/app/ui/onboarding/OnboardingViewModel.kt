package com.holadeutsch.app.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holadeutsch.app.data.repo.StatsRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val statsRepository: StatsRepository
) : ViewModel() {

    var name by mutableStateOf("")
    var dailyGoal by mutableIntStateOf(10)

    val canFinish: Boolean get() = name.isNotBlank()

    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        statsRepository.completeOnboarding(name, dailyGoal)
        onDone()
    }
}
