package com.alongside.core.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

private val DotSize = 6.dp
private val SelectedDotWidth = 18.dp
private val DotShape = RoundedCornerShape(percent = 50)
private const val INACTIVE_DOT_ALPHA = 0.5f

/** Pager indicator from the diary card: small muted dots, the active page an orange pill. */
@Composable
public fun PagerDots(
    pageCount: Int,
    selectedPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DotSize),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == selectedPage
            val width by animateDpAsState(if (selected) SelectedDotWidth else DotSize)
            val color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.alongsideColors.labelMuted.copy(alpha = INACTIVE_DOT_ALPHA)
                }
            Box(
                modifier =
                    Modifier
                        .width(width)
                        .height(DotSize)
                        .background(color, DotShape)
                        .testTag("pager-dot-$index"),
            )
        }
    }
}

@Preview
@Composable
private fun PagerDotsPreview() {
    AlongsideTheme {
        PagerDots(pageCount = 5, selectedPage = 3)
    }
}
