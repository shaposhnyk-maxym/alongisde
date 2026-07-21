package com.alongside.core.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals

class CountryFlagTest {
    @Test
    fun `renders France as its flag emoji`() {
        assertEquals("🇫🇷", countryCodeToFlagEmoji("FR"))
    }

    @Test
    fun `renders Ukraine as its flag emoji`() {
        assertEquals("🇺🇦", countryCodeToFlagEmoji("UA"))
    }

    @Test
    fun `is case-insensitive`() {
        assertEquals("🇺🇸", countryCodeToFlagEmoji("us"))
    }
}
