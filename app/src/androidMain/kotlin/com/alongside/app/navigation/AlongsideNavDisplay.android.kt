package com.alongside.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay

@Composable
internal actual fun AlongsideNavDisplay(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { onBack() },
        entryProvider = entryProvider,
    )
}
