package com.alongside.app.share

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The iOS equivalent of `MainActivity`'s `pendingShareText` field - a Compose-observable holder
 * for the raw shared text/URL string handed off by the Share Extension via the App Group
 * container (see `ShareViewController.swift`/`AlongsideiOSApp.swift`). Read directly inside
 * `MainViewController`'s `ComposeUIViewController` body - Compose's snapshot system observes a
 * plain `mutableStateOf` read the same way regardless of where it happens, so no extra plumbing
 * (or Swift-facing signature change) is needed.
 */
private var pendingShareTextState by mutableStateOf<String?>(null)

public fun currentIosPendingShareText(): String? = pendingShareTextState

public fun setIosPendingShareText(text: String?) {
    pendingShareTextState = text
}
