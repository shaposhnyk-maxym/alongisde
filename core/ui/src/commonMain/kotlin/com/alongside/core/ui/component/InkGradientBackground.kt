package com.alongside.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

/**
 * Hero-screen background: near-black ink with a subtle vertical gradient.
 * Provides [androidx.compose.material3.ColorScheme.onBackground] as the content color,
 * so text inside is light by default.
 */
@Composable
public fun InkGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val brush =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.alongsideColors.gradientTop,
                    MaterialTheme.alongsideColors.gradientBottom,
                ),
        )
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onBackground,
    ) {
        Box(modifier = modifier.background(brush), content = content)
    }
}

@Preview
@Composable
private fun InkGradientBackgroundPreview() {
    AlongsideTheme {
        InkGradientBackground(modifier = Modifier.size(240.dp, 160.dp)) {
            Text("Alongside")
        }
    }
}
