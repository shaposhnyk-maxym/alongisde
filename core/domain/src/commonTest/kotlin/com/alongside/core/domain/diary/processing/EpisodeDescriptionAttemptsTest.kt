package com.alongside.core.domain.diary.processing

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EpisodeDescriptionAttemptsTest {
    @Test
    fun `regeneration allowed when attempts are under the limit`() {
        assertTrue(canRegenerateDescription(0))
        assertTrue(canRegenerateDescription(MAX_DESCRIPTION_REGENERATION_ATTEMPTS - 1))
    }

    @Test
    fun `regeneration disallowed once attempts reach the limit`() {
        assertFalse(canRegenerateDescription(MAX_DESCRIPTION_REGENERATION_ATTEMPTS))
    }

    @Test
    fun `regeneration disallowed past the limit`() {
        assertFalse(canRegenerateDescription(MAX_DESCRIPTION_REGENERATION_ATTEMPTS + 5))
    }
}
