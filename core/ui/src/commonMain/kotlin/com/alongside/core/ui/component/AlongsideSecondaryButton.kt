package com.alongside.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

private val MinHeight = 52.dp
private val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

/** Outlined button on the dark canvas ("Copy Code"). */
@Composable
public fun AlongsideSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = MinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.alongsideColors.paper,
            ),
        contentPadding = ContentPadding,
    ) {
        Text(text)
    }
}

@Preview
@Composable
private fun AlongsideSecondaryButtonPreview() {
    AlongsideTheme {
        AlongsideSecondaryButton(text = "Copy Code", onClick = {})
    }
}
