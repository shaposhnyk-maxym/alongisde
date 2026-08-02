package com.alongside.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alongside.core.ui.theme.alongsideColors

private const val TAB_CONTENT_CROSSFADE_DURATION_MILLIS = 220

/**
 * Main-app chrome: one persistent bottom navigation bar over the current tab's content.
 * Hoisted above navigation (docs/roadmap.md M21.4) - a single instance for the whole
 * tab-browsing session, called once from [com.alongside.app.AlongsideApp] rather than freshly
 * inside every `entry<Tab>`, so `NavigationBarItem`'s own ripple/pill-indicator animation state
 * survives a tab switch instead of resetting with it. Only [content] crossfades, via
 * `AnimatedContent` keyed on [currentTab] - the bar itself sits outside that transition
 * entirely, so it never re-renders as part of a tab switch. [content] takes the target
 * [MainTab] (not a plain no-arg lambda) specifically so `AnimatedContent` gets a real pure
 * function of its own target state to animate between - a lambda that instead closed over the
 * live, already-mutated tab selection would show identical (new) content on both the outgoing
 * and incoming side of the crossfade, i.e. no visible animation at all.
 *
 * Implemented as a hand-rolled `AnimatedContent` rather than relying on
 * `androidx.navigation3.ui.NavDisplay`'s own transition support - that API has no iOS artifacts
 * yet (see `AlongsideNavDisplay`'s own kdoc), so a shared, common-code animation is the only way
 * to get identical behavior on both platforms.
 */
@Composable
internal fun MainShell(
    currentTab: MainTab,
    onTabSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (MainTab) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onTabSelect(tab) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.alongsideColors.labelMuted,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.alongsideColors.labelMuted,
                                // Previously `background` (Ink, near-black) on a `surface`
                                // (SurfaceInk, also dark) bar - the M3 pill indicator was
                                // already animating, just invisible against its own background
                                // (docs/roadmap.md M21.4). `secondaryContainer` is a
                                // theme-derived color meant for exactly this kind of "selected
                                // chip/pill" fill, giving real contrast against `surface`.
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).background(MaterialTheme.colorScheme.background)) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(tween(TAB_CONTENT_CROSSFADE_DURATION_MILLIS)) togetherWith
                        fadeOut(tween(TAB_CONTENT_CROSSFADE_DURATION_MILLIS))
                },
                label = "main-tab-content",
            ) { tab ->
                content(tab)
            }
        }
    }
}
