package de.ruff.dimension.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import de.ruff.dimension.R

/*
 * Schriften: Cinzel für Überschriften, Alegreya für Fließtext. Beide liegen als
 * variable Originaldateien aus github.com/google/fonts unter res/font (SIL OFL 1.1,
 * Lizenztexte unter res/raw und im Lizenz-Bildschirm der App).
 */

@OptIn(ExperimentalTextApi::class)
private fun cinzel(weight: FontWeight) =
    Font(
        R.font.cinzel,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

@OptIn(ExperimentalTextApi::class)
private fun alegreya(weight: FontWeight) =
    Font(
        R.font.alegreya,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

internal val Cinzel =
    FontFamily(
        cinzel(FontWeight.Normal),
        cinzel(FontWeight.SemiBold),
        cinzel(FontWeight.Bold),
    )

internal val Alegreya =
    FontFamily(
        alegreya(FontWeight.Normal),
        alegreya(FontWeight.Medium),
        alegreya(FontWeight.Bold),
    )

private val Base = Typography()

private fun TextStyle.heading(weight: FontWeight = FontWeight.SemiBold) = copy(fontFamily = Cinzel, fontWeight = weight)

private fun TextStyle.body(weight: FontWeight? = null) = copy(fontFamily = Alegreya, fontWeight = weight ?: fontWeight)

internal val RuffTypography =
    Typography(
        displayLarge = Base.displayLarge.heading(FontWeight.Bold),
        displayMedium = Base.displayMedium.heading(FontWeight.Bold),
        displaySmall = Base.displaySmall.heading(FontWeight.Bold),
        headlineLarge = Base.headlineLarge.heading(FontWeight.Bold),
        headlineMedium = Base.headlineMedium.heading(),
        headlineSmall = Base.headlineSmall.heading(),
        titleLarge = Base.titleLarge.heading(),
        titleMedium = Base.titleMedium.heading(),
        titleSmall = Base.titleSmall.heading(),
        bodyLarge = Base.bodyLarge.body(),
        bodyMedium = Base.bodyMedium.body(),
        bodySmall = Base.bodySmall.body(),
        labelLarge = Base.labelLarge.body(FontWeight.Bold),
        labelMedium = Base.labelMedium.body(FontWeight.Bold),
        labelSmall = Base.labelSmall.body(FontWeight.Medium),
    )

internal fun Typography.withFont(fontFamily: FontFamily): Typography =
    copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
    )
