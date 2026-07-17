package com.alongside.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Text roles from the design that have no Material typography slot: the mono
 * overline/diary/digit/meta styles and the serif-italic brand voice.
 */
@Immutable
public class AlongsideExtendedTypography(
    public val overline: TextStyle,
    public val diaryBody: TextStyle,
    public val digit: TextStyle,
    public val meta: TextStyle,
    public val displaySerifItalic: TextStyle,
)

/** Non-composable fallback (system fonts) for the CompositionLocal default. */
internal fun defaultExtendedTypography(
    mono: FontFamily = FontFamily.Monospace,
    serif: FontFamily = FontFamily.Serif,
): AlongsideExtendedTypography =
    AlongsideExtendedTypography(
        overline =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.15.em,
            ),
        diaryBody =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 24.sp,
            ),
        digit =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 32.sp,
            ),
        meta =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
            ),
        displaySerifItalic =
            TextStyle(
                fontFamily = serif,
                fontStyle = FontStyle.Italic,
                fontSize = 40.sp,
                lineHeight = 48.sp,
            ),
    )

internal val LocalAlongsideExtendedTypography = staticCompositionLocalOf {
    defaultExtendedTypography()
}

public val MaterialTheme.alongsideTypography: AlongsideExtendedTypography
    @Composable @ReadOnlyComposable get() = LocalAlongsideExtendedTypography.current
