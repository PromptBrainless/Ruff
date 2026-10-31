package de.ruff.dimension.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import de.ruff.dimension.domain.akte.Allergie
import de.ruff.dimension.domain.akte.AllergieArt
import de.ruff.dimension.domain.akte.Angabe
import de.ruff.dimension.domain.akte.Blutdruck
import de.ruff.dimension.domain.akte.CreatureProfile
import de.ruff.dimension.domain.akte.GanzzahlSpanne
import de.ruff.dimension.domain.akte.Herkunft
import de.ruff.dimension.domain.akte.Schwere
import de.ruff.dimension.domain.akte.Spanne
import de.ruff.dimension.domain.akte.Verfassung
import de.ruff.dimension.domain.akte.Verfassungsstufe
import java.io.Reader

/**
 * Dateiformat der Kreaturen-Akten – gleich für den Seed (`assets/creature_profiles.json`)
 * und für Sicherungen (Export/Import).
 *
 * ```json
 * { "format": "ruff.kreaturen-akte", "formatVersion": 1, "seedVersion": 1,
 *   "profile": [ { "creatureId": "V2-090", "name": "Troll", "quellen": ["…"],
 *     "erscheinung": {"wert": "…", "begruendung": "…"},
 *     "groesseM": {"min": 1.7, "max": 2.1, "begruendung": "…"},
 *     "pulsBpm": {"min": 45, "max": 60, "begruendung": "…"},
 *     "blutdruck": {"systolisch": 165, "diastolisch": 100, "hinweis": "", "begruendung": "…"},
 *     "allergien": {"liste": [{"ausloeser": "Sonnenlicht", "art": "UNVERTRAEGLICHKEIT",
 *                   "schwere": "LEBENSBEDROHLICH", "reaktion": "…"}], "begruendung": "…"},
 *     "verfassung": {"stufe": "ROBUST", "text": "…", "begruendung": "…"},
 *     "iq": {"min": 60, "max": 80, "begruendung": "…"}, "eq": {…},
 *     "wahnhaft": {"wert": false, "begruendung": "…"} } ] }
 * ```
 *
 * `herkunft` ist optional: Im Seed fehlt sie (der Seed setzt immer `ABGELEITET`),
 * in Sicherungen steht sie bei jedem Wert. Ungültige Daten führen zu einer
 * [AkteFormatException] mit Angabe der Akte – nie zu halb importierten Werten.
 */
internal object AkteJson {
    const val FORMAT = "ruff.kreaturen-akte"
    const val FORMAT_VERSION = 1
    const val SEED_FILE = "creature_profiles.json"

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    /** Gelesene Datei. [seedVersion] ist nur im Seed belegt. */
    data class Datei(
        val formatVersion: Int,
        val seedVersion: Int,
        val profile: List<CreatureProfile>,
    )

    class AkteFormatException(
        message: String,
        cause: Throwable? = null,
    ) : IllegalArgumentException(message, cause)

    // --- JSON-Struktur (Gson; alle Felder mit Standardwerten → No-Arg-Konstruktor) ---

    private data class DateiJson(
        val format: String = "",
        val formatVersion: Int = 0,
        val seedVersion: Int = 0,
        val hinweis: String? = null,
        val profile: List<ProfilJson> = emptyList(),
    )

    private data class ProfilJson(
        val creatureId: String = "",
        val name: String? = null,
        val quellen: List<String>? = null,
        val erscheinung: TextJson? = null,
        val lebensraum: TextJson? = null,
        val verhalten: TextJson? = null,
        val groesseM: SpanneJson? = null,
        val gewichtKg: SpanneJson? = null,
        val pulsBpm: GanzJson? = null,
        val blutdruck: BlutdruckJson? = null,
        val allergien: AllergienJson? = null,
        val verfassung: VerfassungJson? = null,
        val iq: GanzJson? = null,
        val eq: GanzJson? = null,
        val wahnhaft: WahnJson? = null,
    )

    private data class TextJson(
        val wert: String? = null,
        val begruendung: String? = null,
        val herkunft: String? = null,
    )

    private data class SpanneJson(
        val min: Double? = null,
        val max: Double? = null,
        val begruendung: String? = null,
        val herkunft: String? = null,
    )

    private data class GanzJson(
        val min: Int? = null,
        val max: Int? = null,
        val begruendung: String? = null,
        val herkunft: String? = null,
    )

    private data class BlutdruckJson(
        val systolisch: Int? = null,
        val diastolisch: Int? = null,
        val hinweis: String? = null,
        val begruendung: String? = null,
        val herkunft: String? = null,
    )

    private data class AllergieJson(
        val ausloeser: String? = null,
        val art: String? = null,
        val schwere: String? = null,
        val reaktion: String? = null,
    )

    private data class AllergienJson(
        val liste: List<AllergieJson>? = null,
        val begruendung: String? = null,
        val herkunft: String? = null,
    )

    private data class VerfassungJson(
        val stufe: String? = null,
        val text: String? = null,
        val begruendung: String? = null,
        val herkunft: String? = null,
    )

    private data class WahnJson(
        val wert: Boolean? = null,
        val begruendung: String? = null,
        val herkunft: String? = null,
    )

    // --- Lesen ---

    /** Liest eine Akten-Datei; [herkunftErzwingen] überschreibt jede Herkunft (Seed: ABGELEITET). */
    fun read(
        reader: Reader,
        herkunftErzwingen: Herkunft? = null,
    ): Datei {
        val datei =
            try {
                gson.fromJson(reader, DateiJson::class.java)
            } catch (e: JsonParseException) {
                throw AkteFormatException("Akten-Datei ist kein gültiges JSON", e)
            } ?: throw AkteFormatException("Akten-Datei ist leer")
        if (datei.format != FORMAT) throw AkteFormatException("Unbekanntes Format „${datei.format}“")
        if (datei.formatVersion !in 1..FORMAT_VERSION) {
            throw AkteFormatException("Formatversion ${datei.formatVersion} wird nicht unterstützt")
        }
        val seen = HashSet<String>()
        val profile =
            datei.profile.map { p ->
                val id = p.creatureId.trim()
                if (id.isEmpty()) throw AkteFormatException("Akte ohne creatureId")
                if (!seen.add(id)) throw AkteFormatException("Akte $id doppelt")
                try {
                    p.toDomain(id, herkunftErzwingen)
                } catch (e: AkteFormatException) {
                    throw AkteFormatException("Akte $id: ${e.message}", e)
                } catch (e: IllegalArgumentException) {
                    throw AkteFormatException("Akte $id: ${e.message}", e)
                }
            }
        return Datei(datei.formatVersion, datei.seedVersion, profile)
    }

    private fun herkunft(
        raw: String?,
        erzwingen: Herkunft?,
    ): Herkunft {
        if (erzwingen != null) return erzwingen
        if (raw == null) return Herkunft.ABGELEITET
        return Herkunft.parse(raw) ?: throw AkteFormatException("Unbekannte Herkunft „$raw“")
    }

    private fun <E : Enum<E>> enumOf(
        entries: List<E>,
        raw: String?,
        was: String,
    ): E = entries.firstOrNull { it.name == raw } ?: throw AkteFormatException("Unbekannte $was „$raw“")

    private fun ProfilJson.toDomain(
        id: String,
        h: Herkunft?,
    ): CreatureProfile {
        fun text(t: TextJson?) =
            t
                ?.wert
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { Angabe(it, t.begruendung.orEmpty(), herkunft(t.herkunft, h)) }

        fun spanne(s: SpanneJson?): Angabe<Spanne>? {
            if (s == null) return null
            val min = s.min ?: s.max ?: throw AkteFormatException("Spanne ohne Zahl")
            return Angabe(Spanne(min, s.max ?: min), s.begruendung.orEmpty(), herkunft(s.herkunft, h))
        }

        fun ganz(s: GanzJson?): Angabe<GanzzahlSpanne>? {
            if (s == null) return null
            val min = s.min ?: s.max ?: throw AkteFormatException("Spanne ohne Zahl")
            return Angabe(GanzzahlSpanne(min, s.max ?: min), s.begruendung.orEmpty(), herkunft(s.herkunft, h))
        }
        return CreatureProfile(
            creatureId = id,
            erscheinung = text(erscheinung),
            lebensraum = text(lebensraum),
            verhalten = text(verhalten),
            groesseM = spanne(groesseM),
            gewichtKg = spanne(gewichtKg),
            pulsBpm = ganz(pulsBpm),
            blutdruck =
                blutdruck?.let {
                    Angabe(
                        Blutdruck(it.systolisch, it.diastolisch, it.hinweis.orEmpty()),
                        it.begruendung.orEmpty(),
                        herkunft(it.herkunft, h),
                    )
                },
            allergien =
                allergien?.let { a ->
                    Angabe(
                        a.liste.orEmpty().map {
                            Allergie(
                                ausloeser = it.ausloeser.orEmpty().trim(),
                                art = enumOf(AllergieArt.entries, it.art, "Allergie-Art"),
                                schwere = enumOf(Schwere.entries, it.schwere, "Schwere"),
                                reaktion = it.reaktion.orEmpty(),
                            )
                        },
                        a.begruendung.orEmpty(),
                        herkunft(a.herkunft, h),
                    )
                },
            verfassung =
                verfassung?.let {
                    Angabe(
                        Verfassung(enumOf(Verfassungsstufe.entries, it.stufe, "Verfassungsstufe"), it.text.orEmpty()),
                        it.begruendung.orEmpty(),
                        herkunft(it.herkunft, h),
                    )
                },
            iq = ganz(iq),
            eq = ganz(eq),
            wahnhaft =
                wahnhaft?.let {
                    Angabe(it.wert ?: throw AkteFormatException("wahnhaft ohne Wert"), it.begruendung.orEmpty(), herkunft(it.herkunft, h))
                },
            quellen = quellen.orEmpty(),
        )
    }

    // --- Schreiben (Sicherung) ---

    /** Schreibt Akten samt Herkunft (Sicherungs-Export). [namen]: Wesen-Namen zur Lesbarkeit. */
    fun write(
        profile: List<CreatureProfile>,
        namen: Map<String, String> = emptyMap(),
        hinweis: String? = null,
    ): String =
        gson.toJson(
            DateiJson(
                format = FORMAT,
                formatVersion = FORMAT_VERSION,
                hinweis = hinweis,
                profile = profile.sortedBy { it.creatureId }.map { it.toJson(namen[it.creatureId]) },
            ),
        )

    private fun CreatureProfile.toJson(name: String?) =
        ProfilJson(
            creatureId = creatureId,
            name = name,
            quellen = quellen,
            erscheinung = erscheinung?.let { TextJson(it.wert, it.begruendung, it.herkunft.name) },
            lebensraum = lebensraum?.let { TextJson(it.wert, it.begruendung, it.herkunft.name) },
            verhalten = verhalten?.let { TextJson(it.wert, it.begruendung, it.herkunft.name) },
            groesseM = groesseM?.let { SpanneJson(it.wert.min, it.wert.max, it.begruendung, it.herkunft.name) },
            gewichtKg = gewichtKg?.let { SpanneJson(it.wert.min, it.wert.max, it.begruendung, it.herkunft.name) },
            pulsBpm = pulsBpm?.let { GanzJson(it.wert.min, it.wert.max, it.begruendung, it.herkunft.name) },
            blutdruck =
                blutdruck?.let {
                    BlutdruckJson(it.wert.systolisch, it.wert.diastolisch, it.wert.hinweis, it.begruendung, it.herkunft.name)
                },
            allergien =
                allergien?.let { a ->
                    AllergienJson(
                        a.wert.map { AllergieJson(it.ausloeser, it.art.name, it.schwere.name, it.reaktion) },
                        a.begruendung,
                        a.herkunft.name,
                    )
                },
            verfassung = verfassung?.let { VerfassungJson(it.wert.stufe.name, it.wert.text, it.begruendung, it.herkunft.name) },
            iq = iq?.let { GanzJson(it.wert.min, it.wert.max, it.begruendung, it.herkunft.name) },
            eq = eq?.let { GanzJson(it.wert.min, it.wert.max, it.begruendung, it.herkunft.name) },
            wahnhaft = wahnhaft?.let { WahnJson(it.wert, it.begruendung, it.herkunft.name) },
        )
}
