package de.ruff.dimension.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reine Partikel-Logik (ohne Compose). */
class GlutPartikelTest {
    @Test
    fun reduzierterModusSchaltetPartikelAb() {
        assertFalse(particlesEnabled(RuffMotion(0f)))
        assertTrue(particlesEnabled(RuffMotion(1f)))
    }

    @Test
    fun fortschrittIstPeriodischUndImIntervall() {
        for (step in 0..400) {
            val t = step * 0.37f
            val p = emberProgress(t, speed = 0.07f, phase = 0.4f)
            assertTrue("p=$p", p >= 0f && p < 1f)
        }
        // Nach genau einer Periode (1 / speed) wieder am selben Punkt.
        assertEquals(emberProgress(0f, 0.05f, 0.3f), emberProgress(20f, 0.05f, 0.3f), 1e-4f)
        // Negative Zeit bleibt im Intervall.
        val negative = emberProgress(-3f, 0.1f, 0f)
        assertTrue(negative >= 0f && negative < 1f)
    }

    @Test
    fun partikelGluehtEinUndAus() {
        assertEquals(0f, emberAlpha(0f), 1e-4f)
        assertEquals(1f, emberAlpha(0.5f), 1e-4f)
        assertEquals(0f, emberAlpha(1f), 1e-4f)
        assertTrue(emberAlpha(0.25f) in 0f..1f)
    }

    @Test
    fun partikelSteigtAufUndBleibtImBild() {
        assertEquals(1f, emberY(0f), 0f)
        assertEquals(0f, emberY(1f), 0f)
        for (step in 0..200) {
            val x = emberX(baseX = 0.99f, sway = 0.05f, phase = 0.1f, timeSeconds = step * 0.1f)
            assertTrue(x in 0f..1f)
        }
    }

    @Test
    fun feldIstDeterministischUndBegrenzt() {
        val a = GlutPartikelFeld(10, seed = 3)
        val b = GlutPartikelFeld(10, seed = 3)
        assertTrue(a.baseX.contentEquals(b.baseX))
        assertEquals(MAX_GLUT_PARTIKEL, GlutPartikelFeld(500).size)
        assertEquals(0, GlutPartikelFeld(-1).size)
        val field = GlutPartikelFeld(MAX_GLUT_PARTIKEL)
        for (i in 0 until field.size) {
            assertTrue(field.speed[i] > 0f)
            assertTrue(field.radius[i] > 0f)
            assertTrue(field.baseX[i] in 0f..1f)
        }
    }
}
