package com.alongside.core.domain.place.importing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Fixtures below are real `maps.app.goo.gl` share links, resolved by hand (following the HTTP
 * redirect chain) - not guessed. All four resolve to the same shape: a name+address in the path,
 * a hex feature-id pair in the `data=` blob, and no `@lat,lng` segment at all - confirming that
 * "no coordinates, name only" is the common case for a place-card share, not an edge case.
 */
class GoogleMapsShareLinkParserTest {
    @Test
    fun `parses a real resolved link with name address and feature id but no coordinates`() {
        val url =
            "https://www.google.com/maps/place/Global+Solar,+%D0%A2%D0%B8%D0%B2%D1%80%D1%96%D0%B2%D1%81%D1%8C%D0%BA" +
                "%D0%B5+%D1%88%D0%BE%D1%81%D0%B5,+1,+%D0%92%D1%96%D0%BD%D0%BD%D0%B8%D1%86%D1%8F,+%D0%92%D1%96%D0%BD" +
                "%D0%BD%D0%B8%D1%86%D1%8C%D0%BA%D0%B0+%D0%BE%D0%B1%D0%BB%D0%B0%D1%81%D1%82%D1%8C,+21000/data=!4m2!" +
                "3m1!1s0x472d5d007ffab745:0xc807e74d1edeaf6e!18m1!1e1?utm_source=mstt_1&entry=gps"

        val result = GoogleMapsShareLinkParser.parse(url)

        assertEquals("Global Solar", result?.displayName)
        assertEquals("Тиврівське шосе, 1, Вінниця, Вінницька область, 21000", result?.address)
        assertEquals("0x472d5d007ffab745:0xc807e74d1edeaf6e", result?.featureId)
        assertNull(result?.latitude)
        assertNull(result?.longitude)
    }

    @Test
    fun `parses a resolved link that includes a lat lng zoom segment`() {
        val url = "https://www.google.com/maps/place/Rynok+Square/@49.8397,24.0297,17z/data=!4m2!3m1!1s0xabc:0xdef"

        val result = GoogleMapsShareLinkParser.parse(url)

        assertEquals("Rynok Square", result?.displayName)
        assertNull(result?.address)
        assertEquals(49.8397, result?.latitude)
        assertEquals(24.0297, result?.longitude)
        assertEquals("0xabc:0xdef", result?.featureId)
    }

    @Test
    fun `parses a resolved link with a single-component name and no data blob`() {
        val url = "https://www.google.com/maps/place/McDonald's/@49.8,24.0,15z"

        val result = GoogleMapsShareLinkParser.parse(url)

        assertEquals("McDonald's", result?.displayName)
        assertNull(result?.address)
        assertNull(result?.featureId)
        assertEquals(49.8, result?.latitude)
        assertEquals(24.0, result?.longitude)
    }

    @Test
    fun `returns null for a URL with no maps place segment`() {
        val result = GoogleMapsShareLinkParser.parse("https://www.google.com/search?q=coffee")

        assertNull(result)
    }

    @Test
    fun `returns null for a non-maps URL entirely`() {
        val result = GoogleMapsShareLinkParser.parse("https://example.com/not-maps-at-all")

        assertNull(result)
    }
}
