package com.alongside.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Eyeballed from `design/main-app.pdf` / `design/login_pairing_onboarding.pdf` (dark ink canvas,
 * warm paper cards, burnt-orange accent) - not pixel-sampled. Revisit with exact values once a
 * real visual-design pass happens.
 */
public object AlongsideColor {
    public val InkBackground: Color = Color(0xFF0A0A0C)
    public val PaperCard: Color = Color(0xFFF3ECE0)
    public val AccentOrange: Color = Color(0xFFE0793C)
    public val TextOnInk: Color = Color(0xFFEDEDED)
    public val TextOnPaper: Color = Color(0xFF1A1A1A)
}
