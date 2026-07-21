package com.alongside.core.ui.format

private const val REGIONAL_INDICATOR_A = 0x1F1E6
private const val SUPPLEMENTARY_PLANE_OFFSET = 0x10000
private const val HIGH_SURROGATE_BASE = 0xD800
private const val LOW_SURROGATE_BASE = 0xDC00
private const val LOW_SURROGATE_MASK = 0x3FF
private const val LOW_SURROGATE_BITS = 10

/**
 * Renders an ISO 3166-1 alpha-2 country code (e.g. "FR") as its flag emoji - two Unicode regional
 * indicator symbols (U+1F1E6..U+1F1FF), one per letter. Each is built as a manual UTF-16 surrogate
 * pair since they sit outside the Basic Multilingual Plane and `kotlin.text` has no
 * codepoint-append helper available in commonMain.
 */
public fun countryCodeToFlagEmoji(countryCode: String): String =
    buildString {
        for (letter in countryCode.uppercase()) {
            val codePoint = REGIONAL_INDICATOR_A + (letter - 'A')
            val offset = codePoint - SUPPLEMENTARY_PLANE_OFFSET
            append(((offset shr LOW_SURROGATE_BITS) + HIGH_SURROGATE_BASE).toChar())
            append(((offset and LOW_SURROGATE_MASK) + LOW_SURROGATE_BASE).toChar())
        }
    }
