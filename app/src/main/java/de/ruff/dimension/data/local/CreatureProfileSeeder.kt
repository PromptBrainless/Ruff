package de.ruff.dimension.data.local

import android.content.res.AssetManager
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import de.ruff.dimension.data.AkteJson
import de.ruff.dimension.domain.akte.CreatureProfile
import de.ruff.dimension.domain.akte.Herkunft
import de.ruff.dimension.domain.akte.ProfilFeld
import java.io.Reader
import java.util.UUID

/** Quelle des Akten-Seeds; in der App `assets/creature_profiles.json`, in Tests eine Datei. */
internal fun interface ProfileSeedSource {
    fun open(): Reader
}

internal class AssetProfileSeedSource(
    private val assets: AssetManager,
) : ProfileSeedSource {
    override fun open(): Reader = assets.open(AkteJson.SEED_FILE).bufferedReader()
}

/** Was ein Akten-Seed-Lauf getan hat. */
internal data class ProfileSeedResult(
    val inserted: Int,
    val updated: Int,
    val unchanged: Int,
    /** Akten, die der Nutzer verworfen hat (Soft-Delete) – der Seed belebt sie nicht. */
    val skippedDeleted: Int,
)

/**
 * Spielt die abgeleiteten Muster-Akten aus [ProfileSeedSource] ein.
 *
 * - Läuft nur, wenn die Seed-Version unter `seed_meta['creature_profiles']` kleiner
 *   als [SEED_VERSION] ist; [importAll] selbst ist idempotent.
 * - Schreibt ausschließlich in `creature_profiles` (und `seed_meta`); die Wesen
 *   (`creatures`) und alle übrigen Tabellen werden nicht berührt.
 * - Werte mit Herkunft [Herkunft.VON_FYNN_BESTAETIGT] oder [Herkunft.MANUELL]
 *   (auch bewusst geleerte) bleiben erhalten; nur fehlende oder rein abgeleitete
 *   Werte werden ersetzt. Alles in einer Transaktion.
 */
internal class CreatureProfileSeeder(
    private val database: RuffDatabase,
    private val source: ProfileSeedSource,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    suspend fun seedIfNeeded(): ProfileSeedResult? {
        val stored = database.seedMetaDao().get(SEED_KEY)?.version ?: 0
        if (stored >= SEED_VERSION) return null
        return importAll()
    }

    suspend fun importAll(): ProfileSeedResult {
        val datei = source.open().use { AkteJson.read(it, herkunftErzwingen = Herkunft.ABGELEITET) }
        check(datei.seedVersion == SEED_VERSION) {
            "creature_profiles.json hat seedVersion ${datei.seedVersion}, erwartet $SEED_VERSION"
        }
        return database.useWriterConnection { connection ->
            connection.immediateTransaction {
                val dao = database.creatureProfileDao()
                var inserted = 0
                var updated = 0
                var unchanged = 0
                var skipped = 0
                val now = clock()
                datei.profile.forEach { seed ->
                    val existing = dao.getByCreatureId(seed.creatureId)
                    when {
                        existing == null -> {
                            dao.insert(newEntity(seed, now))
                            inserted++
                        }
                        !existing.active -> skipped++
                        else -> {
                            val merged = merge(existing, seed)
                            if (merged == existing) {
                                unchanged++
                            } else {
                                dao.update(merged.copy(updatedAt = now, revision = existing.revision + 1))
                                updated++
                            }
                        }
                    }
                }
                val meta = database.seedMetaDao()
                val entry = SeedMetaEntity(SEED_KEY, SEED_VERSION, now)
                if (meta.get(SEED_KEY) == null) meta.insert(entry) else meta.update(entry)
                ProfileSeedResult(inserted, updated, unchanged, skipped)
            }
        }
    }

    private fun newEntity(
        seed: CreatureProfile,
        now: Long,
    ) = CreatureProfileMapping.applyValues(
        CreatureProfileEntity(
            id = newId(),
            creatureId = seed.creatureId,
            createdAt = now,
            updatedAt = now,
            revision = 1,
            seedVersion = SEED_VERSION,
        ),
        seed,
    )

    companion object {
        /** Bei geänderten Muster-Akten erhöhen (und in `creature_profiles.json` angleichen). */
        const val SEED_VERSION = 1
        const val SEED_KEY = "creature_profiles"

        /**
         * Übernimmt aus [seed] alle Felder, die der Seed ersetzen darf; behält den Rest.
         * Liefert [existing] unverändert (gleiches Objekt-Gleichheit), wenn nichts zu tun ist.
         */
        fun merge(
            existing: CreatureProfileEntity,
            seed: CreatureProfile,
        ): CreatureProfileEntity {
            val current = CreatureProfileMapping.toDomain(existing)
            var merged = current.copy(quellen = seed.quellen)
            val geleert = LinkedHashMap<ProfilFeld, Herkunft>()
            ProfilFeld.entries.forEach { feld ->
                val herkunft = CreatureProfileMapping.herkunft(existing, feld)
                if (herkunft == null || herkunft.seedDarfErsetzen) {
                    merged = merged.mitFeldAus(feld, seed)
                } else if (current.angabe(feld) == null) {
                    geleert[feld] = herkunft
                }
            }
            val candidate = CreatureProfileMapping.applyValues(existing, merged, geleert).copy(seedVersion = SEED_VERSION)
            return if (candidate == existing) existing else candidate
        }
    }
}
