package com.m3games.partyinpocket.domain.model.common

/**
 * Карточка для скрытой раздачи: то, что игрок видит, когда берет телефон.
 *
 * Универсальная структура для шпиона, мафии и любых других игр со скрытыми ролями.
 * Для шпиона: title="Локация", mainText="Бар", subText="Бармен", hint="список локаций".
 * Для шпиона-шпиона: title="Роль", mainText="Шпион", hint="список локаций для угадывания".
 * Для мафии в будущем: title="Роль", mainText="Мафия"/"Мирный"/"Доктор", subText/hint опц.
 */
data class HiddenCard(
    val title: String,
    val mainText: String,
    val subText: String? = null,
    val hint: List<String>? = null
)

/**
 * Состояние процесса передачи телефона по кругу с показом скрытой карточки каждому игроку.
 *
 * Поток фаз для каждого игрока:
 *   WAITING_FOR_PLAYER  ("Передайте телефон Игроку X" → тап "Я готов")
 *   → REVEALING_CARD   (карточка показана → тап "Скрыть")
 *   → HIDING_CARD      (экран-заглушка → автопереход к следующему игроку)
 *
 * Раздача считается завершённой, когда currentIndex == playerNames.size.
 */
data class HiddenDealState(
    val playerNames: List<String>,
    val cards: List<HiddenCard>,
    val currentIndex: Int = 0,
    val phase: HiddenDealPhase = HiddenDealPhase.WAITING_FOR_PLAYER
) {
    val currentPlayerName: String?
        get() = playerNames.getOrNull(currentIndex)

    val currentCard: HiddenCard?
        get() = cards.getOrNull(currentIndex)

    val isFinished: Boolean
        get() = currentIndex >= playerNames.size

    val totalPlayers: Int
        get() = playerNames.size

    fun ready(): HiddenDealState =
        if (phase == HiddenDealPhase.WAITING_FOR_PLAYER) copy(phase = HiddenDealPhase.REVEALING_CARD) else this

    fun hide(): HiddenDealState =
        if (phase == HiddenDealPhase.REVEALING_CARD) copy(phase = HiddenDealPhase.HIDING_CARD) else this

    fun advance(): HiddenDealState =
        if (phase == HiddenDealPhase.HIDING_CARD) {
            copy(
                currentIndex = currentIndex + 1,
                phase = HiddenDealPhase.WAITING_FOR_PLAYER
            )
        } else this
}

enum class HiddenDealPhase {
    WAITING_FOR_PLAYER,
    REVEALING_CARD,
    HIDING_CARD
}
