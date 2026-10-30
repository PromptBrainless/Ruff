package de.ruff.dimension.domain

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.local.RuffDatabase
import de.ruff.dimension.data.local.SeedImporter
import de.ruff.dimension.data.local.SeedSource
import de.ruff.dimension.data.repository.RoomCreatureRepository
import de.ruff.dimension.data.repository.RoomRuleRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import java.io.Reader

/** Room-Weg inklusive Mapper und Asset-Fallback gegen das direkte AssetLoader-Ergebnis. */
@RunWith(RobolectricTestRunner::class)
class LibrarySourceTest {
    private val assetsDir: File =
        listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }

    private val fileSource =
        object : SeedSource {
            override fun openCreatures(): Reader = File(assetsDir, AssetLoader.CREATURES_FILE).bufferedReader()

            override fun openRules(): Reader = File(assetsDir, AssetLoader.RULES_FILE).bufferedReader()
        }
    private val brokenSource =
        object : SeedSource {
            override fun openCreatures(): Reader = throw IOException("kaputt")

            override fun openRules(): Reader = throw IOException("kaputt")
        }

    private val expectedCreatures = fileSource.openCreatures().use { AssetLoader.parseCreatures(it) }
    private val expectedRules = fileSource.openRules().use { AssetLoader.parseRules(it) }

    private lateinit var db: RuffDatabase
    private var seedRuns = 0
    private val errors = mutableListOf<Throwable>()

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), RuffDatabase::class.java)
                .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val failingSeed: suspend () -> Unit = { throw SQLiteException("Datenbank nicht nutzbar") }

    private fun source(
        fallback: SeedSource = fileSource,
        seed: (suspend () -> Unit)? = null,
    ): SeededLibrarySource {
        val importer = SeedImporter(db, fileSource)
        return SeededLibrarySource(
            seed = seed ?: { if (importer.seedIfNeeded()) seedRuns++ },
            creatures = RoomCreatureRepository(db.creatureDao()),
            rules = RoomRuleRepository(db.ruleDao()),
            fallback = fallback,
            onDatabaseError = { errors += it },
        )
    }

    @Test
    fun loadsFromRoomIdenticalToAssetLoader() =
        runBlocking {
            val data = source().load()
            assertEquals(DataOrigin.DATABASE, data.origin)
            assertEquals(187, data.creatures.size)
            assertEquals(12, data.rules.size)
            assertEquals(expectedCreatures, data.creatures)
            assertEquals(expectedRules, data.rules)
            data.creatures.zip(expectedCreatures).forEach { (actual, expected) ->
                assertEquals(expected.name, expected.fields.keys.toList(), actual.fields.keys.toList())
            }
            data.rules.zip(expectedRules).forEach { (actual, expected) ->
                assertEquals(expected.title, expected.fields.keys.toList(), actual.fields.keys.toList())
                assertEquals(expected.blocks, actual.blocks)
            }
            assertEquals(1308, data.rules.sumOf { it.blocks.size })
            assertTrue(errors.isEmpty())
        }

    @Test
    fun seedRunsOnlyOnFirstLoad() =
        runBlocking {
            val first = source().load()
            val second = source().load()
            assertEquals(1, seedRuns)
            assertEquals(first, second)
        }

    @Test
    fun fallsBackToAssetsWhenDatabaseFails() =
        runBlocking {
            val data = source(seed = failingSeed).load()
            assertEquals(DataOrigin.ASSETS_FALLBACK, data.origin)
            assertEquals(expectedCreatures, data.creatures)
            assertEquals(expectedRules, data.rules)
            assertEquals(1, errors.size)
        }

    @Test
    fun fallsBackToAssetsWhenDatabaseStaysEmpty() =
        runBlocking {
            val data = source(seed = {}).load()
            assertEquals(DataOrigin.ASSETS_FALLBACK, data.origin)
            assertEquals(expectedCreatures, data.creatures)
            assertEquals(1, errors.size)
        }

    @Test
    fun throwsWhenDatabaseAndAssetsFail() =
        runBlocking {
            try {
                source(fallback = brokenSource, seed = failingSeed).load()
                fail("load() muss werfen, wenn auch die Assets fehlen")
            } catch (e: IOException) {
                assertNotNull(e.message)
            }
            assertFalse(errors.isEmpty())
        }
}
