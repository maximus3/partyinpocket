package com.m3games.partyinpocket.domain.model.crocodile

data class CrocodileSettings(
    val playerCount: Int = 4,
    val targetWordsPerPlayer: Int = 3,
    val turnDurationSeconds: Int = 60,
    val useTimer: Boolean = true,
    val selectedPacks: List<String> = listOf("default"),
    val skipPenalty: Int = 0,
    val maxSkipsPerTurn: Int = 3
)
