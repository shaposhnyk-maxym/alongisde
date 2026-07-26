package com.alongside.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.alongside.core.ui.component.AlongsidePrimaryButton
import com.alongside.core.ui.component.StoriesChrome
import com.alongside.core.ui.theme.AlongsideTheme

private val StoriesChromeSlideColors =
    listOf(
        Brush.linearGradient(listOf(Color(0xFFE2764A), Color(0xFF8A3A1F))),
        Brush.linearGradient(listOf(Color(0xFF4A7CE2), Color(0xFF1F3A8A))),
        Brush.linearGradient(listOf(Color(0xFF4AE28A), Color(0xFF1F8A4A))),
        Brush.linearGradient(listOf(Color(0xFFE2C94A), Color(0xFF8A7A1F))),
    )

/**
 * Stories-chrome UI iteration playground (docs/roadmap.md M20.2): tap zones, auto-advance timer,
 * and scale+fade transitions over colored-gradient placeholder slides - exercises [StoriesChrome]
 * from core:ui directly, no core:domain/feature:recap dependency, so the timing/gesture feel can
 * be tuned live via hot reload before M20.3 wires real Recap data into it.
 */
@Composable
internal fun StoriesChromeSection() {
    var openWindow by remember { mutableStateOf(false) }

    Section(title = "Stories chrome") {
        AlongsidePrimaryButton(text = "Open stories preview", onClick = { openWindow = true })
    }

    if (openWindow) {
        Window(onCloseRequest = { openWindow = false }, title = "Stories chrome") {
            AlongsideTheme {
                StoriesChromeDemo(onFinish = { openWindow = false })
            }
        }
    }
}

@Composable
private fun StoriesChromeDemo(onFinish: () -> Unit) {
    var activeIndex by remember { mutableIntStateOf(0) }
    Box(modifier = Modifier.size(360.dp, 640.dp)) {
        StoriesChrome(
            slideCount = StoriesChromeSlideColors.size,
            activeIndex = activeIndex,
            onActiveIndexChange = { activeIndex = it },
            onFinish = onFinish,
        ) { index, elapsedFraction ->
            Box(
                modifier = Modifier.fillMaxSize().background(StoriesChromeSlideColors[index]),
            ) {
                Text(
                    text = "Slide $index — ${(elapsedFraction * 100).toInt()}%",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
