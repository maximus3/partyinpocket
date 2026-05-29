package com.m3games.partyinpocket.domain.model.hat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HatRoundTest {

    @Test
    fun `EXPLAIN next returns PANTOMIME`() {
        assertEquals(HatRound.PANTOMIME, HatRound.EXPLAIN.next())
    }

    @Test
    fun `PANTOMIME next returns ASSOCIATION`() {
        assertEquals(HatRound.ASSOCIATION, HatRound.PANTOMIME.next())
    }

    @Test
    fun `ASSOCIATION next returns null`() {
        assertNull(HatRound.ASSOCIATION.next())
    }

    @Test
    fun `round numbers are 1 2 3 in order`() {
        assertEquals(1, HatRound.EXPLAIN.number)
        assertEquals(2, HatRound.PANTOMIME.number)
        assertEquals(3, HatRound.ASSOCIATION.number)
    }

    @Test
    fun `full progression yields three rounds before null`() {
        val sequence = generateSequence<HatRound>(HatRound.EXPLAIN) { it.next() }.toList()

        assertEquals(
            listOf(HatRound.EXPLAIN, HatRound.PANTOMIME, HatRound.ASSOCIATION),
            sequence
        )
    }
}
