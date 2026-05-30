package com.m3games.partyinpocket.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSettingsTest {

    @Test
    fun `default settings target OpenRouter with free-only enabled`() {
        val s = AiSettings()

        assertEquals(AiProvider.OpenRouter, s.provider)
        assertEquals(AiProvider.OpenRouter.defaultBaseUrl, s.baseUrl)
        assertEquals(AiProvider.OpenRouter.defaultModel, s.model)
        assertEquals("", s.token)
        assertTrue(s.useFreeOnly)
    }

    @Test
    fun `copy with custom provider keeps remaining fields`() {
        val s = AiSettings(provider = AiProvider.Anthropic, token = "secret")

        val copied = s.copy(useFreeOnly = false)

        assertEquals(AiProvider.Anthropic, copied.provider)
        assertEquals("secret", copied.token)
        assertEquals(false, copied.useFreeOnly)
    }
}
