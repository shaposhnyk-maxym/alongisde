package com.alongside.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AlongsideColorScheme =
    darkColorScheme(
        background = AlongsideColor.InkBackground,
        onBackground = AlongsideColor.TextOnInk,
        surface = AlongsideColor.PaperCard,
        onSurface = AlongsideColor.TextOnPaper,
        primary = AlongsideColor.AccentOrange,
        onPrimary = AlongsideColor.TextOnPaper,
    )

private val AlongsideShapes =
    Shapes(
        small = RoundedCornerShape(50),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
    )

@Composable
public fun AlongsideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AlongsideColorScheme,
        typography = AlongsideTypography,
        shapes = AlongsideShapes,
        content = content,
    )
}
