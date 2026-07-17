package com.alongside.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme

private val MinHeight = 52.dp
private val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

/** Filled terracotta button on the dark canvas ("Allow Photo Access", "Create Trip"). */
@Composable
public fun AlongsidePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = MinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = ContentPadding,
    ) {
        Text(text)
    }
}

@Preview
@Composable
private fun AlongsidePrimaryButtonPreview() {
    AlongsideTheme {
        AlongsidePrimaryButton(text = "Allow Photo Access", onClick = {})
    }
}
