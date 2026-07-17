package com.alongside.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme

/** Elevated dark card on the ink canvas — trip-day counter, diary text, locked-day state. */
@Composable
public fun InkCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(AlongsideSpacing.xl), content = content)
    }
}

@Preview
@Composable
private fun InkCardPreview() {
    AlongsideTheme {
        InkCard {
            Text("Day 5 isn't ready yet")
        }
    }
}
