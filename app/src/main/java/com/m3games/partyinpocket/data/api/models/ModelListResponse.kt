package com.m3games.partyinpocket.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Универсальный DTO под ответ /models у всех провайдеров.
 *
 * - OpenRouter: возвращает name + pricing.{prompt,completion} в копейках за токен.
 * - OpenAI: возвращает только id (display_name отсутствует, name отсутствует).
 * - Anthropic: возвращает display_name + id + type.
 *
 * Поля делаем nullable, чтобы не падать на отсутствующих ключах у разных провайдеров.
 */
@Serializable
data class ModelListResponse(
    val data: List<ApiModelDto> = emptyList()
)

@Serializable
data class ApiModelDto(
    val id: String,
    val name: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val pricing: ModelPricingDto? = null,
    val architecture: ModelArchitectureDto? = null
)

@Serializable
data class ModelPricingDto(
    val prompt: String? = null,
    val completion: String? = null
)

@Serializable
data class ModelArchitectureDto(
    @SerialName("output_modalities")
    val outputModalities: List<String>? = null
)
