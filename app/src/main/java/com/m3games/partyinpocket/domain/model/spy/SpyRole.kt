package com.m3games.partyinpocket.domain.model.spy

sealed class SpyRole {
    data class Civilian(val location: String, val role: String?) : SpyRole()
    data object Spy : SpyRole()
}

data class SpyPlayer(
    val index: Int,
    val name: String,
    val role: SpyRole
)
