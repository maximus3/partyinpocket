package com.m3games.partyinpocket.domain.model.crocodile

import com.m3games.partyinpocket.domain.model.Player

data class CrocodileGameState(
    val settings: CrocodileSettings,
    val players: List<Player>,
    val remainingWords: List<String>,
    val currentWord: String?,
    val currentPlayerIndex: Int,
    val guessedInTurn: List<String> = emptyList(),
    val skippedInTurn: List<String> = emptyList(),
    val playerSkipsLeft: Map<Int, Int>,
    val phase: CrocodileGamePhase,
    val remainingTimeSeconds: Int
) {
    val currentPlayer: Player
        get() = players[currentPlayerIndex]

    val currentPlayerSkipsLeft: Int
        get() = playerSkipsLeft[currentPlayerIndex] ?: 0

    val nextPlayerIndex: Int
        get() = (currentPlayerIndex + 1) % players.size

    val targetTotalWords: Int
        get() = settings.targetWordsPerPlayer * players.size

    val totalScored: Int
        get() = players.sumOf { it.score }

    val isGameFinished: Boolean
        get() = totalScored >= targetTotalWords || remainingWords.isEmpty()
}

enum class CrocodileGamePhase {
    READY_TO_START,
    PLAYING,
    TURN_ENDED,
    GAME_FINISHED
}
