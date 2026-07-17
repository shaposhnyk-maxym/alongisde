package com.alongside.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.alongsideColors

private val TabDotSize = 10.dp
private const val INACTIVE_DOT_ALPHA = 0.45f

/**
 * Main-app chrome: bottom navigation over the current tab's content. Icons are placeholder
 * dots until the real icon set from `design/main-app.pdf` lands with its feature milestones.
 */
@Composable
internal fun MainShell(
    currentTab: MainTab,
    onTabSelect: (MainTab) -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
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
                            val color =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.alongsideColors.labelMuted.copy(alpha = INACTIVE_DOT_ALPHA)
                                }
                            Box(Modifier.size(TabDotSize).background(color, CircleShape))
                        },
                        label = { Text(tab.label) },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.alongsideColors.labelMuted,
                                indicatorColor = MaterialTheme.colorScheme.background,
                            ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).background(MaterialTheme.colorScheme.background)) {
            content()
        }
    }
}
