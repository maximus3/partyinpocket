package com.m3games.partyinpocket.domain.model.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenDealStateTest {

    private val playerNames = listOf("Алиса", "Боб", "Чарли")
    private val cards = listOf(
        HiddenCard(title = "Роль", mainText = "Шпион"),
        HiddenCard(title = "Локация", mainText = "Бар", subText = "Бармен"),
        HiddenCard(title = "Локация", mainText = "Бар", subText = "Гость")
    )
    private val initial = HiddenDealState(playerNames = playerNames, cards = cards)

    @Test
    fun `initial state starts at index 0 waiting for first player`() {
        assertEquals(0, initial.currentIndex)
        assertEquals(HiddenDealPhase.WAITING_FOR_PLAYER, initial.phase)
    }

    @Test
    fun `currentPlayerName matches currentIndex`() {
        assertEquals("Алиса", initial.currentPlayerName)
    }

    @Test
    fun `currentCard matches currentIndex`() {
        assertEquals(cards[0], initial.currentCard)
    }

    @Test
    fun `currentPlayerName returns null when index past end`() {
        val finished = initial.copy(currentIndex = 3)

        assertNull(finished.currentPlayerName)
    }

    @Test
    fun `totalPlayers returns size of playerNames`() {
        assertEquals(3, initial.totalPlayers)
    }

    @Test
    fun `isFinished is true when index equals size`() {
        assertTrue(initial.copy(currentIndex = 3).isFinished)
    }

    @Test
    fun `isFinished is false when index less than size`() {
        assertFalse(initial.copy(currentIndex = 2).isFinished)
    }

    @Test
    fun `ready transitions from WAITING to REVEALING`() {
        val next = initial.ready()

        assertEquals(HiddenDealPhase.REVEALING_CARD, next.phase)
    }

    @Test
    fun `ready is a no-op when not in WAITING phase`() {
        val revealing = initial.copy(phase = HiddenDealPhase.REVEALING_CARD)

        assertEquals(revealing, revealing.ready())
    }

    @Test
    fun `hide transitions from REVEALING to HIDING`() {
        val revealing = initial.copy(phase = HiddenDealPhase.REVEALING_CARD)

        val hidden = revealing.hide()

        assertEquals(HiddenDealPhase.HIDING_CARD, hidden.phase)
    }

    @Test
    fun `hide is a no-op when not in REVEALING phase`() {
        val waiting = initial // WAITING_FOR_PLAYER

        assertEquals(waiting, waiting.hide())
    }

    @Test
    fun `advance moves to next player and back to WAITING`() {
        val hidden = initial.copy(phase = HiddenDealPhase.HIDING_CARD)

        val next = hidden.advance()

        assertEquals(1, next.currentIndex)
        assertEquals(HiddenDealPhase.WAITING_FOR_PLAYER, next.phase)
    }

    @Test
    fun `advance is a no-op when not in HIDING phase`() {
        val waiting = initial

        assertEquals(waiting, waiting.advance())
    }

    @Test
    fun `full sequence ready hide advance walks through all players`() {
        var state = initial

        for (expectedIndex in 0..2) {
            assertEquals(expectedIndex, state.currentIndex)
            assertEquals(HiddenDealPhase.WAITING_FOR_PLAYER, state.phase)
            state = state.ready()
            assertEquals(HiddenDealPhase.REVEALING_CARD, state.phase)
            state = state.hide()
            assertEquals(HiddenDealPhase.HIDING_CARD, state.phase)
            state = state.advance()
        }

        assertTrue(state.isFinished)
        assertEquals(3, state.currentIndex)
    }
}
