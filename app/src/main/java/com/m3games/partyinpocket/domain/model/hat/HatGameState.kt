package com.m3games.partyinpocket.domain.model.hat

import com.m3games.partyinpocket.domain.model.Team

data class HatGameState(
    val settings: HatSettings,
    val teams: List<Team>,
    val allWords: List<String>,
    val remainingWords: List<String>,
    val currentRound: HatRound,
    val currentTeamIndex: Int,
    val currentWord: String?,
    val guessedInTurn: List<String> = emptyList(),
    val skippedInTurn: List<String> = emptyList(),
    val teamSkipsLeft: Map<Int, Int>,
    val phase: HatGamePhase,
    val remainingTimeSeconds: Int
) {
    val currentTeam: Team
        get() = teams[currentTeamIndex]

    val currentTeamSkipsLeft: Int
        get() = teamSkipsLeft[currentTeamIndex] ?: 0

    val nextTeamIndex: Int
        get() = (currentTeamIndex + 1) % teams.size

    val isRoundFinished: Boolean
        get() = remainingWords.isEmpty()

    val isGameFinished: Boolean
        get() = currentRound == HatRound.ASSOCIATION && remainingWords.isEmpty()
}

enum class HatGamePhase {
    READY_TO_START,
    PLAYING,
    TURN_ENDED,
    ROUND_ENDED,
    GAME_FINISHED
}
