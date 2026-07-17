package com.alongside.core.ui.component

import alongside.core.ui.generated.resources.Res
import alongside.core.ui.generated.resources.ic_google
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors
import org.jetbrains.compose.resources.painterResource

private val MinHeight = 52.dp

/**
 * A clickable cream "paper" surface with a Google icon ("Continue with Google") - its own
 * [Surface], the same way [PaperCard] is, rather than a [androidx.compose.material3.Button]
 * painted paper-colored. Doesn't need wrapping in [PaperCard] - it already carries its own
 * background.
 */
@Composable
public fun AlongsideOnPaperButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = MinHeight),
        enabled = enabled,
        color = MaterialTheme.alongsideColors.paperWhite,
        contentColor = MaterialTheme.alongsideColors.onPaper,
        shape = MaterialTheme.shapes.medium,
        // The design's warm hairline border - keeps the white button legible when it sits
        // inside the cream PaperCard rather than directly on the ink canvas.
        border = BorderStroke(1.dp, MaterialTheme.alongsideColors.iconTileOnPaper),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(Res.drawable.ic_google),
                contentDescription = null,
                // The Google mark keeps its own brand colors instead of the content tint.
                tint = Color.Unspecified,
            )
            Text(text)
        }
    }
}

// Wrapped in the dark canvas this button actually sits on in the app (InkGradientBackground) -
// its paperWhite surface is otherwise indistinguishable from a plain white preview background.
@Preview
@Composable
private fun AlongsideOnPaperButtonPreview() {
    AlongsideTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(AlongsideSpacing.lg)) {
                AlongsideOnPaperButton(text = "Continue with Google", onClick = {})
            }
        }
    }
}
