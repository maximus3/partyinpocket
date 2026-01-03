package com.m3games.partyinpocket.domain.model

data class WordPack(
    val id: String,
    val name: String,
    val description: String,
    val words: List<String>
)

sealed class WordSource {
    data object Preset : WordSource()
    // Future:
    // data object UserCreated : WordSource()
    // data class PlayerInput(val wordsPerPlayer: Int) : WordSource()
    // data class AiGenerated(val topic: String) : WordSource()
}
