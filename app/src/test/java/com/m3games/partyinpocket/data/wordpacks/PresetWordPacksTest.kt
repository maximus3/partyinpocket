package com.m3games.partyinpocket.data.wordpacks

import com.m3games.partyinpocket.domain.model.WordPack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetWordPacksTest {

    @After
    fun cleanupGeneratedPacks() {
        val generated = PresetWordPacks.getAll()
            .map { it.id }
            .filter { it.startsWith("generated_") || it == "test_pack" }
        generated.forEach { PresetWordPacks.removeGeneratedPack(it) }
    }

    @Test
    fun `getById returns default pack`() {
        val pack = PresetWordPacks.getById("default")

        assertNotNull(pack)
        assertEquals("default", pack!!.id)
        assertEquals("Стандартный словарь", pack.name)
    }

    @Test
    fun `getById returns null for unknown id`() {
        assertNull(PresetWordPacks.getById("nonexistent"))
    }

    @Test
    fun `getByIds returns packs in input order ignoring unknown`() {
        val result = PresetWordPacks.getByIds(listOf("default", "unknown", "animals"))

        assertEquals(listOf("default", "animals"), result.map { it.id })
    }

    @Test
    fun `getByIds returns empty list when all ids are unknown`() {
        val result = PresetWordPacks.getByIds(listOf("unknown1", "unknown2"))

        assertEquals(emptyList<WordPack>(), result)
    }

    @Test
    fun `addGeneratedPack makes pack findable by id`() {
        val pack = WordPack(
            id = "test_pack",
            name = "Test",
            description = "desc",
            words = listOf("слово1", "слово2")
        )

        PresetWordPacks.addGeneratedPack(pack)

        assertEquals(pack, PresetWordPacks.getById("test_pack"))
    }

    @Test
    fun `removeGeneratedPack removes only the matching id`() {
        val packA = WordPack(id = "test_pack", name = "A", description = "", words = listOf("a"))
        val packB = WordPack(id = "generated_1", name = "B", description = "", words = listOf("b"))
        PresetWordPacks.addGeneratedPack(packA)
        PresetWordPacks.addGeneratedPack(packB)

        PresetWordPacks.removeGeneratedPack("test_pack")

        assertNull(PresetWordPacks.getById("test_pack"))
        assertNotNull(PresetWordPacks.getById("generated_1"))
    }

    @Test
    fun `getAll includes both preset and generated packs`() {
        val pack = WordPack(id = "test_pack", name = "T", description = "", words = listOf("a"))
        PresetWordPacks.addGeneratedPack(pack)

        val all = PresetWordPacks.getAll()
        val ids = all.map { it.id }

        assertTrue("должен содержать preset 'default'", "default" in ids)
        assertTrue("должен содержать preset 'animals'", "animals" in ids)
        assertTrue("должен содержать generated 'test_pack'", "test_pack" in ids)
    }

    @Test
    fun `getAll preserves preset packs order`() {
        val presetIds = PresetWordPacks.getAll().map { it.id }.take(3)

        assertEquals(listOf("default", "extended", "words400"), presetIds)
    }
}
