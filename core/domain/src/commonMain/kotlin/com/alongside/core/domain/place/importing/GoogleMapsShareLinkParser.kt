package com.alongside.core.domain.place.importing

/** A Google Maps place URL, already parsed apart - see [GoogleMapsShareLinkParser]. */
public data class ParsedGoogleMapsLink(
    public val displayName: String,
    public val address: String?,
    public val latitude: Double?,
    public val longitude: Double?,
    public val featureId: String?,
)

/**
 * Pure text-processing over an already-*resolved* Google Maps URL (i.e. after following a
 * `maps.app.goo.gl` short link's HTTP redirect - that hop is [ShareLinkRedirectResolver]'s job,
 * kept separate so this stays a zero-I/O function, unit-testable with literal fixture strings).
 *
 * Every real share link resolves to `.../maps/place/<name>[,<address>][/@lat,lng,zoom]/data=
 * !4m2!3m1!1s<hex>:<hex>!18m1!1e1?...` - the name+address segment is URL-encoded (`+` for space),
 * comma-separated, first component is the display name; the `@lat,lng` segment is only present
 * for pin-drop shares, absent for the far more common place-card share (confirmed against 4 real
 * links); the `!1s<hex>:<hex>` pair is Google's internal feature id for the place.
 */
public object GoogleMapsShareLinkParser {
    private val featureIdRegex = Regex("""!1s(0x[0-9a-fA-F]+:0x[0-9a-fA-F]+)""")
    private val coordinatesRegex = Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)""")
    private const val PLACE_MARKER = "/maps/place/"

    public fun parse(url: String): ParsedGoogleMapsLink? {
        val rawNameSegment = extractRawNameSegment(url) ?: return null

        val components = rawNameSegment.decodeUrlComponent().split(", ")
        val displayName = components.first()
        val address = if (components.size > 1) components.drop(1).joinToString(", ") else null

        val coordinatesMatch = coordinatesRegex.find(url)
        val featureIdMatch = featureIdRegex.find(url)

        return ParsedGoogleMapsLink(
            displayName = displayName,
            address = address,
            latitude = coordinatesMatch?.groupValues?.get(1)?.toDoubleOrNull(),
            longitude = coordinatesMatch?.groupValues?.get(2)?.toDoubleOrNull(),
            featureId = featureIdMatch?.groupValues?.get(1),
        )
    }

    // The URL-encoded name[,address] path segment right after PLACE_MARKER, up to the next `/` or
    // `?` (whichever ends it - a `/@lat,lng,zoom` or `/data=...` segment, or a query string).
    private fun extractRawNameSegment(url: String): String? {
        val markerIndex = url.indexOf(PLACE_MARKER)
        if (markerIndex == -1) return null

        val afterMarker = url.substring(markerIndex + PLACE_MARKER.length)
        val nameSegmentEnd =
            afterMarker.indexOfFirst { it == '/' || it == '?' }.takeIf { it != -1 } ?: afterMarker.length
        return afterMarker.substring(0, nameSegmentEnd).ifEmpty { null }
    }
}

// No Ktor dependency in core:domain (architecture rule: domain stays network-agnostic) - this is
// a minimal percent-decoder good enough for Google Maps URLs (`+` for space, `%XX` for everything
// else), accumulating raw bytes first so multi-byte UTF-8 percent sequences decode correctly.
private fun String.decodeUrlComponent(): String {
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < length) {
        when (val c = this[i]) {
            '+' -> {
                bytes.add(' '.code.toByte())
                i++
            }
            '%' -> {
                bytes.add(substring(i + 1, i + 3).toInt(16).toByte())
                i += 3
            }
            else -> {
                bytes.add(c.code.toByte())
                i++
            }
        }
    }
    return bytes.toByteArray().decodeToString()
}
