package com.m3games.partyinpocket.data

import android.content.Context
import android.content.SharedPreferences
import com.m3games.partyinpocket.domain.model.AiProvider
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
        val providerId = prefs.getString(KEY_PROVIDER, AiProvider.OpenRouter.id)
            ?: AiProvider.OpenRouter.id
        val provider = AiProvider.byId(providerId)
        return AiSettings(
            provider = provider,
            baseUrl = prefs.getString(KEY_BASE_URL, provider.defaultBaseUrl) ?: provider.defaultBaseUrl,
            model = prefs.getString(KEY_MODEL, provider.defaultModel) ?: provider.defaultModel,
            token = prefs.getString(KEY_TOKEN, "") ?: "",
            useFreeOnly = prefs.getBoolean(KEY_USE_FREE_ONLY, true)
        )
    }

    fun saveAiSettings(settings: AiSettings) {
        prefs.edit().apply {
            putString(KEY_PROVIDER, settings.provider.id)
            putString(KEY_BASE_URL, settings.baseUrl)
            putString(KEY_MODEL, settings.model)
            putString(KEY_TOKEN, settings.token)
            putBoolean(KEY_USE_FREE_ONLY, settings.useFreeOnly)
            apply()
        }
        _aiSettings.value = settings
    }

    companion object {
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_TOKEN = "ai_token"
        private const val KEY_USE_FREE_ONLY = "ai_use_free_only"
    }
}
