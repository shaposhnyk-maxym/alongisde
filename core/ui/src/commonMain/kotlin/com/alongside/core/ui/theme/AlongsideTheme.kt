package com.alongside.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val AlongsideColorScheme =
    darkColorScheme(
        background = AlongsideColor.Ink,
        onBackground = AlongsideColor.TextOnInk,
        surface = AlongsideColor.SurfaceInk,
        onSurface = AlongsideColor.TextOnInk,
        surfaceVariant = AlongsideColor.SurfaceInk,
        onSurfaceVariant = AlongsideColor.TextMuted,
        primary = AlongsideColor.Primary,
        onPrimary = AlongsideColor.PaperWhite,
        primaryContainer = AlongsideColor.PrimaryDeep,
        onPrimaryContainer = AlongsideColor.PaperWhite,
        secondary = AlongsideColor.IconMuted,
        onSecondary = AlongsideColor.Ink,
        inverseSurface = AlongsideColor.Paper,
        inverseOnSurface = AlongsideColor.TextOnPaper,
        inversePrimary = AlongsideColor.PrimaryDeep,
        outline = AlongsideColor.OutlineSubtle,
        outlineVariant = AlongsideColor.OutlineSubtle,
        error = AlongsideColor.Destructive,
        onError = AlongsideColor.PaperWhite,
    )

private val AlongsideShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

@Composable
public fun AlongsideTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAlongsideExtendedTypography provides alongsideExtendedTypography(),
    ) {
        MaterialTheme(
            colorScheme = AlongsideColorScheme,
            typography = alongsideTypography(),
            shapes = AlongsideShapes,
            content = content,
        )
    }
}
