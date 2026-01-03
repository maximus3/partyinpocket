package com.m3games.partyinpocket.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.m3games.partyinpocket.R

sealed class Game(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val isAvailable: Boolean = false
) {
    data object Hat : Game(
        id = "hat",
        titleRes = R.string.game_hat,
        descriptionRes = R.string.game_hat_desc,
        iconRes = R.drawable.ic_hat,
        isAvailable = true
    )

    data object Alias : Game(
        id = "alias",
        titleRes = R.string.game_alias,
        descriptionRes = R.string.game_alias_desc,
        iconRes = R.drawable.ic_alias
    )

    data object Crocodile : Game(
        id = "crocodile",
        titleRes = R.string.game_crocodile,
        descriptionRes = R.string.game_crocodile_desc,
        iconRes = R.drawable.ic_crocodile
    )

    data object Spy : Game(
        id = "spy",
        titleRes = R.string.game_spy,
        descriptionRes = R.string.game_spy_desc,
        iconRes = R.drawable.ic_spy
    )

    companion object {
        fun getAll() = listOf(Hat, Alias, Crocodile, Spy)
    }
}
