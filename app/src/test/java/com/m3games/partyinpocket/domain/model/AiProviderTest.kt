package com.m3games.partyinpocket.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderTest {

    @Test
    fun `all contains four known providers`() {
        assertEquals(
            listOf(AiProvider.OpenRouter, AiProvider.OpenAI, AiProvider.Anthropic, AiProvider.Custom),
            AiProvider.all
        )
    }

    @Test
    fun `byId returns matching provider`() {
        assertEquals(AiProvider.OpenAI, AiProvider.byId("openai"))
        assertEquals(AiProvider.Anthropic, AiProvider.byId("anthropic"))
        assertEquals(AiProvider.Custom, AiProvider.byId("custom"))
    }

    @Test
    fun `byId falls back to OpenRouter for unknown id`() {
        assertEquals(AiProvider.OpenRouter, AiProvider.byId("totally-unknown"))
    }

    @Test
    fun `only OpenRouter supports free filter`() {
        assertTrue(AiProvider.OpenRouter.supportsFreeFilter)
        assertFalse(AiProvider.OpenAI.supportsFreeFilter)
        assertFalse(AiProvider.Anthropic.supportsFreeFilter)
        assertFalse(AiProvider.Custom.supportsFreeFilter)
    }

    @Test
    fun `only OpenRouter has userModelsUrl`() {
        assertNotNull(AiProvider.OpenRouter.userModelsUrl)
        assertNull(AiProvider.OpenAI.userModelsUrl)
        assertNull(AiProvider.Anthropic.userModelsUrl)
        assertNull(AiProvider.Custom.userModelsUrl)
    }

    @Test
    fun `Custom has no modelsUrl`() {
        assertNull(AiProvider.Custom.modelsUrl)
    }

    @Test
    fun `non-Custom providers have non-blank defaultBaseUrl and modelsUrl`() {
        listOf(AiProvider.OpenRouter, AiProvider.OpenAI, AiProvider.Anthropic).forEach { provider ->
            assertTrue(
                "${provider.id} should have a default base URL",
                provider.defaultBaseUrl.isNotBlank()
            )
            assertTrue(
                "${provider.id} should have a models URL",
                provider.modelsUrl?.isNotBlank() == true
            )
        }
    }
}
