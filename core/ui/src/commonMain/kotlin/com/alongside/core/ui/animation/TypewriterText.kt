package com.alongside.core.ui.animation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val CursorBlinkMillis = 500L

/**
 * Reveals [text] one character at a time, [charDelayMillis] apart, then calls [onComplete].
 * With [showCursor] a blinking `_` trails the text — the diary's typewriter voice.
 */
@Composable
public fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    charDelayMillis: Long = 40L,
    style: TextStyle = LocalTextStyle.current,
    showCursor: Boolean = false,
    onComplete: () -> Unit = {},
) {
    var visibleCharCount by remember(text) { mutableIntStateOf(0) }
    var cursorVisible by remember { mutableStateOf(true) }
    val currentOnComplete by rememberUpdatedState(onComplete)

    LaunchedEffect(text, charDelayMillis) {
        visibleCharCount = 0
        for (count in 1..text.length) {
            delay(charDelayMillis)
            visibleCharCount = count
        }
        currentOnComplete()
    }

    if (showCursor) {
        LaunchedEffect(Unit) {
            while (isActive) {
                delay(CursorBlinkMillis)
                cursorVisible = !cursorVisible
            }
        }
    }

    val cursor = if (showCursor && cursorVisible) "_" else ""
    Text(text = text.take(visibleCharCount) + cursor, modifier = modifier, style = style)
}

@Preview
@Composable
private fun TypewriterTextPreview() {
    AlongsideTheme {
        // Fixed size so the screenshot capture has a non-empty root even at the animation's
        // very first (empty-text) frame.
        TypewriterText(text = "Your trip, together", modifier = Modifier.size(240.dp, 40.dp))
    }
}

@Preview
@Composable
private fun TypewriterTextCursorPreview() {
    AlongsideTheme {
        TypewriterText(
            text = "We wandered into the old quarter",
            modifier = Modifier.size(280.dp, 40.dp),
            showCursor = true,
        )
    }
}
