package com.m3games.partyinpocket.domain.model

data class AiSettings(
    val provider: AiProvider = AiProvider.OpenRouter,
    val baseUrl: String = AiProvider.OpenRouter.defaultBaseUrl,
    val model: String = AiProvider.OpenRouter.defaultModel,
    val token: String = "",
    val useFreeOnly: Boolean = true
)
