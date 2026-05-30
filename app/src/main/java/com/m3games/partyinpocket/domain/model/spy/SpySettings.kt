package com.m3games.partyinpocket.domain.model.spy

data class SpySettings(
    val playerCount: Int = 4,
    val discussionDurationSeconds: Int = 480,
    val useRoles: Boolean = true,
    val selectedLocationPacks: List<String> = listOf("default_spy")
)
