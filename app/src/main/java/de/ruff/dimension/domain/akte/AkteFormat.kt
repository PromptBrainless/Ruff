package de.ruff.dimension.domain.akte

import java.math.BigDecimal
import java.math.RoundingMode

/** Deutsche Anzeige der Akten-Werte (Dezimalkomma, Halbgeviertstrich für Spannen). */
internal object AkteFormat {
    private const val STRICH = "–"

    fun zahl(value: Double): String {
        val rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
        val plain = if (rounded.scale() < 0) rounded.setScale(0).toPlainString() else rounded.toPlainString()
        return plain.replace('.', ',')
    }

    fun spanne(
        spanne: Spanne,
        einheit: String,
    ): String =
        if (spanne.min == spanne.max) {
            "≈ ${zahl(spanne.min)} $einheit"
        } else {
            "${zahl(spanne.min)}$STRICH${zahl(spanne.max)} $einheit"
        }

    fun ganzzahl(
        spanne: GanzzahlSpanne,
        einheit: String = "",
    ): String {
        val text = if (spanne.min == spanne.max) "≈ ${spanne.min}" else "${spanne.min}$STRICH${spanne.max}"
        return if (einheit.isBlank()) text else "$text $einheit"
    }

    fun puls(spanne: GanzzahlSpanne): String = if (spanne.istNull) "kein Herzschlag" else ganzzahl(spanne, "bpm")

    fun blutdruck(blutdruck: Blutdruck): String {
        val zahlen =
            if (blutdruck.systolisch != null && blutdruck.diastolisch != null) {
                "${blutdruck.systolisch}/${blutdruck.diastolisch} mmHg"
            } else {
                null
            }
        return listOfNotNull(zahlen, blutdruck.hinweis.takeIf { it.isNotBlank() }).joinToString(" · ")
    }

    fun allergie(allergie: Allergie): String = "${allergie.ausloeser} – ${allergie.art.label}, ${allergie.schwere.label}"

    fun wahnhaft(wert: Boolean): String = if (wert) "ja" else "nein"

    /** Grobe Einordnung einer IQ-Schätzung (menschlicher Vergleichsmaßstab, Mittel 100). */
    fun iqBand(spanne: GanzzahlSpanne): String {
        val mitte = (spanne.min + spanne.max) / 2
        return when {
            mitte < 50 -> "tierhaft"
            mitte < 85 -> "unterdurchschnittlich"
            mitte <= 115 -> "durchschnittlich"
            mitte <= 130 -> "überdurchschnittlich"
            mitte <= 160 -> "hochbegabt"
            else -> "jenseits menschlicher Maßstäbe"
        }
    }

    /** Kurzfassung eines Werts für Listen und Tests. */
    fun kurz(angabe: Angabe<*>): String =
        when (val wert = angabe.wert) {
            is String -> wert
            is Boolean -> wahnhaft(wert)
            is Blutdruck -> blutdruck(wert)
            is Verfassung -> wert.stufe.label
            is List<*> ->
                wert.filterIsInstance<Allergie>().joinToString("; ") { allergie(it) }.ifBlank { "keine bekannt" }
            else -> wert.toString()
        }

    /** Kurzfassung eines Felds mit passender Einheit. */
    fun feld(
        feld: ProfilFeld,
        profile: CreatureProfile,
    ): String? =
        when (feld) {
            ProfilFeld.GROESSE -> profile.groesseM?.let { spanne(it.wert, "m") }
            ProfilFeld.GEWICHT -> profile.gewichtKg?.let { spanne(it.wert, "kg") }
            ProfilFeld.PULS -> profile.pulsBpm?.let { puls(it.wert) }
            ProfilFeld.IQ -> profile.iq?.let { ganzzahl(it.wert) }
            ProfilFeld.EQ -> profile.eq?.let { ganzzahl(it.wert) }
            else -> profile.angabe(feld)?.let { kurz(it) }
        }
}
