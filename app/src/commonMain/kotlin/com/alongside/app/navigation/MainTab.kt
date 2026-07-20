package com.alongside.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

/** The five bottom-bar destinations of the main app (`docs/screens.md`). */
internal enum class MainTab(
    val key: NavKey,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    TIMELINE(Timeline, "Timeline", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    PLACES(Places, "Places", Icons.Filled.Place, Icons.Outlined.Place),
    MATCHER(Matcher, "Matcher", Icons.Filled.Style, Icons.Outlined.Style),
    MATCH_LIST(MatchList, "Matches", Icons.Filled.Favorite, Icons.Outlined.Favorite),
}
