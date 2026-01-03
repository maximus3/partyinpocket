package com.m3games.partyinpocket.domain.model

data class AiSettings(
    val baseUrl: String = "https://openrouter.ai/api/v1/chat/completions",
    val model: String = "mistralai/devstral-2512:free",
    val token: String = ""
)
