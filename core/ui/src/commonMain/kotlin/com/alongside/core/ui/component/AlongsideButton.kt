package com.alongside.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

private val ButtonMinHeight = 52.dp

@Composable
public fun AlongsideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AlongsideButtonVariant = AlongsideButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val shape = MaterialTheme.shapes.medium
    val contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    val sizedModifier = modifier.defaultMinSize(minHeight = ButtonMinHeight)
    when (variant) {
        AlongsideButtonVariant.Primary ->
            Button(
                onClick = onClick,
                modifier = sizedModifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
            ) {
                Text(text)
            }

        AlongsideButtonVariant.Secondary ->
            OutlinedButton(
                onClick = onClick,
                modifier = sizedModifier,
                enabled = enabled,
                shape = shape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.alongsideColors.paper,
                    ),
                contentPadding = contentPadding,
            ) {
                Text(text)
            }

        AlongsideButtonVariant.OnPaper ->
            Button(
                onClick = onClick,
                modifier = sizedModifier,
                enabled = enabled,
                shape = shape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.alongsideColors.paperWhite,
                        contentColor = MaterialTheme.alongsideColors.onPaper,
                    ),
                contentPadding = contentPadding,
            ) {
                Text(text)
            }

        AlongsideButtonVariant.Text ->
            TextButton(
                onClick = onClick,
                modifier = sizedModifier,
                enabled = enabled,
                shape = shape,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.alongsideColors.labelMuted,
                    ),
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
        AlongsideButton(text = "Allow Photo Access", onClick = {})
    }
}

@Preview
@Composable
private fun AlongsideButtonSecondaryPreview() {
    AlongsideTheme {
        AlongsideButton(text = "Copy Code", onClick = {}, variant = AlongsideButtonVariant.Secondary)
    }
}

@Preview
@Composable
private fun AlongsideButtonOnPaperPreview() {
    AlongsideTheme {
        Surface(color = MaterialTheme.alongsideColors.paper) {
            Box(modifier = Modifier.padding(AlongsideSpacing.lg)) {
                AlongsideButton(
                    text = "Continue with Google",
                    onClick = {},
                    variant = AlongsideButtonVariant.OnPaper,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AlongsideButtonTextPreview() {
    AlongsideTheme {
        AlongsideButton(text = "Not now", onClick = {}, variant = AlongsideButtonVariant.Text)
    }
}
