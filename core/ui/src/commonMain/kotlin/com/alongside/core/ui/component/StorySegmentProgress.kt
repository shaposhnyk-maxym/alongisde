package com.alongside.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideColor
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme

private val SegmentHeight = 3.dp
private val SegmentShape = RoundedCornerShape(percent = 50)
private val PastSegmentColor = AlongsideColor.TextOnInk.copy(alpha = 0.6f)
private val TrackColor = AlongsideColor.SurfaceInk

/**
 * Stories-style top progress bar from the recap flow: one thin segment per story,
 * completed segments filled, the active one partially filled by [activeProgress] (0..1).
 */
@Composable
public fun StorySegmentProgress(
    segmentCount: Int,
    activeSegment: Int,
    modifier: Modifier = Modifier,
    activeProgress: Float = 0f,
) {
    val coercedProgress = activeProgress.coerceIn(0f, 1f)
    Row(
        modifier = modifier.fillMaxWidth().height(SegmentHeight),
        horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.xs),
    ) {
        repeat(segmentCount) { index ->
            val segmentModifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(SegmentShape)
                    .background(if (index < activeSegment) PastSegmentColor else TrackColor)
                    .testTag("story-segment")
            if (index == activeSegment) {
                Box(modifier = segmentModifier.progressSemantics(coercedProgress)) {
                    if (coercedProgress > 0f) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(coercedProgress)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            } else {
                Box(modifier = segmentModifier)
            }
        }
    }
}

@Preview
@Composable
private fun StorySegmentProgressPreview() {
    AlongsideTheme {
        Box(
            modifier =
                Modifier
                    .width(240.dp)
                    .background(Color.Black),
        ) {
            StorySegmentProgress(segmentCount = 4, activeSegment = 2, activeProgress = 0.6f)
        }
    }
}
