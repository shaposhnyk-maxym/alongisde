package com.alongside.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography

public enum class DigitTileTone {
    /** Terracotta glyph — trip-day counter, invite code. */
    Accent,

    /** Cream glyph — quieter tiles. */
    Paper,
}

/** Dark rounded tile with a single large mono character (flip-counter digits, invite codes). */
@Composable
public fun DigitTile(
    char: Char,
    modifier: Modifier = Modifier,
    tone: DigitTileTone = DigitTileTone.Accent,
) {
    val color =
        when (tone) {
            DigitTileTone.Accent -> MaterialTheme.alongsideColors.digitAccent
            DigitTileTone.Paper -> MaterialTheme.alongsideColors.paper
        }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(
            modifier = Modifier.size(width = 44.dp, height = 56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = char.toString(),
                style = MaterialTheme.alongsideTypography.digit,
                color = color,
            )
        }
    }
}

@Preview
@Composable
private fun DigitTilePreview() {
    AlongsideTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm)) {
            "7F3K9Q".forEach { DigitTile(char = it) }
            DigitTile(char = '4', tone = DigitTileTone.Paper)
        }
    }
}
