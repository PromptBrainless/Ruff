package de.ruff.dimension.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reine Regeln des Favoriten-Herzens (ohne Compose-Laufzeit). */
class FavoriteHeartTest {
    @Test
    fun `Favorisieren bestaetigt deutlich, Entfernen ist ein leichter Tick`() {
        assertEquals(RuffHaptik.BESTAETIGUNG, favoriteHaptik(becomesFavorite = true))
        assertEquals(RuffHaptik.AUSWAHL, favoriteHaptik(becomesFavorite = false))
    }

    @Test
    fun `Screenreader-Zustand und Testtags sind eindeutig`() {
        assertEquals("Favorit", favoriteStateDescription(true))
        assertEquals("Kein Favorit", favoriteStateDescription(false))
        assertEquals("favorit_herz_liste_A-1", favoriteHeartTag("A-1", "liste"))
        assertTrue(favoriteHeartTag("A-1", "liste") != favoriteHeartTag("A-1", "detail"))
    }

    @Test
    fun `Sprung bleibt in vernuenftigen Grenzen`() {
        assertTrue(HerzSprung.SPITZE in 1.1f..1.4f)
        assertTrue(HerzSprung.NACHFEDERN in 0.85f..1f)
        assertTrue(HerzSprung.EINSINKEN in 0.75f..1f)
    }
}
