package com.alongside.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

/**
 * Renders the top of the Navigation 3 back stack. The Android actual delegates to the real
 * `androidx.navigation3.ui.NavDisplay` (transitions, predictive back); navigation3-ui
 * publishes no iOS/desktop artifacts at 1.1.0-alpha01, so the other targets render the top
 * entry directly - same graph, no animations - until the UI artifact goes multiplatform.
 */
@Composable
internal expect fun AlongsideNavDisplay(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
)
