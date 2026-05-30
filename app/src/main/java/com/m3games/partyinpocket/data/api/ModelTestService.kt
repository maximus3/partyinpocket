package com.m3games.partyinpocket.data.api

import android.util.Log
import com.m3games.partyinpocket.data.api.models.Message
import com.m3games.partyinpocket.data.api.models.OpenRouterError
import com.m3games.partyinpocket.data.api.models.OpenRouterResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Лёгкий тест-запрос к chat-completions для проверки доступности модели и
 * замера времени ответа. Тратит минимум токенов — просим сказать одну букву.
 *
 * Поддерживает OpenAI-совместимый формат (OpenRouter, OpenAI, Custom).
 * Для Anthropic формат запроса другой — этот сервис не сработает,
 * но и Test All у нас включается только в free-режиме OpenRouter.
 */
class ModelTestService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }

    sealed class Outcome {
        data class Success(val durationMs: Long) : Outcome()
        data class Failure(val message: String) : Outcome()
    }

    suspend fun testModel(
        baseUrl: String,
        token: String,
        modelId: String
    ): Outcome {
        if (baseUrl.isBlank()) return Outcome.Failure("нет URL")
        if (token.isBlank()) return Outcome.Failure("нет токена")
        if (modelId.isBlank()) return Outcome.Failure("пустой ID")

        val start = System.currentTimeMillis()
        return try {
            val response = client.post(baseUrl) {
                contentType(ContentType.Application.Json)
                headers { append("Authorization", "Bearer $token") }
                setBody(
                    TestRequest(
                        model = modelId,
                        messages = listOf(Message(role = "user", content = "Скажи букву \"а\""))
                    )
                )
            }
            val duration = System.currentTimeMillis() - start
            if (response.status.value in 200..299) {
                val body: OpenRouterResponse = response.body()
                val content = body.choices.firstOrNull()?.message?.content
                if (content.isNullOrBlank()) {
                    Outcome.Failure("пустой ответ")
                } else {
                    Outcome.Success(duration)
                }
            } else {
                val err = runCatching { response.body<OpenRouterError>() }.getOrNull()
                Outcome.Failure(briefError(response.status.value, err?.error?.message))
            }
        } catch (e: Exception) {
            Log.e("ModelTest", "Test failed for $modelId", e)
            val msg = e.message?.take(60) ?: "ошибка сети"
            Outcome.Failure(msg)
        }
    }

    private fun briefError(httpCode: Int, message: String?): String = when (httpCode) {
        401 -> "401 токен"
        402 -> "402 нет кредитов"
        403 -> "403 лимит ключа"
        404 -> "404 нет модели"
        429 -> "429 rate limit"
        500, 502, 503, 504 -> "$httpCode сервер"
        else -> "$httpCode${message?.let { " " + it.take(30) } ?: ""}"
    }

    fun close() {
        client.close()
    }
}

@Serializable
private data class TestRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int = 5
)
