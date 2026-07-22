package com.alongside.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideTypography

/**
 * Shared top-of-screen title row - the same treatment every top-level tab screen uses for
 * visual consistency (title in [MaterialTheme.typography.headlineSmall], an optional trailing
 * slot for a short meta string, e.g. Matcher's "N left" counter).
 *
 * The surrounding inset ([AlongsideSpacing.xl] horizontal, [AlongsideSpacing.lg] vertical) is
 * baked in here, not left to each call site - screens picking their own value is exactly how
 * this drifted (Matcher/Places/Home each used a different one before this existed). Every tab
 * screen is hosted by the same `MainShell` `Scaffold` (bottom nav, no top bar), which already
 * reserves the status bar inset in the padding it hands to content - a screen must NOT also
 * apply its own `Modifier.statusBarsPadding()` around this, or the header sits visibly lower on
 * that one screen than on every other tab.
 */
@Composable
public fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = AlongsideSpacing.xl, vertical = AlongsideSpacing.lg),
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
