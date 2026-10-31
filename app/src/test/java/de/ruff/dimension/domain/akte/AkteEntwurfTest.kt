package de.ruff.dimension.domain.akte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Editor-Entwurf: Umwandlung, Prüfung und Allergie-Zeilen; dazu die deutsche Anzeige. */
class AkteEntwurfTest {
    private val profil =
        CreatureProfile(
            creatureId = "V2-090",
            erscheinung = Angabe("Graue Haut", "Bild"),
            groesseM = Angabe(Spanne(1.6, 2.0), "Katalog"),
            pulsBpm = Angabe(GanzzahlSpanne(45, 60), "Annahme"),
            blutdruck = Angabe(Blutdruck(150, 95, "in Ruhe"), "Annahme"),
            allergien =
                Angabe(
                    listOf(Allergie("Sonnenlicht (UV)", AllergieArt.UNVERTRAEGLICHKEIT, Schwere.LEBENSBEDROHLICH, "Versteinerung")),
                    "Schwächen",
                ),
            verfassung = Angabe(Verfassung(Verfassungsstufe.ROBUST, "zäh"), "Bild"),
            iq = Angabe(GanzzahlSpanne(60, 85), "Lore"),
            wahnhaft = Angabe(false, "zielgerichtet"),
        )

    private fun ok(e: AkteEntwurf.Ergebnis) = (e as AkteEntwurf.Ergebnis.Ok).profile

    private fun fehler(e: AkteEntwurf.Ergebnis) = (e as AkteEntwurf.Ergebnis.Fehler).fehler

    private fun alleManuell(p: CreatureProfile): CreatureProfile =
        ProfilFeld.entries.fold(p) { acc, feld -> acc.mitHerkunft(feld, Herkunft.MANUELL) }

    @Test
    fun `Entwurf hin und zurueck ergibt dieselben Werte als manuell`() {
        val entwurf = AkteEntwurf.aus(profil)
        assertEquals(alleManuell(profil), ok(AkteEntwurf.zuProfil("V2-090", entwurf)))
    }

    @Test
    fun `leerer Entwurf ergibt leere Akte`() {
        assertTrue(ok(AkteEntwurf.zuProfil("A", AkteEntwurf.aus(null))).istLeer)
    }

    @Test
    fun `Dezimalkomma wird verstanden`() {
        val e = AkteEntwurf.aus(null) + mapOf("groesse.min" to "0,6", "groesse.max" to "0,9")
        assertEquals(Spanne(0.6, 0.9), ok(AkteEntwurf.zuProfil("A", e)).groesseM!!.wert)
    }

    @Test
    fun `einzelne Grenze ergibt Punktwert`() {
        val e = AkteEntwurf.aus(null) + mapOf("iq.min" to "100")
        assertEquals(GanzzahlSpanne(100, 100), ok(AkteEntwurf.zuProfil("A", e)).iq!!.wert)
    }

    @Test
    fun `fehlerhafte Eingaben werden je Feld gemeldet`() {
        val e =
            AkteEntwurf.aus(null) +
                mapOf(
                    "groesse.min" to "abc",
                    "gewicht.min" to "50",
                    "gewicht.max" to "10",
                    "puls.min" to "-3",
                    "blutdruck.sys" to "120",
                    "verfassung.text" to "ohne Stufe",
                )
        val f = fehler(AkteEntwurf.zuProfil("A", e))
        assertTrue(f.containsKey("groesse.min"))
        assertTrue(f.containsKey("gewicht.max"))
        assertTrue(f.containsKey("puls.min"))
        assertTrue(f.containsKey("blutdruck.dia"))
        assertTrue(f.containsKey("verfassung.stufe"))
    }

    @Test
    fun `Blutdruck nur als Hinweis ist erlaubt, 0 nicht`() {
        val hinweis = AkteEntwurf.aus(null) + mapOf("blutdruck.hinweis" to "kein Kreislauf")
        assertEquals(Blutdruck(hinweis = "kein Kreislauf"), ok(AkteEntwurf.zuProfil("A", hinweis)).blutdruck!!.wert)
        val null0 = AkteEntwurf.aus(null) + mapOf("blutdruck.sys" to "0", "blutdruck.dia" to "0")
        assertTrue(fehler(AkteEntwurf.zuProfil("A", null0)).containsKey("blutdruck.sys"))
    }

    @Test
    fun `Allergie-Zeilen anfuegen und entfernen ruecken korrekt auf`() {
        var e = AkteEntwurf.aus(profil)
        e = AkteEntwurf.mitNeuerAllergie(e) + (AkteEntwurf.allergieKey(1, "ausloeser") to "Glockenläuten")
        e = AkteEntwurf.mitNeuerAllergie(e) + (AkteEntwurf.allergieKey(2, "ausloeser") to "Eisen")
        assertEquals(3, AkteEntwurf.anzahlAllergien(e))
        e = AkteEntwurf.ohneAllergie(e, 0)
        val liste = ok(AkteEntwurf.zuProfil("A", e)).allergien!!.wert
        assertEquals(listOf("Glockenläuten", "Eisen"), liste.map { it.ausloeser })
        assertEquals(Schwere.DEUTLICH, liste.first().schwere)
        assertEquals("Schwächen", ok(AkteEntwurf.zuProfil("A", e)).allergien!!.begruendung)
    }

    @Test
    fun `hoechstens MAX_ALLERGIEN Zeilen`() {
        var e = AkteEntwurf.aus(null)
        repeat(AkteEntwurf.MAX_ALLERGIEN + 3) { e = AkteEntwurf.mitNeuerAllergie(e) }
        assertEquals(AkteEntwurf.MAX_ALLERGIEN, AkteEntwurf.anzahlAllergien(e))
    }

    @Test
    fun `Reaktion ohne Ausloeser ist ein Fehler, leere Zeile wird ignoriert`() {
        var e = AkteEntwurf.mitNeuerAllergie(AkteEntwurf.aus(null))
        assertEquals(null, ok(AkteEntwurf.zuProfil("A", e)).allergien)
        e = e + (AkteEntwurf.allergieKey(0, "reaktion") to "Ausschlag")
        assertTrue(fehler(AkteEntwurf.zuProfil("A", e)).containsKey(AkteEntwurf.allergieKey(0, "ausloeser")))
    }

    @Test
    fun `wahnhaft ja, nein und unbekannt`() {
        fun w(v: String) = ok(AkteEntwurf.zuProfil("A", AkteEntwurf.aus(null) + ("wahnhaft.wert" to v))).wahnhaft?.wert
        assertEquals(true, w("ja"))
        assertEquals(false, w("nein"))
        assertEquals(null, w(""))
    }

    @Test
    fun `deutsche Anzeige`() {
        assertEquals("1,6–2 m", AkteFormat.spanne(Spanne(1.6, 2.0), "m"))
        assertEquals("≈ 0,25 kg", AkteFormat.spanne(Spanne(0.25, 0.25), "kg"))
        assertEquals("kein Herzschlag", AkteFormat.puls(GanzzahlSpanne(0, 0)))
        assertEquals("45–60 bpm", AkteFormat.puls(GanzzahlSpanne(45, 60)))
        assertEquals("150/95 mmHg · in Ruhe", AkteFormat.blutdruck(Blutdruck(150, 95, "in Ruhe")))
        assertEquals("kein Kreislauf", AkteFormat.blutdruck(Blutdruck(hinweis = "kein Kreislauf")))
        assertEquals(
            "Sonnenlicht (UV) – Unverträglichkeit, lebensbedrohlich",
            AkteFormat.allergie(profil.allergien!!.wert.single()),
        )
        assertEquals("unterdurchschnittlich", AkteFormat.iqBand(GanzzahlSpanne(60, 85)))
        assertEquals("jenseits menschlicher Maßstäbe", AkteFormat.iqBand(GanzzahlSpanne(150, 180)))
        assertEquals("keine bekannt", AkteFormat.kurz(Angabe(emptyList<Allergie>())))
    }

    @Test
    fun `Domaenenwerte pruefen ihre Grenzen`() {
        fun fails(block: () -> Unit) = assertTrue(runCatching(block).exceptionOrNull() is IllegalArgumentException)
        fails { Spanne(2.0, 1.0) }
        fails { Spanne(-1.0, 1.0) }
        fails { GanzzahlSpanne(5, 1) }
        fails { Blutdruck(80, 120) }
        fails { Blutdruck(120, null) }
        fails { Blutdruck() }
        fails { Allergie(" ", AllergieArt.ALLERGIE, Schwere.LEICHT) }
    }

    @Test
    fun `offene Ableitungen zaehlen nur abgeleitete Werte`() {
        assertEquals(8, profil.offeneAbleitungen)
        val teils = profil.mitHerkunft(ProfilFeld.IQ, Herkunft.VON_FYNN_BESTAETIGT).mitHerkunft(ProfilFeld.PULS, Herkunft.MANUELL)
        assertEquals(6, teils.offeneAbleitungen)
        // Fehlende Felder bleiben fehlend.
        assertEquals(null, profil.mitHerkunft(ProfilFeld.EQ, Herkunft.MANUELL).eq)
    }
}
