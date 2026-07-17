package com.alongside.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

public enum class CircleIconButtonStyle {
    /** Dark circle with a muted glyph — matcher "skip". */
    Dark,

    /** Larger terracotta circle with a white glyph — matcher "want to go". */
    Primary,
}

/** Circular action button pair from the matcher screen. */
@Composable
public fun CircleIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    style: CircleIconButtonStyle = CircleIconButtonStyle.Dark,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val size =
        when (style) {
            CircleIconButtonStyle.Dark -> 48.dp
            CircleIconButtonStyle.Primary -> 64.dp
        }
    val containerColor =
        when (style) {
            CircleIconButtonStyle.Dark -> MaterialTheme.colorScheme.surface
            CircleIconButtonStyle.Primary -> MaterialTheme.colorScheme.primary
        }
    val contentColor =
        when (style) {
            CircleIconButtonStyle.Dark -> MaterialTheme.alongsideColors.iconMuted
            CircleIconButtonStyle.Primary -> MaterialTheme.colorScheme.onPrimary
        }
    val semanticsModifier =
        if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        }
    Surface(
        onClick = onClick,
        modifier = semanticsModifier.size(size),
        enabled = enabled,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Preview
@Composable
private fun CircleIconButtonPreview() {
    AlongsideTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg)) {
            CircleIconButton(onClick = {}, contentDescription = "Skip place") {
                Text("✕")
            }
            CircleIconButton(
                onClick = {},
                contentDescription = "Want to go",
                style = CircleIconButtonStyle.Primary,
            ) {
                Text("♥")
            }
        }
    }
}
