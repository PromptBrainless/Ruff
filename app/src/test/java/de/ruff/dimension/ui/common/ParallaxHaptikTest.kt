package de.ruff.dimension.ui.common

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Parallax-Regel und Haptik-Zuordnung. */
class ParallaxHaptikTest {
    @Test
    fun reduzierterModusOhneParallax() {
        assertEquals(0f, parallaxOffset(scrollPx = 500, maxPx = 300f, reduced = true), 0f)
    }

    @Test
    fun parallaxFolgtDemScrollenBegrenzt() {
        assertEquals(0f, parallaxOffset(0, 300f, reduced = false), 0f)
        assertEquals(100 * PARALLAX_FAKTOR, parallaxOffset(100, 300f, reduced = false), 1e-4f)
        assertEquals(300f, parallaxOffset(10_000, 300f, reduced = false), 0f)
        assertEquals(0f, parallaxOffset(-50, 300f, reduced = false), 0f)
        assertEquals(0f, parallaxOffset(100, 0f, reduced = false), 0f)
    }

    @Test
    fun parallaxBlendetLeichtAus() {
        assertEquals(1f, parallaxAlpha(0f, 300f), 0f)
        assertEquals(0.6f, parallaxAlpha(300f, 300f), 1e-4f)
        assertEquals(1f, parallaxAlpha(10f, 0f), 0f)
    }

    @Test
    fun haptikZuordnung() {
        assertEquals(HapticFeedbackType.TextHandleMove, hapticType(RuffHaptik.AUSWAHL))
        assertEquals(HapticFeedbackType.LongPress, hapticType(RuffHaptik.BESTAETIGUNG))
        assertNotEquals(hapticType(RuffHaptik.AUSWAHL), hapticType(RuffHaptik.BESTAETIGUNG))
    }
}
