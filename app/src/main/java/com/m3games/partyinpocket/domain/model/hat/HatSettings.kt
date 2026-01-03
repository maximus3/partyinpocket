package com.m3games.partyinpocket.domain.model.hat

data class HatSettings(
    val teamCount: Int = 2,
    val wordCount: Int = 40,
    val turnDurationSeconds: Int = 60,
    val selectedPacks: List<String> = listOf("default"),
    val skipPenalty: Int = 0,
    val maxSkipsPerTurn: Int = 5
)
