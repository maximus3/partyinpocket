package com.m3games.partyinpocket.data.api.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>,
    val response_format: ResponseFormat? = null
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String,
    val json_schema: JsonSchema
)

@Serializable
data class JsonSchema(
    val name: String,
    val schema: JsonObject
)

@Serializable
data class OpenRouterResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)

@Serializable
data class WordListResponse(
    val words: List<String>
)

@Serializable
data class OpenRouterError(
    val error: ErrorDetails
)

@Serializable
data class ErrorDetails(
    val message: String,
    val code: Int
)
