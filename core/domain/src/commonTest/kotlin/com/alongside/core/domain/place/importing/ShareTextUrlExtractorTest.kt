package com.alongside.core.domain.place.importing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShareTextUrlExtractorTest {
    @Test
    fun `text with a name and a share link extracts just the link`() {
        assertEquals(
            "https://maps.app.goo.gl/Mnzn3A1DSQYoXsDw5",
            extractShareUrl("Global Solar\nhttps://maps.app.goo.gl/Mnzn3A1DSQYoXsDw5"),
        )
    }

    @Test
    fun `bare url with no surrounding text extracts unchanged`() {
        assertEquals("https://maps.app.goo.gl/abc", extractShareUrl("https://maps.app.goo.gl/abc"))
    }

    @Test
    fun `text with no url returns null`() {
        assertNull(extractShareUrl("Global Solar, no link here"))
    }

    @Test
    fun `empty text returns null`() {
        assertNull(extractShareUrl(""))
    }

    @Test
    fun `text with trailing punctuation after the url is included since urls can contain it`() {
        // Deliberately permissive - a trailing query string or path segment is valid URL content,
        // not punctuation to strip; ShareLinkRedirectResolver/GoogleMapsShareLinkParser downstream
        // already tolerate real Google Maps URLs' own query strings.
        assertEquals(
            "https://maps.app.goo.gl/abc?query=1",
            extractShareUrl("Check this out: https://maps.app.goo.gl/abc?query=1"),
        )
    }

    @Test
    fun `multiple urls in the text extracts the first one`() {
        assertEquals(
            "https://maps.app.goo.gl/first",
            extractShareUrl("https://maps.app.goo.gl/first and also https://maps.app.goo.gl/second"),
        )
    }
}
