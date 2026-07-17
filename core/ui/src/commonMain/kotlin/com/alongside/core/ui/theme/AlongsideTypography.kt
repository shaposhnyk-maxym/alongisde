package com.alongside.core.ui.theme

import alongside.core.ui.generated.resources.Res
import alongside.core.ui.generated.resources.ibm_plex_mono_medium
import alongside.core.ui.generated.resources.ibm_plex_mono_regular
import alongside.core.ui.generated.resources.inter_medium
import alongside.core.ui.generated.resources.inter_regular
import alongside.core.ui.generated.resources.inter_semibold
import alongside.core.ui.generated.resources.lora_italic
import alongside.core.ui.generated.resources.lora_medium
import alongside.core.ui.generated.resources.lora_medium_italic
import alongside.core.ui.generated.resources.lora_regular
import alongside.core.ui.generated.resources.lora_semibold
import alongside.core.ui.generated.resources.lora_semibold_italic
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font

@Composable
internal fun loraFamily(): FontFamily =
    FontFamily(
        Font(Res.font.lora_regular, FontWeight.Normal),
        Font(Res.font.lora_medium, FontWeight.Medium),
        Font(Res.font.lora_semibold, FontWeight.SemiBold),
        Font(Res.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.lora_medium_italic, FontWeight.Medium, FontStyle.Italic),
        Font(Res.font.lora_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    )

@Composable
internal fun interFamily(): FontFamily =
    FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
    )

@Composable
internal fun plexMonoFamily(): FontFamily =
    FontFamily(
        Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal),
        Font(Res.font.ibm_plex_mono_medium, FontWeight.Medium),
    )

/**
 * Material typography mapped to the design's roles: Lora (serif) for
 * display/headline/title, Inter (sans) for body/label. Mono roles live in
 * [AlongsideExtendedTypography].
 */
@Composable
internal fun alongsideTypography(
    serif: FontFamily = loraFamily(),
    sans: FontFamily = interFamily(),
): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = serif),
        displayMedium = base.displayMedium.copy(fontFamily = serif),
        displaySmall = base.displaySmall.copy(fontFamily = serif),
        headlineLarge = base.headlineLarge.copy(fontFamily = serif),
        headlineMedium = base.headlineMedium.copy(fontFamily = serif),
        headlineSmall = base.headlineSmall.copy(fontFamily = serif),
        titleLarge = base.titleLarge.copy(fontFamily = serif),
        titleMedium = base.titleMedium.copy(fontFamily = serif, fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontFamily = serif, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = sans),
        bodyMedium = base.bodyMedium.copy(fontFamily = sans),
        bodySmall = base.bodySmall.copy(fontFamily = sans),
        labelLarge = base.labelLarge.copy(fontFamily = sans, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = sans, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = sans, fontWeight = FontWeight.Medium),
    )
}

@Composable
internal fun alongsideExtendedTypography(
    mono: FontFamily = plexMonoFamily(),
    serif: FontFamily = loraFamily(),
): AlongsideExtendedTypography = defaultExtendedTypography(mono = mono, serif = serif)
