package com.m3games.partyinpocket.data.api

import android.util.Log
import com.m3games.partyinpocket.data.api.models.ApiModelDto
import com.m3games.partyinpocket.data.api.models.ModelListResponse
import com.m3games.partyinpocket.domain.model.AiModel
import com.m3games.partyinpocket.domain.model.AiProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ModelListService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Загружает список моделей у указанного провайдера.
     *
     * Для OpenRouter с включённым freeOnly:
     *  - дёргаем /models/user (нужен токен)
     *  - фильтруем по id, кончающемуся на ":free", либо по нулевому pricing
     *
     * Для Custom — пытаемся вывести URL списка моделей из customBaseUrl
     * (заменяем /chat/completions → /models). Если не получается — возвращаем failure.
     */
    suspend fun fetchModels(
        provider: AiProvider,
        customBaseUrl: String,
        token: String,
        freeOnly: Boolean
    ): Result<List<AiModel>> {
        val targetUrl = when {
            provider == AiProvider.OpenRouter && freeOnly -> provider.userModelsUrl
            provider == AiProvider.Custom -> deriveCustomModelsUrl(customBaseUrl)
            else -> provider.modelsUrl
        } ?: return Result.failure(IllegalStateException("Не удалось определить URL для списка моделей"))

        if (provider == AiProvider.OpenRouter && freeOnly && token.isBlank()) {
            return Result.failure(IllegalStateException("Для загрузки free-моделей OpenRouter нужен токен"))
        }
        if (provider != AiProvider.OpenRouter && token.isBlank()) {
            return Result.failure(IllegalStateException("Для загрузки моделей нужен API токен"))
        }

        return try {
            val response = client.get(targetUrl) {
                headers {
                    if (token.isNotBlank()) {
                        when (provider) {
                            AiProvider.Anthropic -> {
                                append("x-api-key", token)
                                append("anthropic-version", "2023-06-01")
                            }
                            else -> {
                                append("Authorization", "Bearer $token")
                            }
                        }
                    }
                }
            }

            if (!response.status.value.toString().startsWith("2")) {
                Log.e("ModelListService", "HTTP ${response.status.value} from $targetUrl")
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }

            val parsed: ModelListResponse = response.body()
            val filteredDto = if (provider == AiProvider.OpenRouter && freeOnly) {
                // Строгий фильтр для free-режима OpenRouter:
                // 1) id обязан оканчиваться на :free (иначе модель платная)
                // 2) среди output-модальностей должен быть text — мы парсим только текстовый ответ
                parsed.data.filter { dto ->
                    dto.id.endsWith(":free") && hasTextOutput(dto)
                }
            } else {
                parsed.data
            }
            val all = filteredDto.map { it.toAiModel() }
            Result.success(all.sortedBy { it.displayName.lowercase() })
        } catch (e: Exception) {
            Log.e("ModelListService", "Ошибка загрузки моделей", e)
            Result.failure(e)
        }
    }

    private fun ApiModelDto.toAiModel(): AiModel {
        val readable = displayName ?: name ?: id
        return AiModel(
            id = id,
            displayName = readable,
            isFree = isFree(this)
        )
    }

    /**
     * Для Custom-провайдера выводим URL списка моделей из baseUrl chat-completions:
     * https://host/v1/chat/completions → https://host/v1/models
     *
     * Если в baseUrl нет /chat/completions — возвращаем null (пусть пользователь введёт модель руками).
     */
    private fun deriveCustomModelsUrl(customBaseUrl: String): String? {
        if (customBaseUrl.isBlank()) return null
        if (!customBaseUrl.contains("/chat/completions")) return null
        return customBaseUrl.replace("/chat/completions", "/models")
    }

    private fun isFree(dto: ApiModelDto): Boolean = dto.id.endsWith(":free")

    /**
     * Модель должна уметь возвращать text. Если поле architecture не пришло
     * (старые ответы или другие провайдеры) — считаем, что text поддерживается.
     */
    private fun hasTextOutput(dto: ApiModelDto): Boolean {
        val modalities = dto.architecture?.outputModalities ?: return true
        return "text" in modalities
    }

    fun close() {
        client.close()
    }
}
