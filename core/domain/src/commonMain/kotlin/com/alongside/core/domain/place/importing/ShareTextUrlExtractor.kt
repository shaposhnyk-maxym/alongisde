package com.alongside.core.domain.place.importing

private val URL_REGEX = Regex("""https?://\S+""")

/**
 * Android's `ACTION_SEND`/`EXTRA_TEXT` (and the iOS Share Extension equivalent) hands over
 * whatever free text the OS share sheet assembled - typically `"<Place name>\n<share link>"`,
 * not a bare URL - so [PlaceImportPipeline.import] (which expects an already-identified URL)
 * can't consume it directly. Pure, zero-I/O: extracts the first `http(s)://` substring, or null
 * if the shared text contains no URL at all.
 */
public fun extractShareUrl(text: String): String? = URL_REGEX.find(text)?.value
