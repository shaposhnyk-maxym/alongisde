package com.alongside.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme

@Composable
public fun AlongsideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AlongsideButtonVariant = AlongsideButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    when (variant) {
        AlongsideButtonVariant.Primary ->
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = ButtonDefaults.shape,
                contentPadding = contentPadding,
            ) {
                Text(text)
            }

        AlongsideButtonVariant.Secondary ->
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = ButtonDefaults.outlinedShape,
                contentPadding = contentPadding,
            ) {
                Text(text)
            }
    }
}

@Preview
@Composable
private fun AlongsideButtonPrimaryPreview() {
    AlongsideTheme {
        AlongsideButton(text = "Continue with Google", onClick = {})
    }
}

@Preview
@Composable
private fun AlongsideButtonSecondaryPreview() {
    AlongsideTheme {
        AlongsideButton(text = "Join a Trip", onClick = {}, variant = AlongsideButtonVariant.Secondary)
    }
}
