package de.ruff.dimension.data.local

import android.content.res.AssetManager
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import java.io.Reader

/** Liefert die Seed-Dateien; in der App aus den Assets, in Tests vom Dateisystem. */
internal interface SeedSource {
    fun openCreatures(): Reader

    fun openRules(): Reader
}

internal class AssetSeedSource(
    private val assets: AssetManager,
) : SeedSource {
    override fun openCreatures(): Reader = assets.open(AssetLoader.CREATURES_FILE).bufferedReader()

    override fun openRules(): Reader = assets.open(AssetLoader.RULES_FILE).bufferedReader()
}

/**
 * Spielt `creatures.json` und `rules.json` in die Datenbank ein.
 *
 * - Läuft nur, wenn die gespeicherte Seed-Version kleiner als [SEED_VERSION] ist.
 * - Alles geschieht in einer Transaktion: entweder vollständig oder gar nicht.
 * - Inhaltstabellen werden per Upsert geschrieben, nie per REPLACE, damit keine
 *   Fremdschlüssel-Kaskaden ausgelöst werden. Es werden keine Wesen oder Seiten
 *   gelöscht; nur überzählige Blöcke einer gekürzten Seite werden entfernt.
 * - Nutzertabellen (künftig Favoriten/Notizen) werden hier nie angefasst.
 * - Der FTS5-Index ([SearchIndex]) wird von Triggern mitgeführt und am Ende
 *   derselben Transaktion zusätzlich vollständig neu aufgebaut.
 */
internal class SeedImporter(
    private val database: RuffDatabase,
    private val source: SeedSource,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Gibt `true` zurück, wenn ein Seed eingespielt wurde. */
    suspend fun seedIfNeeded(): Boolean {
        val stored = database.seedMetaDao().get(SEED_KEY)?.version ?: 0
        if (stored >= SEED_VERSION) return false
        importAll()
        return true
    }

    /** Spielt die Assets unabhängig von der Version ein; idempotent. */
    suspend fun importAll() {
        val creatures = source.openCreatures().use { AssetLoader.parseCreatures(it) }
        val rules = source.openRules().use { AssetLoader.parseRules(it) }
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                database.creatureDao().upsertAll(creatures.mapIndexed { index, it -> it.toEntity(index) })
                val ruleDao = database.ruleDao()
                ruleDao.upsertPages(rules.mapIndexed { index, it -> it.toEntity(index) })
                rules.forEach { page ->
                    val pageId = page.stableId()
                    ruleDao.upsertBlocks(
                        page.blocks.mapIndexed { index, block ->
                            RuleBlockEntity(pageId = pageId, position = index, type = block.type, text = block.text)
                        },
                    )
                    ruleDao.deleteBlocksFrom(pageId, page.blocks.size)
                }
                database.seedMetaDao().upsert(SeedMetaEntity(SEED_KEY, SEED_VERSION, clock()))
                SearchIndex.REBUILD.forEach { connection.execSQL(it) }
            }
        }
    }

    companion object {
        /** Bei geänderten Asset-Daten erhöhen, damit der Seed erneut läuft. */
        const val SEED_VERSION = 1
        const val SEED_KEY = "assets"
    }
}

/** Stabile Seiten-ID: der eindeutige Quellpfad aus dem Notion-Export. */
internal fun RulePage.stableId(): String = source

internal fun Creature.toEntity(position: Int) =
    CreatureEntity(
        id = id,
        position = position,
        name = name,
        source = source,
        status = status,
        powerLevel = powerLevel,
        powerLabel = powerLabel,
        spielbar = spielbar,
        description = description,
        archetype = archetype,
        portrait = portrait,
        icon = icon,
        cover = cover,
        fields = fields,
    )

internal fun RulePage.toEntity(position: Int) =
    RulePageEntity(id = stableId(), position = position, title = title, source = source, fields = fields)
