package com.m3games.partyinpocket.domain.model.spy

data class SpyLocation(
    val name: String,
    val roles: List<String> = emptyList()
)

data class SpyLocationPack(
    val id: String,
    val name: String,
    val description: String,
    val locations: List<SpyLocation>
)
