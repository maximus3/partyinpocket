package com.m3games.partyinpocket.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.m3games.partyinpocket.domain.model.hat.HatRound
import org.junit.Assert.assertEquals
import org.junit.Test

class TeamTest {

    @Test
    fun `withScore adds points to round`() {
        val team = Team(id = 0, name = "A", colorArgb = 0)

        val updated = team.withScore(HatRound.EXPLAIN, 3)

        assertEquals(mapOf(HatRound.EXPLAIN to 3), updated.scores)
    }

    @Test
    fun `withScore accumulates points on repeated calls`() {
        val team = Team(id = 0, name = "A", colorArgb = 0)
            .withScore(HatRound.EXPLAIN, 2)
            .withScore(HatRound.EXPLAIN, 5)

        assertEquals(mapOf(HatRound.EXPLAIN to 7), team.scores)
    }

    @Test
    fun `withScore tracks rounds independently`() {
        val team = Team(id = 0, name = "A", colorArgb = 0)
            .withScore(HatRound.EXPLAIN, 4)
            .withScore(HatRound.PANTOMIME, 2)
            .withScore(HatRound.ASSOCIATION, 1)

        assertEquals(
            mapOf(
                HatRound.EXPLAIN to 4,
                HatRound.PANTOMIME to 2,
                HatRound.ASSOCIATION to 1
            ),
            team.scores
        )
    }

    @Test
    fun `withScore accepts negative points for skip penalty`() {
        val team = Team(id = 0, name = "A", colorArgb = 0)
            .withScore(HatRound.EXPLAIN, 5)
            .withScore(HatRound.EXPLAIN, -2)

        assertEquals(mapOf(HatRound.EXPLAIN to 3), team.scores)
    }

    @Test
    fun `totalScore sums scores across all rounds`() {
        val team = Team(id = 0, name = "A", colorArgb = 0)
            .withScore(HatRound.EXPLAIN, 4)
            .withScore(HatRound.PANTOMIME, 3)
            .withScore(HatRound.ASSOCIATION, 2)

        assertEquals(9, team.totalScore)
    }

    @Test
    fun `totalScore is zero when no scores recorded`() {
        val team = Team(id = 0, name = "A", colorArgb = 0)

        assertEquals(0, team.totalScore)
    }

    @Test
    fun `Team create converts Color to ARGB Int`() {
        val team = Team.create(id = 1, name = "Red", color = Color.Red)

        assertEquals(Color.Red.toArgb(), team.colorArgb)
    }

    @Test
    fun `color property reconstructs Color from ARGB`() {
        val original = Color(red = 0.5f, green = 0.25f, blue = 0.75f, alpha = 1f)
        val team = Team.create(id = 0, name = "A", color = original)

        assertEquals(original.toArgb(), team.color.toArgb())
    }

    @Test
    fun `withScore returns new instance and does not mutate original`() {
        val original = Team(id = 0, name = "A", colorArgb = 0)
        val updated = original.withScore(HatRound.EXPLAIN, 1)

        assertEquals(emptyMap<HatRound, Int>(), original.scores)
        assertEquals(mapOf(HatRound.EXPLAIN to 1), updated.scores)
    }
}
