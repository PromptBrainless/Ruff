package de.ruff.dimension.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.ruff.dimension.domain.akte.Allergie
import de.ruff.dimension.domain.akte.AllergieArt
import de.ruff.dimension.domain.akte.Angabe
import de.ruff.dimension.domain.akte.Blutdruck
import de.ruff.dimension.domain.akte.CreatureProfile
import de.ruff.dimension.domain.akte.GanzzahlSpanne
import de.ruff.dimension.domain.akte.Herkunft
import de.ruff.dimension.domain.akte.ProfilFeld
import de.ruff.dimension.domain.akte.Schwere
import de.ruff.dimension.domain.akte.Spanne
import de.ruff.dimension.domain.akte.Verfassung
import de.ruff.dimension.domain.akte.Verfassungsstufe

/** Wandelt zwischen [CreatureProfileEntity] (Spalten) und [CreatureProfile] (Domäne). */
internal object CreatureProfileMapping {
    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type
    private val allergyListType = object : TypeToken<List<AllergieZeile>>() {}.type

    /** JSON-Form einer Allergie in der Spalte `allergien` (Namen sind Teil des Such-Triggers). */
    private data class AllergieZeile(
        val ausloeser: String = "",
        val art: String = "",
        val schwere: String = "",
        val reaktion: String = "",
    )

    fun quellenToJson(quellen: List<String>): String = gson.toJson(quellen, stringListType)

    fun quellenFromJson(json: String?): List<String> =
        json?.let { runCatching { gson.fromJson<List<String>>(it, stringListType) }.getOrNull() }.orEmpty()

    fun allergienToJson(list: List<Allergie>): String =
        gson.toJson(list.map { AllergieZeile(it.ausloeser, it.art.name, it.schwere.name, it.reaktion) }, allergyListType)

    fun allergienFromJson(json: String?): List<Allergie> =
        json
            ?.let { runCatching { gson.fromJson<List<AllergieZeile>>(it, allergyListType) }.getOrNull() }
            .orEmpty()
            .mapNotNull { z ->
                val art = AllergieArt.entries.firstOrNull { it.name == z.art } ?: return@mapNotNull null
                val schwere = Schwere.entries.firstOrNull { it.name == z.schwere } ?: return@mapNotNull null
                runCatching { Allergie(z.ausloeser, art, schwere, z.reaktion) }.getOrNull()
            }

    private fun <T> angabe(
        herkunft: String?,
        begruendung: String?,
        wert: () -> T?,
    ): Angabe<T>? {
        val h = Herkunft.parse(herkunft) ?: return null
        val w = runCatching(wert).getOrNull() ?: return null
        return Angabe(w, begruendung.orEmpty(), h)
    }

    fun toDomain(e: CreatureProfileEntity): CreatureProfile =
        CreatureProfile(
            creatureId = e.creatureId,
            erscheinung = angabe(e.erscheinungHerkunft, e.erscheinungBegruendung) { e.erscheinung?.takeIf { it.isNotBlank() } },
            lebensraum = angabe(e.lebensraumHerkunft, e.lebensraumBegruendung) { e.lebensraum?.takeIf { it.isNotBlank() } },
            verhalten = angabe(e.verhaltenHerkunft, e.verhaltenBegruendung) { e.verhalten?.takeIf { it.isNotBlank() } },
            groesseM =
                angabe(e.groesseHerkunft, e.groesseBegruendung) {
                    if (e.groesseMinM != null && e.groesseMaxM != null) Spanne(e.groesseMinM, e.groesseMaxM) else null
                },
            gewichtKg =
                angabe(e.gewichtHerkunft, e.gewichtBegruendung) {
                    if (e.gewichtMinKg != null && e.gewichtMaxKg != null) Spanne(e.gewichtMinKg, e.gewichtMaxKg) else null
                },
            pulsBpm =
                angabe(e.pulsHerkunft, e.pulsBegruendung) {
                    if (e.pulsMinBpm != null && e.pulsMaxBpm != null) GanzzahlSpanne(e.pulsMinBpm, e.pulsMaxBpm) else null
                },
            blutdruck =
                angabe(e.blutdruckHerkunft, e.blutdruckBegruendung) {
                    if (e.blutdruckSystolisch == null && e.blutdruckHinweis.isNullOrBlank()) {
                        null
                    } else {
                        Blutdruck(e.blutdruckSystolisch, e.blutdruckDiastolisch, e.blutdruckHinweis.orEmpty())
                    }
                },
            allergien = angabe(e.allergienHerkunft, e.allergienBegruendung) { e.allergien?.let { allergienFromJson(it) } },
            verfassung =
                angabe(e.verfassungHerkunft, e.verfassungBegruendung) {
                    Verfassungsstufe.entries.firstOrNull { it.name == e.verfassungStufe }?.let {
                        Verfassung(
                            it,
                            e.verfassungText.orEmpty(),
                        )
                    }
                },
            iq =
                angabe(e.iqHerkunft, e.iqBegruendung) {
                    if (e.iqMin != null &&
                        e.iqMax != null
                    ) {
                        GanzzahlSpanne(e.iqMin, e.iqMax)
                    } else {
                        null
                    }
                },
            eq =
                angabe(e.eqHerkunft, e.eqBegruendung) {
                    if (e.eqMin != null &&
                        e.eqMax != null
                    ) {
                        GanzzahlSpanne(e.eqMin, e.eqMax)
                    } else {
                        null
                    }
                },
            wahnhaft = angabe(e.wahnhaftHerkunft, e.wahnhaftBegruendung) { e.wahnhaft },
            quellen = quellenFromJson(e.quellen),
            revision = e.revision,
            updatedAt = e.updatedAt,
        )

    /** Herkunft eines Felds direkt aus den Spalten (auch für bewusst geleerte Werte). */
    fun herkunft(
        e: CreatureProfileEntity,
        feld: ProfilFeld,
    ): Herkunft? =
        Herkunft.parse(
            when (feld) {
                ProfilFeld.ERSCHEINUNG -> e.erscheinungHerkunft
                ProfilFeld.LEBENSRAUM -> e.lebensraumHerkunft
                ProfilFeld.VERHALTEN -> e.verhaltenHerkunft
                ProfilFeld.GROESSE -> e.groesseHerkunft
                ProfilFeld.GEWICHT -> e.gewichtHerkunft
                ProfilFeld.PULS -> e.pulsHerkunft
                ProfilFeld.BLUTDRUCK -> e.blutdruckHerkunft
                ProfilFeld.ALLERGIEN -> e.allergienHerkunft
                ProfilFeld.VERFASSUNG -> e.verfassungHerkunft
                ProfilFeld.IQ -> e.iqHerkunft
                ProfilFeld.EQ -> e.eqHerkunft
                ProfilFeld.WAHNHAFT -> e.wahnhaftHerkunft
            },
        )

    /**
     * Schreibt die Werte aus [p] in [base] (Kopf- und Metaspalten bleiben).
     * [geleert]: Felder ohne Wert, deren Herkunft trotzdem gespeichert wird
     * (bewusst geleert, damit der Seed sie nicht wieder füllt).
     */
    fun applyValues(
        base: CreatureProfileEntity,
        p: CreatureProfile,
        geleert: Map<ProfilFeld, Herkunft> = emptyMap(),
    ): CreatureProfileEntity {
        fun h(
            feld: ProfilFeld,
            angabe: Angabe<*>?,
        ): String? = angabe?.herkunft?.name ?: geleert[feld]?.name
        return base.copy(
            quellen = quellenToJson(p.quellen),
            erscheinung = p.erscheinung?.wert,
            erscheinungBegruendung = p.erscheinung?.begruendung,
            erscheinungHerkunft = h(ProfilFeld.ERSCHEINUNG, p.erscheinung),
            lebensraum = p.lebensraum?.wert,
            lebensraumBegruendung = p.lebensraum?.begruendung,
            lebensraumHerkunft = h(ProfilFeld.LEBENSRAUM, p.lebensraum),
            verhalten = p.verhalten?.wert,
            verhaltenBegruendung = p.verhalten?.begruendung,
            verhaltenHerkunft = h(ProfilFeld.VERHALTEN, p.verhalten),
            groesseMinM = p.groesseM?.wert?.min,
            groesseMaxM = p.groesseM?.wert?.max,
            groesseBegruendung = p.groesseM?.begruendung,
            groesseHerkunft = h(ProfilFeld.GROESSE, p.groesseM),
            gewichtMinKg = p.gewichtKg?.wert?.min,
            gewichtMaxKg = p.gewichtKg?.wert?.max,
            gewichtBegruendung = p.gewichtKg?.begruendung,
            gewichtHerkunft = h(ProfilFeld.GEWICHT, p.gewichtKg),
            pulsMinBpm = p.pulsBpm?.wert?.min,
            pulsMaxBpm = p.pulsBpm?.wert?.max,
            pulsBegruendung = p.pulsBpm?.begruendung,
            pulsHerkunft = h(ProfilFeld.PULS, p.pulsBpm),
            blutdruckSystolisch = p.blutdruck?.wert?.systolisch,
            blutdruckDiastolisch = p.blutdruck?.wert?.diastolisch,
            blutdruckHinweis = p.blutdruck?.wert?.hinweis,
            blutdruckBegruendung = p.blutdruck?.begruendung,
            blutdruckHerkunft = h(ProfilFeld.BLUTDRUCK, p.blutdruck),
            allergien = p.allergien?.wert?.let { allergienToJson(it) },
            allergienBegruendung = p.allergien?.begruendung,
            allergienHerkunft = h(ProfilFeld.ALLERGIEN, p.allergien),
            verfassungStufe =
                p.verfassung
                    ?.wert
                    ?.stufe
                    ?.name,
            verfassungText = p.verfassung?.wert?.text,
            verfassungBegruendung = p.verfassung?.begruendung,
            verfassungHerkunft = h(ProfilFeld.VERFASSUNG, p.verfassung),
            iqMin = p.iq?.wert?.min,
            iqMax = p.iq?.wert?.max,
            iqBegruendung = p.iq?.begruendung,
            iqHerkunft = h(ProfilFeld.IQ, p.iq),
            eqMin = p.eq?.wert?.min,
            eqMax = p.eq?.wert?.max,
            eqBegruendung = p.eq?.begruendung,
            eqHerkunft = h(ProfilFeld.EQ, p.eq),
            wahnhaft = p.wahnhaft?.wert,
            wahnhaftBegruendung = p.wahnhaft?.begruendung,
            wahnhaftHerkunft = h(ProfilFeld.WAHNHAFT, p.wahnhaft),
        )
    }
}
