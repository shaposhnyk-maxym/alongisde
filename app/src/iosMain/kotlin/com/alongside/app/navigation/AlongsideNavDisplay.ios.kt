package com.alongside.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

// Plain top-entry renderer: navigation3-ui has no iOS artifacts yet (see the expect KDoc).
@Composable
internal actual fun AlongsideNavDisplay(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier,
) {
    val key = backStack.lastOrNull() ?: return
    entryProvider(key).Content()
}
