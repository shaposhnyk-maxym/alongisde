package com.alongside.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideTypography

/**
 * Shared top-of-screen title row - the same treatment every top-level tab screen uses for
 * visual consistency (title in [MaterialTheme.typography.headlineSmall], an optional trailing
 * slot for a short meta string, e.g. Matcher's "N left" counter). Each screen still owns its own
 * background/padding around it - this is only the title row itself.
 */
@Composable
public fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        trailing()
    }
}

@Preview
@Composable
private fun ScreenHeaderPreview() {
    AlongsideTheme {
        ScreenHeader(title = "Places")
    }
}

@Preview
@Composable
private fun ScreenHeaderWithTrailingPreview() {
    AlongsideTheme {
        ScreenHeader(title = "Matcher") {
            Text(text = "14 left", style = MaterialTheme.alongsideTypography.meta)
        }
    }
}
