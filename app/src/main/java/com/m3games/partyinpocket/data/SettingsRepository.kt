package com.m3games.partyinpocket.data

import android.content.Context
import android.content.SharedPreferences
import com.m3games.partyinpocket.domain.model.AiSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_settings",
        Context.MODE_PRIVATE
    )

    private val _aiSettings = MutableStateFlow(loadAiSettings())
    val aiSettings: StateFlow<AiSettings> = _aiSettings.asStateFlow()

    private fun loadAiSettings(): AiSettings {
        return AiSettings(
            baseUrl = prefs.getString(KEY_BASE_URL, AiSettings().baseUrl) ?: AiSettings().baseUrl,
            model = prefs.getString(KEY_MODEL, AiSettings().model) ?: AiSettings().model,
            token = prefs.getString(KEY_TOKEN, AiSettings().token) ?: AiSettings().token
        )
    }

    fun saveAiSettings(settings: AiSettings) {
        prefs.edit().apply {
            putString(KEY_BASE_URL, settings.baseUrl)
            putString(KEY_MODEL, settings.model)
            putString(KEY_TOKEN, settings.token)
            apply()
        }
        _aiSettings.value = settings
    }

    companion object {
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_TOKEN = "ai_token"
    }
}
