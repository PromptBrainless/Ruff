package de.ruff.dimension.domain.akte

/*
 * Kreaturen-Akte: einheitlicher Steckbrief je Wesen, bewusst ohne Regelwerksbezug.
 *
 * Jeder Wert ist eine [Angabe] mit kurzer Begründung und Herkunft. Abgeleitete
 * Werte sind Einschätzungen aus Bild und Lore – keine Fakten – und werden in der
 * Oberfläche immer als solche gekennzeichnet.
 */

/** Woher ein Wert stammt. */
internal enum class Herkunft(
    val label: String,
) {
    /** Aus Bild und Lore abgeleitet (Seed); Einschätzung, kein Fakt. */
    ABGELEITET("abgeleitet"),

    /** Abgeleiteter Wert, den Fynn geprüft und bestätigt hat. */
    VON_FYNN_BESTAETIGT("bestätigt"),

    /** Im Editor von Hand gesetzt oder überschrieben. */
    MANUELL("manuell"),
    ;

    /** Der Akten-Seed darf nur Werte ersetzen, die nie bestätigt oder überschrieben wurden. */
    val seedDarfErsetzen: Boolean get() = this == ABGELEITET

    companion object {
        fun parse(value: String?): Herkunft? = value?.let { v -> entries.firstOrNull { it.name == v } }
    }
}

/** Ein Wert samt Begründung und Herkunft. */
internal data class Angabe<out T>(
    val wert: T,
    val begruendung: String = "",
    val herkunft: Herkunft = Herkunft.ABGELEITET,
)

/** Kommazahl-Spanne (z. B. Größe in m), `min <= max`, nie negativ. */
internal data class Spanne(
    val min: Double,
    val max: Double,
) {
    init {
        require(min.isFinite() && max.isFinite()) { "Spanne muss endlich sein" }
        require(min >= 0.0) { "Spanne darf nicht negativ sein" }
        require(min <= max) { "Minimum größer als Maximum" }
    }
}

/** Ganzzahl-Spanne (Puls, IQ, EQ), `min <= max`, nie negativ. */
internal data class GanzzahlSpanne(
    val min: Int,
    val max: Int,
) {
    init {
        require(min >= 0) { "Spanne darf nicht negativ sein" }
        require(min <= max) { "Minimum größer als Maximum" }
    }

    /** Puls 0–0: kein Herzschlag (etwa bei Untoten). */
    val istNull: Boolean get() = max == 0
}

/** Blutdruck in mmHg; ohne Zahlen nur als Hinweis (z. B. „kein Kreislauf“). */
internal data class Blutdruck(
    val systolisch: Int? = null,
    val diastolisch: Int? = null,
    val hinweis: String = "",
) {
    init {
        require((systolisch == null) == (diastolisch == null)) { "Systolisch und diastolisch nur gemeinsam" }
        if (systolisch != null && diastolisch != null) {
            require(systolisch > 0 && diastolisch > 0) { "Blutdruck muss positiv sein" }
            require(systolisch >= diastolisch) { "Systolisch muss mindestens diastolisch sein" }
        }
        require(systolisch != null || hinweis.isNotBlank()) { "Blutdruck braucht Werte oder einen Hinweis" }
    }
}

internal enum class AllergieArt(
    val label: String,
) {
    ALLERGIE("Allergie"),
    UNVERTRAEGLICHKEIT("Unverträglichkeit"),
}

internal enum class Schwere(
    val label: String,
) {
    LEICHT("leicht"),
    DEUTLICH("deutlich"),
    SCHWER("schwer"),
    LEBENSBEDROHLICH("lebensbedrohlich"),
}

/** Ein Auslöser mit Art, Schwere und typischer Reaktion, z. B. „UV-Licht – unverträglich“. */
internal data class Allergie(
    val ausloeser: String,
    val art: AllergieArt,
    val schwere: Schwere,
    val reaktion: String = "",
) {
    init {
        require(ausloeser.isNotBlank()) { "Auslöser fehlt" }
    }
}

internal enum class Verfassungsstufe(
    val label: String,
) {
    GEBRECHLICH("gebrechlich"),
    SCHWACH("schwach"),
    DURCHSCHNITTLICH("durchschnittlich"),
    ROBUST("robust"),
    AUSSERGEWOEHNLICH("außergewöhnlich"),
    UEBERNATUERLICH("übernatürlich"),
}

/** Körperliche Verfassung: Einstufung plus Freitext. */
internal data class Verfassung(
    val stufe: Verfassungsstufe,
    val text: String = "",
)

/** Die Felder der Akte, in Anzeigereihenfolge, mit ihrem Abschnitt. */
internal enum class ProfilFeld(
    val label: String,
    val abschnitt: AkteAbschnitt,
) {
    ERSCHEINUNG("Erscheinung", AkteAbschnitt.STECKBRIEF),
    LEBENSRAUM("Lebensraum", AkteAbschnitt.STECKBRIEF),
    VERHALTEN("Verhalten", AkteAbschnitt.STECKBRIEF),
    GROESSE("Größe", AkteAbschnitt.KOERPER),
    GEWICHT("Gewicht", AkteAbschnitt.KOERPER),
    PULS("Puls", AkteAbschnitt.KOERPER),
    BLUTDRUCK("Blutdruck", AkteAbschnitt.KOERPER),
    VERFASSUNG("Körperliche Verfassung", AkteAbschnitt.VERFASSUNG),
    ALLERGIEN("Allergien & Unverträglichkeiten", AkteAbschnitt.VERFASSUNG),
    IQ("Intelligenz (IQ)", AkteAbschnitt.KOGNITION),
    EQ("Emotionale Intelligenz (EQ)", AkteAbschnitt.KOGNITION),
    WAHNHAFT("Wahnhaft", AkteAbschnitt.KOGNITION),
}

/** Abschnitte der Detailansicht. */
internal enum class AkteAbschnitt(
    val titel: String,
) {
    STECKBRIEF("Steckbrief"),
    KOERPER("Körperliche Werte"),
    VERFASSUNG("Verfassung & Allergien"),
    KOGNITION("Kognition"),
}

/** Steckbrief eines Wesens. Fehlende Werte sind `null`. */
internal data class CreatureProfile(
    val creatureId: String,
    val erscheinung: Angabe<String>? = null,
    val lebensraum: Angabe<String>? = null,
    val verhalten: Angabe<String>? = null,
    val groesseM: Angabe<Spanne>? = null,
    val gewichtKg: Angabe<Spanne>? = null,
    val pulsBpm: Angabe<GanzzahlSpanne>? = null,
    val blutdruck: Angabe<Blutdruck>? = null,
    val allergien: Angabe<List<Allergie>>? = null,
    val verfassung: Angabe<Verfassung>? = null,
    val iq: Angabe<GanzzahlSpanne>? = null,
    val eq: Angabe<GanzzahlSpanne>? = null,
    val wahnhaft: Angabe<Boolean>? = null,
    /** Worauf die Ableitung beruht (Bild, Lore-Felder); nur Information. */
    val quellen: List<String> = emptyList(),
    val revision: Long = 0,
    val updatedAt: Long = 0,
) {
    fun angabe(feld: ProfilFeld): Angabe<*>? =
        when (feld) {
            ProfilFeld.ERSCHEINUNG -> erscheinung
            ProfilFeld.LEBENSRAUM -> lebensraum
            ProfilFeld.VERHALTEN -> verhalten
            ProfilFeld.GROESSE -> groesseM
            ProfilFeld.GEWICHT -> gewichtKg
            ProfilFeld.PULS -> pulsBpm
            ProfilFeld.BLUTDRUCK -> blutdruck
            ProfilFeld.ALLERGIEN -> allergien
            ProfilFeld.VERFASSUNG -> verfassung
            ProfilFeld.IQ -> iq
            ProfilFeld.EQ -> eq
            ProfilFeld.WAHNHAFT -> wahnhaft
        }

    /** Übernimmt das Feld [feld] (Wert, Begründung, Herkunft) aus [quelle]. */
    fun mitFeldAus(
        feld: ProfilFeld,
        quelle: CreatureProfile,
    ): CreatureProfile =
        when (feld) {
            ProfilFeld.ERSCHEINUNG -> copy(erscheinung = quelle.erscheinung)
            ProfilFeld.LEBENSRAUM -> copy(lebensraum = quelle.lebensraum)
            ProfilFeld.VERHALTEN -> copy(verhalten = quelle.verhalten)
            ProfilFeld.GROESSE -> copy(groesseM = quelle.groesseM)
            ProfilFeld.GEWICHT -> copy(gewichtKg = quelle.gewichtKg)
            ProfilFeld.PULS -> copy(pulsBpm = quelle.pulsBpm)
            ProfilFeld.BLUTDRUCK -> copy(blutdruck = quelle.blutdruck)
            ProfilFeld.ALLERGIEN -> copy(allergien = quelle.allergien)
            ProfilFeld.VERFASSUNG -> copy(verfassung = quelle.verfassung)
            ProfilFeld.IQ -> copy(iq = quelle.iq)
            ProfilFeld.EQ -> copy(eq = quelle.eq)
            ProfilFeld.WAHNHAFT -> copy(wahnhaft = quelle.wahnhaft)
        }

    /** Setzt die Herkunft eines vorhandenen Felds; fehlende Felder bleiben fehlend. */
    fun mitHerkunft(
        feld: ProfilFeld,
        herkunft: Herkunft,
    ): CreatureProfile {
        val angepasst =
            CreatureProfile(
                creatureId = creatureId,
                erscheinung = erscheinung?.copy(herkunft = herkunft),
                lebensraum = lebensraum?.copy(herkunft = herkunft),
                verhalten = verhalten?.copy(herkunft = herkunft),
                groesseM = groesseM?.copy(herkunft = herkunft),
                gewichtKg = gewichtKg?.copy(herkunft = herkunft),
                pulsBpm = pulsBpm?.copy(herkunft = herkunft),
                blutdruck = blutdruck?.copy(herkunft = herkunft),
                allergien = allergien?.copy(herkunft = herkunft),
                verfassung = verfassung?.copy(herkunft = herkunft),
                iq = iq?.copy(herkunft = herkunft),
                eq = eq?.copy(herkunft = herkunft),
                wahnhaft = wahnhaft?.copy(herkunft = herkunft),
            )
        return mitFeldAus(feld, angepasst)
    }

    /** Felder mit Wert, in Anzeigereihenfolge. */
    val vorhandeneFelder: List<ProfilFeld> get() = ProfilFeld.entries.filter { angabe(it) != null }

    val istLeer: Boolean get() = vorhandeneFelder.isEmpty()

    /** Anzahl Werte, die noch nur abgeleitet (nicht bestätigt/überschrieben) sind. */
    val offeneAbleitungen: Int get() = vorhandeneFelder.count { angabe(it)?.herkunft == Herkunft.ABGELEITET }
}
