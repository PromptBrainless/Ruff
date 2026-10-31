package de.ruff.dimension.domain.akte

/**
 * Eingabestand des Akten-Editors als flache Schlüssel/Wert-Liste.
 *
 * Flach, damit er per `rememberSaveable` Drehen und Prozessende übersteht. Alle
 * Felder sind Texte; erst [AkteEntwurf.zuProfil] prüft und wandelt sie um. Geänderte
 * Felder bekommen dabei die Herkunft [Herkunft.MANUELL]; welche Felder tatsächlich
 * geändert wurden, entscheidet beim Speichern das Repository (Vergleich mit dem Stand).
 */
internal object AkteEntwurf {
    const val BEGRUENDUNG = "begruendung"
    const val ALLERGIEN_ANZAHL = "allergien.anzahl"

    /** Höchstzahl Allergie-Zeilen im Editor. */
    const val MAX_ALLERGIEN = 12

    fun key(
        feld: ProfilFeld,
        teil: String,
    ): String = "${feld.name.lowercase()}.$teil"

    fun begruendung(feld: ProfilFeld): String = key(feld, BEGRUENDUNG)

    fun allergieKey(
        index: Int,
        teil: String,
    ): String = "allergien.$index.$teil"

    /** Ergebnis der Prüfung: entweder ein Profil oder Fehlermeldungen je Schlüssel. */
    sealed interface Ergebnis {
        data class Ok(
            val profile: CreatureProfile,
        ) : Ergebnis

        data class Fehler(
            val fehler: Map<String, String>,
        ) : Ergebnis
    }

    fun aus(profile: CreatureProfile?): Map<String, String> {
        val out = LinkedHashMap<String, String>()

        fun text(
            feld: ProfilFeld,
            angabe: Angabe<String>?,
        ) {
            out[key(feld, "wert")] = angabe?.wert.orEmpty()
            out[begruendung(feld)] = angabe?.begruendung.orEmpty()
        }

        fun spanne(
            feld: ProfilFeld,
            angabe: Angabe<Spanne>?,
        ) {
            out[key(feld, "min")] =
                angabe
                    ?.wert
                    ?.min
                    ?.let(AkteFormat::zahl)
                    .orEmpty()
            out[key(feld, "max")] =
                angabe
                    ?.wert
                    ?.max
                    ?.let(AkteFormat::zahl)
                    .orEmpty()
            out[begruendung(feld)] = angabe?.begruendung.orEmpty()
        }

        fun ganz(
            feld: ProfilFeld,
            angabe: Angabe<GanzzahlSpanne>?,
        ) {
            out[key(feld, "min")] =
                angabe
                    ?.wert
                    ?.min
                    ?.toString()
                    .orEmpty()
            out[key(feld, "max")] =
                angabe
                    ?.wert
                    ?.max
                    ?.toString()
                    .orEmpty()
            out[begruendung(feld)] = angabe?.begruendung.orEmpty()
        }
        text(ProfilFeld.ERSCHEINUNG, profile?.erscheinung)
        text(ProfilFeld.LEBENSRAUM, profile?.lebensraum)
        text(ProfilFeld.VERHALTEN, profile?.verhalten)
        spanne(ProfilFeld.GROESSE, profile?.groesseM)
        spanne(ProfilFeld.GEWICHT, profile?.gewichtKg)
        ganz(ProfilFeld.PULS, profile?.pulsBpm)
        val bd = profile?.blutdruck
        out[key(ProfilFeld.BLUTDRUCK, "sys")] =
            bd
                ?.wert
                ?.systolisch
                ?.toString()
                .orEmpty()
        out[key(ProfilFeld.BLUTDRUCK, "dia")] =
            bd
                ?.wert
                ?.diastolisch
                ?.toString()
                .orEmpty()
        out[key(ProfilFeld.BLUTDRUCK, "hinweis")] = bd?.wert?.hinweis.orEmpty()
        out[begruendung(ProfilFeld.BLUTDRUCK)] = bd?.begruendung.orEmpty()
        val vf = profile?.verfassung
        out[key(ProfilFeld.VERFASSUNG, "stufe")] =
            vf
                ?.wert
                ?.stufe
                ?.name
                .orEmpty()
        out[key(ProfilFeld.VERFASSUNG, "text")] = vf?.wert?.text.orEmpty()
        out[begruendung(ProfilFeld.VERFASSUNG)] = vf?.begruendung.orEmpty()
        val allergien = profile?.allergien?.wert.orEmpty()
        out[ALLERGIEN_ANZAHL] = allergien.size.toString()
        allergien.forEachIndexed { i, a ->
            out[allergieKey(i, "ausloeser")] = a.ausloeser
            out[allergieKey(i, "art")] = a.art.name
            out[allergieKey(i, "schwere")] = a.schwere.name
            out[allergieKey(i, "reaktion")] = a.reaktion
        }
        out[begruendung(ProfilFeld.ALLERGIEN)] = profile?.allergien?.begruendung.orEmpty()
        ganz(ProfilFeld.IQ, profile?.iq)
        ganz(ProfilFeld.EQ, profile?.eq)
        out[key(ProfilFeld.WAHNHAFT, "wert")] =
            when (profile?.wahnhaft?.wert) {
                true -> "ja"
                false -> "nein"
                null -> ""
            }
        out[begruendung(ProfilFeld.WAHNHAFT)] = profile?.wahnhaft?.begruendung.orEmpty()
        return out
    }

    /** Fügt eine leere Allergie-Zeile an (höchstens [MAX_ALLERGIEN]). */
    fun mitNeuerAllergie(entwurf: Map<String, String>): Map<String, String> {
        val n = anzahlAllergien(entwurf)
        if (n >= MAX_ALLERGIEN) return entwurf
        return entwurf +
            mapOf(
                ALLERGIEN_ANZAHL to (n + 1).toString(),
                allergieKey(n, "ausloeser") to "",
                allergieKey(n, "art") to AllergieArt.UNVERTRAEGLICHKEIT.name,
                allergieKey(n, "schwere") to Schwere.DEUTLICH.name,
                allergieKey(n, "reaktion") to "",
            )
    }

    /** Entfernt Zeile [index]; nachfolgende Zeilen rücken auf. */
    fun ohneAllergie(
        entwurf: Map<String, String>,
        index: Int,
    ): Map<String, String> {
        val n = anzahlAllergien(entwurf)
        if (index !in 0 until n) return entwurf
        val teile = listOf("ausloeser", "art", "schwere", "reaktion")
        val out = entwurf.filterKeys { !it.startsWith("allergien.") || it == begruendung(ProfilFeld.ALLERGIEN) }.toMutableMap()
        var ziel = 0
        for (i in 0 until n) {
            if (i == index) continue
            teile.forEach { t -> out[allergieKey(ziel, t)] = entwurf[allergieKey(i, t)].orEmpty() }
            ziel++
        }
        out[ALLERGIEN_ANZAHL] = (n - 1).toString()
        return LinkedHashMap(out)
    }

    fun anzahlAllergien(entwurf: Map<String, String>): Int = (entwurf[ALLERGIEN_ANZAHL]?.toIntOrNull() ?: 0).coerceIn(0, MAX_ALLERGIEN)

    /** Prüft den Entwurf; geänderte Felder sind vorläufig [Herkunft.MANUELL]. */
    fun zuProfil(
        creatureId: String,
        entwurf: Map<String, String>,
        quellen: List<String> = emptyList(),
    ): Ergebnis {
        val fehler = LinkedHashMap<String, String>()
        val m = Herkunft.MANUELL

        fun v(key: String) = entwurf[key].orEmpty().trim()

        fun b(feld: ProfilFeld) = v(begruendung(feld))

        fun text(feld: ProfilFeld): Angabe<String>? = v(key(feld, "wert")).takeIf { it.isNotEmpty() }?.let { Angabe(it, b(feld), m) }

        fun zahl(key: String): Double? {
            val raw = v(key)
            if (raw.isEmpty()) return null
            val parsed = raw.replace(',', '.').toDoubleOrNull()
            if (parsed == null || !parsed.isFinite() || parsed < 0) fehler[key] = "Keine gültige Zahl"
            return parsed
        }

        fun ganz(key: String): Int? {
            val raw = v(key)
            if (raw.isEmpty()) return null
            val parsed = raw.toIntOrNull()
            if (parsed == null || parsed < 0) fehler[key] = "Keine gültige ganze Zahl"
            return parsed
        }

        fun spanne(feld: ProfilFeld): Angabe<Spanne>? {
            val min = zahl(key(feld, "min"))
            val max = zahl(key(feld, "max"))
            if (min == null && max == null) return null
            val lo = min ?: max!!
            val hi = max ?: min!!
            if (lo > hi) {
                fehler[key(feld, "max")] = "Maximum kleiner als Minimum"
                return null
            }
            return runCatching { Angabe(Spanne(lo, hi), b(feld), m) }.getOrNull()
        }

        fun ganzSpanne(feld: ProfilFeld): Angabe<GanzzahlSpanne>? {
            val min = ganz(key(feld, "min"))
            val max = ganz(key(feld, "max"))
            if (min == null && max == null) return null
            val lo = min ?: max!!
            val hi = max ?: min!!
            if (lo > hi) {
                fehler[key(feld, "max")] = "Maximum kleiner als Minimum"
                return null
            }
            return runCatching { Angabe(GanzzahlSpanne(lo, hi), b(feld), m) }.getOrNull()
        }

        val blutdruck: Angabe<Blutdruck>? =
            run {
                val sysKey = key(ProfilFeld.BLUTDRUCK, "sys")
                val diaKey = key(ProfilFeld.BLUTDRUCK, "dia")
                val sys = ganz(sysKey)
                val dia = ganz(diaKey)
                val hinweis = v(key(ProfilFeld.BLUTDRUCK, "hinweis"))
                when {
                    sys == null && dia == null && hinweis.isEmpty() -> null
                    (sys == null) != (dia == null) -> {
                        fehler[if (sys == null) sysKey else diaKey] = "Systolisch und diastolisch zusammen angeben"
                        null
                    }
                    sys != null && dia != null && sys < dia -> {
                        fehler[sysKey] = "Systolisch muss mindestens diastolisch sein"
                        null
                    }
                    sys == 0 || dia == 0 -> {
                        fehler[sysKey] = "Für „kein Kreislauf“ den Hinweis nutzen"
                        null
                    }
                    else -> Angabe(Blutdruck(sys, dia, hinweis), b(ProfilFeld.BLUTDRUCK), m)
                }
            }

        val verfassung: Angabe<Verfassung>? =
            run {
                val stufeKey = key(ProfilFeld.VERFASSUNG, "stufe")
                val stufe = Verfassungsstufe.entries.firstOrNull { it.name == v(stufeKey) }
                val text = v(key(ProfilFeld.VERFASSUNG, "text"))
                when {
                    stufe == null && text.isEmpty() -> null
                    stufe == null -> {
                        fehler[stufeKey] = "Einstufung wählen"
                        null
                    }
                    else -> Angabe(Verfassung(stufe, text), b(ProfilFeld.VERFASSUNG), m)
                }
            }

        val allergien: Angabe<List<Allergie>>? =
            run {
                val liste =
                    (0 until anzahlAllergien(entwurf)).mapNotNull { i ->
                        val ausloeser = v(allergieKey(i, "ausloeser"))
                        val reaktion = v(allergieKey(i, "reaktion"))
                        if (ausloeser.isEmpty()) {
                            if (reaktion.isNotEmpty()) fehler[allergieKey(i, "ausloeser")] = "Auslöser fehlt"
                            return@mapNotNull null
                        }
                        Allergie(
                            ausloeser = ausloeser,
                            art = AllergieArt.entries.firstOrNull { it.name == v(allergieKey(i, "art")) } ?: AllergieArt.UNVERTRAEGLICHKEIT,
                            schwere = Schwere.entries.firstOrNull { it.name == v(allergieKey(i, "schwere")) } ?: Schwere.DEUTLICH,
                            reaktion = reaktion,
                        )
                    }
                val begr = b(ProfilFeld.ALLERGIEN)
                if (liste.isEmpty() && begr.isEmpty()) null else Angabe(liste, begr, m)
            }

        val wahnhaft: Angabe<Boolean>? =
            when (v(key(ProfilFeld.WAHNHAFT, "wert"))) {
                "ja" -> Angabe(true, b(ProfilFeld.WAHNHAFT), m)
                "nein" -> Angabe(false, b(ProfilFeld.WAHNHAFT), m)
                else -> null
            }

        val profile =
            CreatureProfile(
                creatureId = creatureId,
                erscheinung = text(ProfilFeld.ERSCHEINUNG),
                lebensraum = text(ProfilFeld.LEBENSRAUM),
                verhalten = text(ProfilFeld.VERHALTEN),
                groesseM = spanne(ProfilFeld.GROESSE),
                gewichtKg = spanne(ProfilFeld.GEWICHT),
                pulsBpm = ganzSpanne(ProfilFeld.PULS),
                blutdruck = blutdruck,
                allergien = allergien,
                verfassung = verfassung,
                iq = ganzSpanne(ProfilFeld.IQ),
                eq = ganzSpanne(ProfilFeld.EQ),
                wahnhaft = wahnhaft,
                quellen = quellen,
            )
        return if (fehler.isEmpty()) Ergebnis.Ok(profile) else Ergebnis.Fehler(fehler)
    }
}
