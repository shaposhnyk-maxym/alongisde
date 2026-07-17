package com.alongside.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography

/** Mono uppercase letterspaced label used above sections and inside cards. */
@Composable
public fun OverlineLabel(
    text: String,
    modifier: Modifier = Modifier,
    tone: OverlineLabelTone = OverlineLabelTone.Muted,
) {
    val color =
        when (tone) {
            OverlineLabelTone.Accent -> MaterialTheme.colorScheme.primary
            OverlineLabelTone.Muted -> MaterialTheme.alongsideColors.labelMuted
        }
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.alongsideTypography.overline,
        color = color,
    )
}

@Preview
@Composable
private fun OverlineLabelPreview() {
    AlongsideTheme {
        Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm)) {
            OverlineLabel(text = "Step 1 of 4", tone = OverlineLabelTone.Accent)
            OverlineLabel(text = "New Matches")
        }
    }
}
