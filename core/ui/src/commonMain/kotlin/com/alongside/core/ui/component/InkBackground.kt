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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

/**
 * Flat near-black background - [InkGradientBackground] without the gradient sheen, for screens
 * that want the same dark canvas without its vertical brush (e.g. the Places list, matching its
 * design reference's flatter, cooler-reading background).
 */
@Composable
public fun InkBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onBackground,
    ) {
        Box(modifier = modifier.background(MaterialTheme.alongsideColors.gradientTop), content = content)
    }
}

@Preview
@Composable
private fun InkBackgroundPreview() {
    AlongsideTheme {
        InkBackground(modifier = Modifier.size(240.dp, 160.dp)) {
            Text("Alongside")
        }
    }
}
