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
import com.alongside.core.ui.theme.alongsideColors

/** Cream "paper" card — callouts, list rows, sheets on the dark canvas. */
@Composable
public fun PaperCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.alongsideColors.paper,
        contentColor = MaterialTheme.alongsideColors.onPaper,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(AlongsideSpacing.xl), content = content)
    }
}

@Preview
@Composable
private fun PaperCardPreview() {
    AlongsideTheme {
        PaperCard {
            Text("Day 4 - Vinnytsia")
        }
    }
}
