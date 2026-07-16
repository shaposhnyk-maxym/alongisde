package com.alongside.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.alongside.core.ui.theme.AlongsideTheme
import kotlin.math.roundToInt

/**
 * Animates a number from [startValue] to [targetValue] - works for both count-up and count-down
 * depending on which side of [startValue] the [targetValue] falls.
 */
@Composable
public fun CountUpText(
    targetValue: Int,
    modifier: Modifier = Modifier,
    startValue: Int = 0,
    durationMillis: Int = 800,
    style: TextStyle = LocalTextStyle.current,
) {
    val animatable = remember { Animatable(startValue.toFloat()) }
    LaunchedEffect(targetValue, durationMillis) {
        animatable.animateTo(targetValue.toFloat(), tween(durationMillis))
    }
    val displayValue by remember { derivedStateOf { animatable.value.roundToInt() } }
    Text(text = displayValue.toString(), modifier = modifier, style = style)
}

@Preview
@Composable
private fun CountUpTextPreview() {
    AlongsideTheme {
        CountUpText(targetValue = 7)
    }
}
