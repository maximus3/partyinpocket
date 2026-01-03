package com.m3games.partyinpocket.presentation.screens.settings

import androidx.lifecycle.ViewModel
import com.m3games.partyinpocket.data.SettingsRepository
import com.m3games.partyinpocket.domain.model.AiSettings
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val aiSettings: StateFlow<AiSettings> = settingsRepository.aiSettings

    fun saveAiSettings(settings: AiSettings) {
        settingsRepository.saveAiSettings(settings)
    }
}
