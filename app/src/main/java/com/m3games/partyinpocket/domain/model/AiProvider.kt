package com.m3games.partyinpocket.domain.model

/**
 * Провайдер ИИ для генерации контента. Знает зашитые URL для chat-completions и
 * для списка моделей, а также модель по умолчанию.
 *
 * Кастомный провайдер позволяет указать собственный baseUrl и модель вручную —
 * у него нет ручки списка моделей.
 */
sealed class AiProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val modelsUrl: String?,
    val userModelsUrl: String?,
    val defaultModel: String,
    val supportsFreeFilter: Boolean
) {
    data object OpenRouter : AiProvider(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1/chat/completions",
        modelsUrl = "https://openrouter.ai/api/v1/models",
        userModelsUrl = "https://openrouter.ai/api/v1/models/user",
        defaultModel = "mistralai/devstral-2512:free",
        supportsFreeFilter = true
    )

    data object OpenAI : AiProvider(
        id = "openai",
        displayName = "ChatGPT",
        defaultBaseUrl = "https://api.openai.com/v1/chat/completions",
        modelsUrl = "https://api.openai.com/v1/models",
        userModelsUrl = null,
        defaultModel = "gpt-4o-mini",
        supportsFreeFilter = false
    )

    data object Anthropic : AiProvider(
        id = "anthropic",
        displayName = "Claude",
        defaultBaseUrl = "https://api.anthropic.com/v1/messages",
        modelsUrl = "https://api.anthropic.com/v1/models",
        userModelsUrl = null,
        defaultModel = "claude-sonnet-4-5",
        supportsFreeFilter = false
    )

    data object Custom : AiProvider(
        id = "custom",
        displayName = "Custom",
        defaultBaseUrl = "",
        modelsUrl = null,
        userModelsUrl = null,
        defaultModel = "",
        supportsFreeFilter = false
    )

    companion object {
        val all: List<AiProvider> by lazy {
            listOf(OpenRouter, OpenAI, Anthropic, Custom)
        }

        fun byId(id: String): AiProvider = all.find { it.id == id } ?: OpenRouter
    }
}
