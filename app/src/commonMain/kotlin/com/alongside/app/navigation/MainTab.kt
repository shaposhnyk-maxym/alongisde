package com.alongside.app.navigation

import androidx.navigation3.runtime.NavKey

/** The five bottom-bar destinations of the main app (`docs/screens.md`). */
internal enum class MainTab(
    val key: NavKey,
    val label: String,
) {
    HOME(Home, "Home"),
    TIMELINE(Timeline, "Timeline"),
    PLACES(Places, "Places"),
    MATCHER(Matcher, "Matcher"),
    MATCH_LIST(MatchList, "Matches"),
}
