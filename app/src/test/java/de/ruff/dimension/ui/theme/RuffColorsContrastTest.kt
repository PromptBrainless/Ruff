package de.ruff.dimension.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** WCAG-2.x-Kontrast der Textrollen im dunklen Ruff-Schema (AA: mindestens 4,5:1). */
class RuffColorsContrastTest {
    private fun channel(c: Float): Double = if (c <= 0.04045f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color): Double = 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val (light, dark) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    @Test
    fun textRolesMeetWcagAa() {
        val s = RuffDarkColors
        val surfaces =
            listOf(
                s.background,
                s.surface,
                s.surfaceContainerLowest,
                s.surfaceContainerLow,
                s.surfaceContainer,
                s.surfaceContainerHigh,
                s.surfaceContainerHighest,
                s.surfaceVariant,
                // Hintergrund der ausgewählten Karte in Liste/Detail (zweispaltig).
                s.secondaryContainer,
            )
        val pairs =
            buildList {
                surfaces.forEach { bg ->
                    listOf(s.onSurface, s.onSurfaceVariant, s.primary, s.secondary, s.tertiary, s.error).forEach { add(it to bg) }
                }
                add(s.onBackground to s.background)
                add(s.onPrimary to s.primary)
                add(s.onPrimaryContainer to s.primaryContainer)
                add(s.onSecondary to s.secondary)
                add(s.onSecondaryContainer to s.secondaryContainer)
                add(s.onTertiary to s.tertiary)
                add(s.onTertiaryContainer to s.tertiaryContainer)
                add(s.onError to s.error)
                add(s.onErrorContainer to s.errorContainer)
                add(s.inverseOnSurface to s.inverseSurface)
                // Text auf Bildern liegt immer auf mindestens diesem Scrim (85 % Schwarz über Weiß = schlimmster Fall).
                add(Pergament to Color(0xFF262626))
                add(Glut to Color(0xFF262626))
            }
        val failures = pairs.filter { (fg, bg) -> contrast(fg, bg) < 4.5 }
        assertTrue("Unter 4,5:1: $failures", failures.isEmpty())
    }
}
