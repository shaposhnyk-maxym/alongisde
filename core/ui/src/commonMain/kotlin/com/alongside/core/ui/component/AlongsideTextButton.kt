package com.alongside.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

private val MinHeight = 52.dp
private val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

/** Plain muted text button ("Not now"). */
@Composable
public fun AlongsideTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = MinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.alongsideColors.labelMuted,
            ),
        contentPadding = ContentPadding,
    ) {
        Text(text)
    }
}

@Preview
@Composable
private fun AlongsideTextButtonPreview() {
    AlongsideTheme {
        AlongsideTextButton(text = "Not now", onClick = {})
    }
}
