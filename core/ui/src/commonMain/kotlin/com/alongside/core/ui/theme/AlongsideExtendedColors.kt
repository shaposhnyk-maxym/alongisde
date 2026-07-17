package com.alongside.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design tokens that have no Material color-scheme slot: the cream "paper" family,
 * the toast banner, muted text tiers, and the ink gradient stops.
 */
@Immutable
@Suppress("LongParameterList") // Token holder, mirrors Material's own ColorScheme shape.
public class AlongsideExtendedColors(
    public val paper: Color,
    public val onPaper: Color,
    public val onPaperSecondary: Color,
    public val paperWhite: Color,
    public val iconTileOnPaper: Color,
    public val toast: Color,
    public val onToast: Color,
    public val iconMuted: Color,
    public val labelMuted: Color,
    public val digitAccent: Color,
    public val gradientTop: Color,
    public val gradientBottom: Color,
)

internal val LocalAlongsideExtendedColors = staticCompositionLocalOf {
    AlongsideExtendedColors(
        paper = AlongsideColor.Paper,
        onPaper = AlongsideColor.TextOnPaper,
        onPaperSecondary = AlongsideColor.TextOnPaperSecondary,
        paperWhite = AlongsideColor.PaperWhite,
        iconTileOnPaper = AlongsideColor.IconTileOnPaper,
        toast = AlongsideColor.ToastBrown,
        onToast = AlongsideColor.TextOnInk,
        iconMuted = AlongsideColor.IconMuted,
        labelMuted = AlongsideColor.LabelMuted,
        digitAccent = AlongsideColor.Primary,
        gradientTop = AlongsideColor.Ink,
        gradientBottom = AlongsideColor.InkGradientBottom,
    )
}

public val MaterialTheme.alongsideColors: AlongsideExtendedColors
    @Composable @ReadOnlyComposable get() = LocalAlongsideExtendedColors.current
