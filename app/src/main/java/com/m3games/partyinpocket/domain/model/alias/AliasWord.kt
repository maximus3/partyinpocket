package com.m3games.partyinpocket.domain.model.alias

data class AliasWord(
    val word: String,
    val forbidden: List<String> = emptyList()
)
