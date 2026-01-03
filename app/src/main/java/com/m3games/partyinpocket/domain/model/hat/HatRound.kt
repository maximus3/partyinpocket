package com.m3games.partyinpocket.domain.model.hat

import androidx.annotation.StringRes
import com.m3games.partyinpocket.R

enum class HatRound(
    val number: Int,
    @StringRes val titleRes: Int,
    @StringRes val rulesRes: Int
) {
    EXPLAIN(1, R.string.round_explain, R.string.rules_explain),
    PANTOMIME(2, R.string.round_pantomime, R.string.rules_pantomime),
    ASSOCIATION(3, R.string.round_association, R.string.rules_association);

    fun next(): HatRound? = when (this) {
        EXPLAIN -> PANTOMIME
        PANTOMIME -> ASSOCIATION
        ASSOCIATION -> null
    }
}
