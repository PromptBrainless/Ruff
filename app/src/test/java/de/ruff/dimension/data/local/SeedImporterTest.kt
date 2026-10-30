package de.ruff.dimension.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.local.RuffDatabase.Companion.configure
import de.ruff.dimension.data.repository.RoomCreatureRepository
import de.ruff.dimension.data.repository.RoomRuleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.Reader

/** Seed der echten Assets in eine In-Memory-Datenbank (Robolectric, ohne Emulator). */
@RunWith(RobolectricTestRunner::class)
class SeedImporterTest {
    private val assetsDir: File =
        listOf(File("src/main/assets"), File("app/src/main/assets"))
            .firstOrNull { it.isDirectory }
            ?: error("src/main/assets nicht gefunden (Arbeitsverzeichnis: ${File(".").absolutePath})")

    private val fileSource =
        object : SeedSource {
            override fun openCreatures(): Reader = File(assetsDir, AssetLoader.CREATURES_FILE).bufferedReader()

            override fun openRules(): Reader = File(assetsDir, AssetLoader.RULES_FILE).bufferedReader()
        }

    private val jsonCreatures: List<Creature> by lazy { fileSource.openCreatures().use { AssetLoader.parseCreatures(it) } }
    private val jsonRules: List<RulePage> by lazy { fileSource.openRules().use { AssetLoader.parseRules(it) } }

    private lateinit var db: RuffDatabase
    private var now = 1_000L
    private lateinit var importer: SeedImporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, RuffDatabase::class.java)
                .configure()
                .allowMainThreadQueries()
                .build()
        importer = SeedImporter(db, fileSource, clock = { now })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `Seed ergibt 187 Wesen 12 Seiten und 1308 Bloecke`() =
        runBlocking {
            assertTrue(importer.seedIfNeeded())

            assertEquals(187, db.creatureDao().count())
            assertEquals(12, db.ruleDao().countPages())
            assertEquals(1308, db.ruleDao().countBlocks())
            jsonRules.forEach { page ->
                assertEquals(page.title, page.blocks.size, db.ruleDao().getBlocks(page.source).size)
            }
        }

    @Test
    fun `alle Wesen und fields sind nach dem Roundtrip identisch zum JSON`() =
        runBlocking {
            importer.seedIfNeeded()
            val fromDb = RoomCreatureRepository(db.creatureDao()).getCreatures()

            assertEquals(jsonCreatures, fromDb)
            jsonCreatures.zip(fromDb).forEach { (json, stored) ->
                assertEquals("Schlüsselreihenfolge ${json.id}", json.fields.keys.toList(), stored.fields.keys.toList())
            }
        }

    @Test
    fun `Regelseiten und Bloecke sind nach dem Roundtrip identisch zum JSON`() =
        runBlocking {
            importer.seedIfNeeded()
            val repository = RoomRuleRepository(db.ruleDao())

            assertEquals(jsonRules, repository.getRulePages())
            assertEquals(jsonRules.first(), repository.getRulePage(jsonRules.first().source))
        }

    @Test
    fun `Entities decken alle Schluessel der JSON-Dateien ab`() {
        val creatureKeys =
            File(assetsDir, AssetLoader.CREATURES_FILE)
                .reader()
                .use { JsonParser.parseReader(it) }
                .asJsonArray
                .flatMap { it.asJsonObject.keySet() }
                .toSet()
        val ruleKeys =
            File(assetsDir, AssetLoader.RULES_FILE)
                .reader()
                .use { JsonParser.parseReader(it) }
                .asJsonArray
                .flatMap { it.asJsonObject.keySet() }
                .toSet()
        val creatureColumns =
            CreatureEntity::class.java.declaredFields
                .map { it.name }
                .toSet()

        assertEquals(emptySet<String>(), creatureKeys - creatureColumns)
        assertEquals(setOf("title", "source", "fields", "blocks"), ruleKeys)
    }

    @Test
    fun `zweiter Seed-Lauf ist idempotent`() =
        runBlocking {
            assertTrue(importer.seedIfNeeded())
            val creaturesBefore = db.creatureDao().getAll()
            val pagesBefore = db.ruleDao().getPages()
            val blocksBefore = db.ruleDao().getAllBlocks()
            val metaBefore = db.seedMetaDao().get(SeedImporter.SEED_KEY)

            now = 2_000L
            assertFalse(importer.seedIfNeeded())
            assertEquals(metaBefore, db.seedMetaDao().get(SeedImporter.SEED_KEY))

            // Auch ein erzwungener Lauf erzeugt keine Duplikate und ändert keine Inhalte.
            importer.importAll()
            assertEquals(creaturesBefore, db.creatureDao().getAll())
            assertEquals(pagesBefore, db.ruleDao().getPages())
            assertEquals(blocksBefore, db.ruleDao().getAllBlocks())
        }

    @Test
    fun `wiederholter Import haelt 187 12 1308 und den Suchindex exakt`() =
        runBlocking {
            repeat(3) { importer.importAll() }

            assertEquals(187, db.creatureDao().count())
            assertEquals(12, db.ruleDao().countPages())
            assertEquals(1308, db.ruleDao().countBlocks())
            assertEquals(187 + 12, countSearchRows())
        }

    @Test
    fun `geaenderte Seed-Daten werden aktualisiert ohne Duplikate`() =
        runBlocking {
            importer.importAll()
            val first = jsonCreatures.first()
            val shortened = jsonRules.first()
            val changedSource =
                object : SeedSource {
                    override fun openCreatures(): Reader =
                        java.io.StringReader(
                            com.google.gson.Gson().toJson(
                                File(assetsDir, AssetLoader.CREATURES_FILE).reader().use { JsonParser.parseReader(it) }.asJsonArray.also {
                                    it[0].asJsonObject.addProperty("name", first.name + " (neu)")
                                },
                            ),
                        )

                    override fun openRules(): Reader =
                        java.io.StringReader(
                            com.google.gson.Gson().toJson(
                                File(assetsDir, AssetLoader.RULES_FILE).reader().use { JsonParser.parseReader(it) }.asJsonArray.also {
                                    val blocks = it[0].asJsonObject.getAsJsonArray("blocks")
                                    blocks.remove(blocks.size() - 1)
                                },
                            ),
                        )
                }
            SeedImporter(db, changedSource, clock = { now }).importAll()

            assertEquals(187, db.creatureDao().count())
            assertEquals(first.name + " (neu)", db.creatureDao().getById(first.id)?.name)
            assertEquals(12, db.ruleDao().countPages())
            assertEquals(1307, db.ruleDao().countBlocks())
            assertEquals(shortened.blocks.size - 1, db.ruleDao().getBlocks(shortened.source).size)
            assertEquals(187 + 12, countSearchRows())
        }

    private suspend fun countSearchRows(): Int =
        db.useReaderConnection { connection ->
            connection.usePrepared("SELECT COUNT(*) FROM ${SearchIndex.TABLE}") { statement ->
                statement.step()
                statement.getLong(0).toInt()
            }
        }

    @Test
    fun `Seed-Version und Zeitpunkt werden gesetzt`() =
        runBlocking {
            assertEquals(null, db.seedMetaDao().get(SeedImporter.SEED_KEY))
            importer.seedIfNeeded()

            val meta = db.seedMetaDao().get(SeedImporter.SEED_KEY)
            assertNotNull(meta)
            assertEquals(SeedImporter.SEED_VERSION, meta!!.version)
            assertEquals(1_000L, meta.seededAtMillis)
        }

    @Test
    fun `Flow liefert Wesen in JSON-Reihenfolge und Einzelabruf per ID`() =
        runBlocking {
            importer.seedIfNeeded()

            val observed = db.creatureDao().observeAll().first()
            assertEquals(jsonCreatures.map { it.id }, observed.map { it.id })
            assertEquals("Aziza", db.creatureDao().getById("V2-I-012")?.name)
            assertEquals(null, db.creatureDao().getById("gibt-es-nicht"))
        }
}
