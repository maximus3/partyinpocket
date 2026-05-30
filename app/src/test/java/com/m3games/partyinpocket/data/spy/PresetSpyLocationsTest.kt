package com.m3games.partyinpocket.data.spy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetSpyLocationsTest {

    @Test
    fun `getById returns default pack`() {
        val pack = PresetSpyLocations.getById("default_spy")

        assertNotNull(pack)
        assertEquals("default_spy", pack!!.id)
    }

    @Test
    fun `getById returns null for unknown id`() {
        assertNull(PresetSpyLocations.getById("nonexistent"))
    }

    @Test
    fun `getByIds returns packs in input order ignoring unknown`() {
        val result = PresetSpyLocations.getByIds(listOf("default_spy", "unknown", "fantasy_spy"))

        assertEquals(listOf("default_spy", "fantasy_spy"), result.map { it.id })
    }

    @Test
    fun `getAll returns three preset packs`() {
        val all = PresetSpyLocations.getAll()

        assertEquals(listOf("default_spy", "fantasy_spy", "office_spy"), all.map { it.id })
    }

    @Test
    fun `default pack contains at least 10 locations`() {
        val pack = PresetSpyLocations.default

        assertTrue("Должно быть достаточно локаций для разнообразия", pack.locations.size >= 10)
    }

    @Test
    fun `every default location has at least one role`() {
        val pack = PresetSpyLocations.default

        assertFalse(pack.locations.any { it.roles.isEmpty() })
    }

    @Test
    fun `every default location has unique name`() {
        val names = PresetSpyLocations.default.locations.map { it.name }

        assertEquals(names.size, names.distinct().size)
    }
}
