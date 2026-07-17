package com.alongside.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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

private val BannerDotSize = 10.dp

/** Warm dark banner with a leading terracotta dot ("Added ... from Google Maps"). */
@Composable
public fun DotBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.alongsideColors.toast,
        contentColor = MaterialTheme.alongsideColors.onToast,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AlongsideSpacing.lg, vertical = AlongsideSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(BannerDotSize)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
private fun DotBannerPreview() {
    AlongsideTheme {
        DotBanner(text = "Added \"Rynok Square\" from Google Maps")
    }
}
