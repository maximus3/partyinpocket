package com.m3games.partyinpocket.domain.model

sealed class WordGenerationState {
    data object Idle : WordGenerationState()
    data class Loading(val attempt: Int, val generatedCount: Int) : WordGenerationState()
    data class Success(val words: List<String>) : WordGenerationState()
    data class PartialSuccess(
        val words: List<String>,
        val attempts: Int,
        val targetCount: Int
    ) : WordGenerationState()
    data class Error(val message: String) : WordGenerationState()
}
