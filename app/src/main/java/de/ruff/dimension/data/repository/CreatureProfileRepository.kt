package de.ruff.dimension.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import de.ruff.dimension.data.AkteJson
import de.ruff.dimension.data.local.CreatureProfileEntity
import de.ruff.dimension.data.local.CreatureProfileMapping
import de.ruff.dimension.data.local.RuffDatabase
import de.ruff.dimension.domain.akte.Angabe
import de.ruff.dimension.domain.akte.CreatureProfile
import de.ruff.dimension.domain.akte.Herkunft
import de.ruff.dimension.domain.akte.ProfilFeld
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.StringReader
import java.util.UUID

/** Kreaturen-Akten: lesen, bearbeiten, bestätigen, sichern. Unabhängig vom Inhalts-Seed. */
internal interface CreatureProfileRepository {
    /** Aktive Akten je Wesen-ID. */
    fun observeProfiles(): Flow<Map<String, CreatureProfile>>

    suspend fun get(creatureId: String): CreatureProfile?

    /**
     * Speichert einen Editor-Stand. Felder, deren Wert oder Begründung sich gegenüber dem
     * gespeicherten Stand ändern, bekommen [Herkunft.MANUELL]; unveränderte behalten ihre
     * Herkunft. Ohne Änderung wird nichts geschrieben. Liefert den gespeicherten Stand.
     */
    suspend fun save(edited: CreatureProfile): CreatureProfile

    /** Markiert alle noch abgeleiteten Werte als [Herkunft.VON_FYNN_BESTAETIGT]; liefert die Anzahl. */
    suspend fun confirmDerived(creatureId: String): Int

    /** Sicherung aller aktiven Akten im [AkteJson]-Format (mit Herkunft). */
    suspend fun exportJson(): String

    /**
     * Spielt eine Sicherung ein: Jede enthaltene Akte ersetzt die gespeicherte (Herkunft
     * wie in der Datei). Ungültige Dateien ändern nichts. Liefert die Anzahl Akten.
     */
    suspend fun importJson(json: String): Int
}

internal class RoomCreatureProfileRepository(
    private val database: RuffDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : CreatureProfileRepository {
    private val dao get() = database.creatureProfileDao()

    override fun observeProfiles(): Flow<Map<String, CreatureProfile>> =
        dao
            .observeActive()
            .map { list -> list.associate { it.creatureId to CreatureProfileMapping.toDomain(it) } }
            .distinctUntilChanged()

    override suspend fun get(creatureId: String): CreatureProfile? =
        dao.getByCreatureId(creatureId)?.takeIf { it.active }?.let(CreatureProfileMapping::toDomain)

    override suspend fun save(edited: CreatureProfile): CreatureProfile {
        require(edited.creatureId.isNotBlank()) { "creatureId fehlt" }
        return transaction {
            val existing = dao.getByCreatureId(edited.creatureId)
            val current = existing?.takeIf { it.active }?.let(CreatureProfileMapping::toDomain)
            val stored = current ?: CreatureProfile(edited.creatureId)
            var merged = stored.copy(quellen = current?.quellen ?: edited.quellen)
            val geleert = LinkedHashMap<ProfilFeld, Herkunft>()
            ProfilFeld.entries.forEach { feld ->
                val alt = stored.angabe(feld)
                val neu = edited.angabe(feld)
                val gleich = alt?.ohneHerkunft() == neu?.ohneHerkunft()
                when {
                    gleich && neu == null -> {
                        // Bewusst geleerte Felder bleiben geleert.
                        existing?.takeIf { it.active }?.let { CreatureProfileMapping.herkunft(it, feld) }?.let { geleert[feld] = it }
                    }
                    gleich -> Unit
                    neu == null -> {
                        merged = merged.mitFeldAus(feld, CreatureProfile(edited.creatureId))
                        geleert[feld] = Herkunft.MANUELL
                    }
                    else -> merged = merged.mitFeldAus(feld, edited).mitHerkunft(feld, Herkunft.MANUELL)
                }
            }
            write(existing, merged, geleert)
        }
    }

    override suspend fun confirmDerived(creatureId: String): Int =
        transaction {
            val existing = dao.getByCreatureId(creatureId)?.takeIf { it.active } ?: return@transaction 0
            val current = CreatureProfileMapping.toDomain(existing)
            val offen = current.vorhandeneFelder.filter { current.angabe(it)?.herkunft == Herkunft.ABGELEITET }
            if (offen.isEmpty()) return@transaction 0
            val confirmed = offen.fold(current) { p, feld -> p.mitHerkunft(feld, Herkunft.VON_FYNN_BESTAETIGT) }
            write(existing, confirmed, geleertAus(existing, current))
            offen.size
        }

    override suspend fun exportJson(): String {
        val names = database.creatureDao().getAll().associate { it.id to it.name }
        val profiles = dao.getAll().filter { it.active }.map(CreatureProfileMapping::toDomain)
        return AkteJson.write(profiles, names, hinweis = "Ruff-Sicherung der Kreaturen-Akten")
    }

    override suspend fun importJson(json: String): Int {
        val datei = AkteJson.read(StringReader(json))
        return transaction {
            datei.profile.forEach { p -> write(dao.getByCreatureId(p.creatureId), p, emptyMap()) }
            datei.profile.size
        }
    }

    private fun geleertAus(
        e: CreatureProfileEntity,
        p: CreatureProfile,
    ): Map<ProfilFeld, Herkunft> =
        ProfilFeld.entries
            .filter { p.angabe(it) == null }
            .mapNotNull { feld -> CreatureProfileMapping.herkunft(e, feld)?.let { feld to it } }
            .toMap()

    /** Schreibt [profile]; legt an, belebt verworfene Akten oder aktualisiert. Ohne Änderung kein Schreiben. */
    private suspend fun write(
        existing: CreatureProfileEntity?,
        profile: CreatureProfile,
        geleert: Map<ProfilFeld, Herkunft>,
    ): CreatureProfile {
        val now = clock()
        if (existing == null) {
            val entity =
                CreatureProfileMapping.applyValues(
                    CreatureProfileEntity(
                        id = newId(),
                        creatureId = profile.creatureId,
                        createdAt = now,
                        updatedAt = now,
                        revision = 1,
                    ),
                    profile,
                    geleert,
                )
            dao.insert(entity)
            return CreatureProfileMapping.toDomain(entity)
        }
        val candidate = CreatureProfileMapping.applyValues(existing, profile, geleert).copy(deletedAt = null)
        if (candidate == existing) return CreatureProfileMapping.toDomain(existing)
        val updated = candidate.copy(updatedAt = now, revision = existing.revision + 1)
        dao.update(updated)
        return CreatureProfileMapping.toDomain(updated)
    }

    private suspend fun <T> transaction(block: suspend () -> T): T =
        database.useWriterConnection { connection -> connection.immediateTransaction { block() } }
}

private fun Angabe<*>.ohneHerkunft(): Pair<Any?, String> = wert to begruendung.trim()
