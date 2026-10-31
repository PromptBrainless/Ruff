package de.ruff.dimension.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.ruff.dimension.data.AkteJson
import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.local.RuffDatabase.Companion.configure
import de.ruff.dimension.data.repository.RoomFavoritesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Migrationen v2 → v3 → v4 mit echten Seed-Daten.
 *
 * Ohne `room-testing`: Die v2-Datenbank wird exakt aus dem exportierten Schema
 * `schemas/…/2.json` (createSql, setupQueries, Identity-Hash) plus Suchindex und
 * `content_edits` gebaut und mit den echten Assets befüllt – so, wie eine
 * installierte v2-App sie hinterlässt. Danach öffnet Room (aktuell v4) die Datei,
 * führt [MIGRATION_2_3] und [MIGRATION_3_4] aus und validiert das Schema gegen den
 * generierten Code; jede Abweichung würde dort mit IllegalStateException scheitern.
 * Für v3 → v4 entsteht die v3-Datei analog aus `3.json` inklusive Favoriten.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbFile: File = context.getDatabasePath("migration-test.db")
    private val schemaDir: File =
        listOf(File("schemas"), File("app/schemas"))
            .map { File(it, "de.ruff.dimension.data.local.RuffDatabase") }
            .first { it.isDirectory }
    private val assetsDir: File =
        listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
    private val converters = Converters()
    private var db: RuffDatabase? = null

    private fun schema(version: Int): JsonObject =
        JsonParser.parseString(File(schemaDir, "$version.json").readText()).asJsonObject["database"].asJsonObject

    @Before
    fun setUp() {
        dbFile.parentFile?.mkdirs()
        context.deleteDatabase(dbFile.name)
    }

    @After
    fun tearDown() {
        db?.close()
        context.deleteDatabase(dbFile.name)
    }

    /** Baut eine v2-Datei wie von der v2-App: Schema aus 2.json, Suchindex, Bearbeitungen, Seed. */
    private fun createV2WithSeed() {
        val v2 = schema(2)
        assertEquals(2, v2["version"].asInt)
        val creatures = File(assetsDir, AssetLoader.CREATURES_FILE).bufferedReader().use { AssetLoader.parseCreatures(it) }
        val rules = File(assetsDir, AssetLoader.RULES_FILE).bufferedReader().use { AssetLoader.parseRules(it) }
        BundledSQLiteDriver().open(dbFile.absolutePath).use { c ->
            c.execSQL("PRAGMA foreign_keys = ON")
            c.execSQL("BEGIN")
            v2["entities"].asJsonArray.forEach { e ->
                val entity = e.asJsonObject
                c.execSQL(entity["createSql"].asString.replace("\${TABLE_NAME}", entity["tableName"].asString))
            }
            v2["setupQueries"].asJsonArray.forEach { c.execSQL(it.asString) }
            SearchIndex.create(c)
            c.execSQL(RoomContentEditStore.CREATE_TABLE)

            creatures.forEachIndexed { index, creature -> c.insertCreature(creature.toEntity(index)) }
            rules.forEachIndexed { index, page ->
                val entity = page.toEntity(index)
                c.prepare("INSERT INTO rule_pages(id, position, title, source, fields) VALUES(?, ?, ?, ?, ?)").use {
                    it.bindText(1, entity.id)
                    it.bindLong(2, entity.position.toLong())
                    it.bindText(3, entity.title)
                    it.bindText(4, entity.source)
                    it.bindText(5, converters.fieldsToJson(entity.fields))
                    it.step()
                }
                page.blocks.forEachIndexed { position, block ->
                    c.prepare("INSERT INTO rule_blocks(pageId, position, type, text) VALUES(?, ?, ?, ?)").use {
                        it.bindText(1, entity.id)
                        it.bindLong(2, position.toLong())
                        it.bindText(3, block.type)
                        it.bindText(4, block.text)
                        it.step()
                    }
                }
            }
            SearchIndex.REBUILD.forEach { c.execSQL(it) }
            c.execSQL(
                "INSERT INTO seed_meta(`key`, version, seededAtMillis) " +
                    "VALUES('${SeedImporter.SEED_KEY}', ${SeedImporter.SEED_VERSION}, 1000)",
            )
            c.execSQL("INSERT INTO content_edits(contentType, refId, editedJson) VALUES('creature', '${creatures[0].id}', '{}')")
            c.execSQL("COMMIT")
            c.execSQL("PRAGMA user_version = 2")
        }
    }

    private fun SQLiteConnection.insertCreature(e: CreatureEntity) {
        prepare(
            "INSERT INTO creatures(id, position, name, source, status, powerLevel, powerLabel, spielbar, " +
                "description, archetype, portrait, icon, cover, fields) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ).use {
            it.bindText(1, e.id)
            it.bindLong(2, e.position.toLong())
            it.bindText(3, e.name)
            it.bindText(4, e.source)
            it.bindText(5, e.status)
            it.bindLong(6, e.powerLevel.toLong())
            it.bindText(7, e.powerLabel)
            if (e.spielbar == null) it.bindNull(8) else it.bindLong(8, if (e.spielbar) 1 else 0)
            it.bindText(9, e.description)
            it.bindText(10, e.archetype)
            it.bindText(11, e.portrait)
            it.bindText(12, e.icon)
            it.bindText(13, e.cover)
            it.bindText(14, converters.fieldsToJson(e.fields))
            it.step()
        }
    }

    private fun SQLiteConnection.scalar(sql: String): Long =
        prepare(sql).use {
            it.step()
            it.getLong(0)
        }

    private fun SQLiteConnection.text(sql: String): String =
        prepare(sql).use {
            it.step()
            it.getText(0)
        }

    /** Zahlen, die vor und nach der Migration identisch sein müssen. */
    private data class Counts(
        val creatures: Long,
        val pages: Long,
        val blocks: Long,
        val searchRows: Long,
        val edits: Long,
        val seedVersion: Long,
    )

    private fun SQLiteConnection.counts() =
        Counts(
            creatures = scalar("SELECT COUNT(*) FROM creatures"),
            pages = scalar("SELECT COUNT(*) FROM rule_pages"),
            blocks = scalar("SELECT COUNT(*) FROM rule_blocks"),
            searchRows = scalar("SELECT COUNT(*) FROM search_index"),
            edits = scalar("SELECT COUNT(*) FROM content_edits"),
            seedVersion = scalar("SELECT version FROM seed_meta WHERE `key` = '${SeedImporter.SEED_KEY}'"),
        )

    private fun openCurrent(): RuffDatabase =
        Room
            .databaseBuilder(context, RuffDatabase::class.java, dbFile.absolutePath)
            .configure()
            .build()
            .also { db = it }

    @Test
    fun `Migration 2 nach 4 behaelt alle Seed-Zahlen und legt leere Favoriten und Akten an`() =
        runBlocking {
            createV2WithSeed()
            val before = BundledSQLiteDriver().open(dbFile.absolutePath).use { it.counts() }
            assertEquals(Counts(187, 12, 1308, 199, 1, SeedImporter.SEED_VERSION.toLong()), before)

            val database = openCurrent()
            // Erster Zugriff öffnet die Datei: Room migriert und validiert das Schema.
            assertEquals(187, database.creatureDao().count())
            val after = database.useReaderConnection { it.usePrepared("SELECT COUNT(*) FROM favorites") { s -> s.step() } }
            assertTrue(after)
            database.close()
            db = null
            assertEquals(before, BundledSQLiteDriver().open(dbFile.absolutePath).use { it.counts() })

            BundledSQLiteDriver().open(dbFile.absolutePath).use { c ->
                assertEquals(4L, c.scalar("PRAGMA user_version"))
                assertEquals(schema(4)["identityHash"].asString, c.text("SELECT identity_hash FROM room_master_table WHERE id = 42"))
                assertEquals(0L, c.scalar("SELECT COUNT(*) FROM favorites"))
                assertEquals(1L, c.scalar("SELECT COUNT(*) FROM sqlite_master WHERE name = 'index_favorites_creatureId'"))
                assertEquals(0L, c.scalar("SELECT COUNT(*) FROM creature_profiles"))
                assertEquals(1L, c.scalar("SELECT COUNT(*) FROM sqlite_master WHERE name = 'index_creature_profiles_creatureId'"))
                // Suchindex und Trigger leben nach der Migration weiter; v4 ergänzt drei Akten-Trigger.
                assertEquals(
                    SEARCH_TRIGGERS_V4,
                    c.scalar("SELECT COUNT(*) FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'search_%'"),
                )
            }
            val reopened = openCurrent()
            assertEquals(187, reopened.creatureDao().count())
            assertEquals(0, reopened.favoriteDao().countActive())
        }

    @Test
    fun `nach der Migration sind Favoriten nutzbar und der Seed laeuft nicht erneut`() =
        runBlocking {
            createV2WithSeed()
            val database = openCurrent()
            val repo = RoomFavoritesRepository(database, clock = { 5_000L })
            val ids = database.creatureDao().getAll().map { it.id }

            assertTrue(repo.toggle(ids.first()))
            assertTrue(repo.toggle(ids.last()))
            assertEquals(setOf(ids.first(), ids.last()), repo.observeFavoriteIds().first())

            val source =
                object : SeedSource {
                    override fun openCreatures() = File(assetsDir, AssetLoader.CREATURES_FILE).bufferedReader()

                    override fun openRules() = File(assetsDir, AssetLoader.RULES_FILE).bufferedReader()
                }
            val importer = SeedImporter(database, source)
            assertFalse(importer.seedIfNeeded())
            importer.importAll()

            assertEquals(187, database.creatureDao().count())
            assertEquals(12, database.ruleDao().countPages())
            assertEquals(1308, database.ruleDao().countBlocks())
            assertEquals(2, database.favoriteDao().countActive())
        }

    @Test
    fun `Migrations-SQL entspricht exakt dem exportierten Schema 3`() {
        val v3 = schema(3)
        assertEquals(3, v3["version"].asInt)
        val favorites =
            v3["entities"].asJsonArray.map { it.asJsonObject }.single { it["tableName"].asString == "favorites" }
        assertEquals(
            FavoritesSchema.CREATE_TABLE,
            favorites["createSql"].asString.replace("\${TABLE_NAME}", "favorites"),
        )
        val index = favorites["indices"].asJsonArray.single().asJsonObject
        assertTrue(index["unique"].asBoolean)
        assertEquals(FavoritesSchema.CREATE_INDEX, index["createSql"].asString.replace("\${TABLE_NAME}", "favorites"))
        // Alle v2-Tabellen sind in v3 unverändert (rein additive Migration).
        val v2Tables = schema(2)["entities"].asJsonArray.map { it.asJsonObject }
        val v3Tables = v3["entities"].asJsonArray.map { it.asJsonObject }.associateBy { it["tableName"].asString }
        v2Tables.forEach { table ->
            assertEquals(table, v3Tables.getValue(table["tableName"].asString))
        }
        assertEquals(v2Tables.size + 1, v3Tables.size)
    }

    /** Baut eine v3-Datei wie von der v3-App (Schema aus 3.json, Seed, zwei Favoriten). */
    private fun createV3WithSeedAndFavorites() {
        createV2WithSeed()
        val v3 = schema(3)
        BundledSQLiteDriver().open(dbFile.absolutePath).use { c ->
            c.execSQL("BEGIN")
            val favorites = v3["entities"].asJsonArray.map { it.asJsonObject }.single { it["tableName"].asString == "favorites" }
            c.execSQL(favorites["createSql"].asString.replace("\${TABLE_NAME}", "favorites"))
            favorites["indices"].asJsonArray.forEach {
                c.execSQL(
                    it.asJsonObject["createSql"].asString.replace("\${TABLE_NAME}", "favorites"),
                )
            }
            c.execSQL("UPDATE room_master_table SET identity_hash = '${v3["identityHash"].asString}' WHERE id = 42")
            c.execSQL(
                "INSERT INTO favorites(id, creatureId, createdAt, updatedAt, revision, deletedAt) " +
                    "VALUES('fav-1', 'V2-090', 1, 1, 1, NULL), ('fav-2', 'V2-006', 2, 2, 1, NULL)",
            )
            c.execSQL("COMMIT")
            c.execSQL("PRAGMA user_version = 3")
        }
    }

    @Test
    fun `Migration 3 nach 4 ist additiv und behaelt Wesen, Suche und Favoriten`() =
        runBlocking {
            createV3WithSeedAndFavorites()
            val before = BundledSQLiteDriver().open(dbFile.absolutePath).use { it.counts() }
            assertEquals(187L, before.creatures)
            assertEquals(199L, before.searchRows)

            val database = openCurrent()
            assertEquals(187, database.creatureDao().count())
            assertEquals(2, database.favoriteDao().countActive())
            assertEquals(0, database.creatureProfileDao().countActive())
            database.close()
            db = null

            BundledSQLiteDriver().open(dbFile.absolutePath).use { c ->
                assertEquals(before, c.counts())
                assertEquals(4L, c.scalar("PRAGMA user_version"))
                assertEquals(schema(4)["identityHash"].asString, c.text("SELECT identity_hash FROM room_master_table WHERE id = 42"))
                assertEquals(
                    SEARCH_TRIGGERS_V4,
                    c.scalar("SELECT COUNT(*) FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'search_%'"),
                )
                assertEquals(2L, c.scalar("SELECT COUNT(*) FROM favorites WHERE deletedAt IS NULL"))
            }
        }

    @Test
    fun `nach Migration 3 nach 4 spielt der Akten-Seed ein, ohne Wesen zu aendern`() =
        runBlocking {
            createV3WithSeedAndFavorites()
            val database = openCurrent()
            val creaturesBefore = database.creatureDao().getAll()
            val seeder =
                CreatureProfileSeeder(
                    database,
                    ProfileSeedSource { File(assetsDir, AkteJson.SEED_FILE).bufferedReader() },
                    clock = { 7_000L },
                )
            val result = seeder.seedIfNeeded()
            assertEquals(6, result?.inserted)
            assertEquals(6, database.creatureProfileDao().countActive())
            assertEquals(creaturesBefore, database.creatureDao().getAll())
            assertEquals(2, database.favoriteDao().countActive())
            val searchRows =
                database.useReaderConnection { c ->
                    c.usePrepared("SELECT COUNT(*) FROM search_index WHERE type = '${ProfileSearch.TYPE}'") {
                        it.step()
                        it.getLong(0)
                    }
                }
            assertEquals(6L, searchRows)
        }

    @Test
    fun `Migrations-SQL entspricht exakt dem exportierten Schema 4`() {
        val v4 = schema(4)
        assertEquals(4, v4["version"].asInt)
        val profiles =
            v4["entities"].asJsonArray.map { it.asJsonObject }.single { it["tableName"].asString == "creature_profiles" }
        assertEquals(
            CreatureProfilesSchema.CREATE_TABLE,
            profiles["createSql"].asString.replace("\${TABLE_NAME}", "creature_profiles"),
        )
        val index = profiles["indices"].asJsonArray.single().asJsonObject
        assertTrue(index["unique"].asBoolean)
        assertEquals(
            CreatureProfilesSchema.CREATE_INDEX,
            index["createSql"].asString.replace("\${TABLE_NAME}", "creature_profiles"),
        )
        // Alle v3-Tabellen sind in v4 unverändert (rein additive Migration).
        val v3Tables = schema(3)["entities"].asJsonArray.map { it.asJsonObject }
        val v4Tables = v4["entities"].asJsonArray.map { it.asJsonObject }.associateBy { it["tableName"].asString }
        v3Tables.forEach { table ->
            assertEquals(table, v4Tables.getValue(table["tableName"].asString))
        }
        assertEquals(v3Tables.size + 1, v4Tables.size)
    }

    private companion object {
        /** 10 Inhalts-Trigger (v2) + 3 Akten-Trigger (v4). */
        const val SEARCH_TRIGGERS_V4 = 13L
    }
}
