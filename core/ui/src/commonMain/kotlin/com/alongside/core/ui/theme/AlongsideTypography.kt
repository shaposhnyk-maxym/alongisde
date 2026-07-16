package com.alongside.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle

// Placeholder: FontFamily.Serif/Default until real custom fonts exist under design/fonts/.
private val displayStyle =
    TextStyle(
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
    )

public val AlongsideTypography: Typography =
    Typography().let { base ->
        base.copy(
            displayLarge = base.displayLarge.merge(displayStyle),
            displayMedium = base.displayMedium.merge(displayStyle),
            headlineLarge = base.headlineLarge.merge(displayStyle),
            headlineMedium = base.headlineMedium.merge(displayStyle),
        )
    }
