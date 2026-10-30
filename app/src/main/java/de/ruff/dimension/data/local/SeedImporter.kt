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
 * - Inhaltstabellen werden abgeglichen (neu → INSERT, geändert → UPDATE,
 *   unverändert → nichts), nie per REPLACE, damit keine Fremdschlüssel-Kaskaden
 *   ausgelöst werden. Es werden keine Wesen oder Seiten
 *   gelöscht; nur überzählige Blöcke einer gekürzten Seite werden entfernt.
 * - Nutzertabellen (Favoriten, künftig Notizen) werden hier nie angefasst.
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
                syncCreatures(creatures.mapIndexed { index, it -> it.toEntity(index) })
                syncRules(rules)
                syncMeta(SeedMetaEntity(SEED_KEY, SEED_VERSION, clock()))
                SearchIndex.REBUILD.forEach { connection.execSQL(it) }
            }
        }
    }

    /*
     * Abgleich statt @Upsert: Room erkennt bei @Upsert einen Schlüsselkonflikt nur
     * am Text der SQLException („unique“, „1555“ oder „2067“). Liefert der Treiber
     * diesen Text nicht (gebündeltes SQLite unter der JVM/Robolectric meldet nur
     * „Error code: “), wirft der zweite Lauf statt zu aktualisieren. Deshalb werden
     * vorhandene Schlüssel vorher gelesen: neue Zeilen per INSERT (ABORT), geänderte
     * per UPDATE, unveränderte gar nicht – unabhängig von Treiber und Fehlertext,
     * und ohne Trigger-Last für unveränderte Zeilen.
     */

    private suspend fun syncCreatures(entities: List<CreatureEntity>) {
        val dao = database.creatureDao()
        val existing = dao.getAll().associateBy { it.id }
        val (known, fresh) = entities.partition { it.id in existing }
        if (fresh.isNotEmpty()) dao.insertAll(fresh)
        val changed = known.filter { existing[it.id] != it }
        if (changed.isNotEmpty()) dao.updateAll(changed)
    }

    private suspend fun syncRules(rules: List<RulePage>) {
        val dao = database.ruleDao()
        val pages = rules.mapIndexed { index, it -> it.toEntity(index) }
        val existingPages = dao.getPages().associateBy { it.id }
        val (knownPages, freshPages) = pages.partition { it.id in existingPages }
        if (freshPages.isNotEmpty()) dao.insertPages(freshPages)
        val changedPages = knownPages.filter { existingPages[it.id] != it }
        if (changedPages.isNotEmpty()) dao.updatePages(changedPages)

        val existingBlocks = dao.getAllBlocks().associateBy { it.pageId to it.position }
        rules.forEach { page ->
            val pageId = page.stableId()
            val blocks =
                page.blocks.mapIndexed { index, block ->
                    RuleBlockEntity(pageId = pageId, position = index, type = block.type, text = block.text)
                }
            val (knownBlocks, freshBlocks) = blocks.partition { (it.pageId to it.position) in existingBlocks }
            if (freshBlocks.isNotEmpty()) dao.insertBlocks(freshBlocks)
            val changedBlocks = knownBlocks.filter { existingBlocks[it.pageId to it.position] != it }
            if (changedBlocks.isNotEmpty()) dao.updateBlocks(changedBlocks)
            dao.deleteBlocksFrom(pageId, page.blocks.size)
        }
    }

    private suspend fun syncMeta(meta: SeedMetaEntity) {
        val dao = database.seedMetaDao()
        if (dao.get(meta.key) == null) dao.insert(meta) else dao.update(meta)
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
