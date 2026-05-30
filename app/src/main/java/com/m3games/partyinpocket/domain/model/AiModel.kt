package com.m3games.partyinpocket.domain.model

/**
 * Модель ИИ, доступная для выбора в настройках.
 *
 * @param id строковый идентификатор, используемый в API-запросах
 * @param displayName читабельное название для UI
 * @param isFree модель бесплатна (для фильтра у OpenRouter)
 */
data class AiModel(
    val id: String,
    val displayName: String,
    val isFree: Boolean = false
)
