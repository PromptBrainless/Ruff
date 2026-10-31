package de.ruff.dimension.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.local.RuffDatabase
import de.ruff.dimension.data.local.RuffDatabase.Companion.configure
import de.ruff.dimension.data.local.SeedImporter
import de.ruff.dimension.data.local.SeedSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.Reader

/** DAO und Repository der Favoriten gegen eine echte Room-Datenbank (Datei, damit Neustarts prüfbar sind). */
@RunWith(RobolectricTestRunner::class)
class FavoritesRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbFile: File = context.getDatabasePath("favoriten-test.db")
    private var now = 10_000L
    private var nextId = 0
    private lateinit var db: RuffDatabase
    private lateinit var repo: RoomFavoritesRepository

    private val assetsDir: File =
        listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }

    private val fileSource =
        object : SeedSource {
            override fun openCreatures(): Reader = File(assetsDir, AssetLoader.CREATURES_FILE).bufferedReader()

            override fun openRules(): Reader = File(assetsDir, AssetLoader.RULES_FILE).bufferedReader()
        }

    @Before
    fun setUp() {
        dbFile.parentFile?.mkdirs()
        dbFile.delete()
        open()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbFile.name)
    }

    private fun open() {
        db =
            Room
                .databaseBuilder(context, RuffDatabase::class.java, dbFile.absolutePath)
                .configure()
                .build()
        repo = RoomFavoritesRepository(db, clock = { now }, newId = { "uuid-${++nextId}" })
    }

    private fun reopen() {
        db.close()
        open()
    }

    @Test
    fun `Favorisieren legt genau eine Zeile mit Revision 1 an`() =
        runBlocking {
            assertTrue(repo.setFavorite("wesen-a", true))

            val row = db.favoriteDao().getByCreatureId("wesen-a")
            assertNotNull(row)
            assertEquals("uuid-1", row!!.id)
            assertEquals(10_000L, row.createdAt)
            assertEquals(10_000L, row.updatedAt)
            assertEquals(1L, row.revision)
            assertNull(row.deletedAt)
            assertEquals(setOf("wesen-a"), repo.observeFavoriteIds().first())
            assertTrue(repo.isFavorite("wesen-a"))
        }

    @Test
    fun `Entfernen ist ein Soft-Delete und erhoeht die Revision`() =
        runBlocking {
            repo.setFavorite("wesen-a", true)
            now = 20_000L
            assertFalse(repo.setFavorite("wesen-a", false))

            val row = db.favoriteDao().getByCreatureId("wesen-a")!!
            assertEquals(20_000L, row.deletedAt)
            assertEquals(20_000L, row.updatedAt)
            assertEquals(10_000L, row.createdAt)
            assertEquals(2L, row.revision)
            assertEquals(1, db.favoriteDao().getAll().size)
            assertEquals(0, db.favoriteDao().countActive())
            assertEquals(emptySet<String>(), repo.observeFavoriteIds().first())
            assertFalse(repo.isFavorite("wesen-a"))
        }

    @Test
    fun `erneutes Favorisieren belebt dieselbe Zeile statt eine neue anzulegen`() =
        runBlocking {
            repo.setFavorite("wesen-a", true)
            now = 20_000L
            repo.setFavorite("wesen-a", false)
            now = 30_000L
            assertTrue(repo.setFavorite("wesen-a", true))

            val rows = db.favoriteDao().getAll()
            assertEquals(1, rows.size)
            val row = rows.single()
            assertEquals("uuid-1", row.id)
            assertNull(row.deletedAt)
            assertEquals(30_000L, row.updatedAt)
            assertEquals(10_000L, row.createdAt)
            assertEquals(3L, row.revision)
        }

    @Test
    fun `gleicher Zustand erneut gesetzt aendert weder Zeitstempel noch Revision`() =
        runBlocking {
            repo.setFavorite("wesen-a", true)
            now = 99_000L
            repo.setFavorite("wesen-a", true)
            assertEquals(1L, db.favoriteDao().getByCreatureId("wesen-a")!!.revision)
            assertEquals(10_000L, db.favoriteDao().getByCreatureId("wesen-a")!!.updatedAt)

            // Entfernen eines Nicht-Favoriten legt keine Zeile an.
            assertFalse(repo.setFavorite("wesen-b", false))
            assertNull(db.favoriteDao().getByCreatureId("wesen-b"))
        }

    @Test
    fun `toggle wechselt den Zustand und liefert den neuen Wert`() =
        runBlocking {
            assertTrue(repo.toggle("wesen-a"))
            assertFalse(repo.toggle("wesen-a"))
            assertTrue(repo.toggle("wesen-a"))
            assertEquals(3L, db.favoriteDao().getByCreatureId("wesen-a")!!.revision)
        }

    @Test
    fun `parallele Toggles gehen nicht verloren`() =
        runBlocking {
            // Gerade Anzahl Umschaltungen → wieder kein Favorit, Revision zählt jede einzelne.
            (1..10).map { async { repo.toggle("wesen-a") } }.awaitAll()
            val row = db.favoriteDao().getByCreatureId("wesen-a")!!
            assertEquals(10L, row.revision)
            assertFalse(repo.isFavorite("wesen-a"))
        }

    @Test
    fun `Reihenfolge folgt dem Favorisieren`() =
        runBlocking {
            repo.setFavorite("c", true)
            now += 1
            repo.setFavorite("a", true)
            now += 1
            repo.setFavorite("b", true)
            assertEquals(listOf("c", "a", "b"), repo.observeFavoriteIds().first().toList())
        }

    @Test(expected = IllegalArgumentException::class)
    fun `leere Wesen-ID wird abgelehnt`() {
        runBlocking { repo.toggle(" ") }
    }

    @Test
    fun `Favoriten ueberstehen einen Neustart der Datenbank`() =
        runBlocking {
            repo.setFavorite("wesen-a", true)
            repo.setFavorite("wesen-b", true)
            repo.setFavorite("wesen-b", false)

            reopen()

            assertEquals(setOf("wesen-a"), repo.observeFavoriteIds().first())
            assertEquals(2, db.favoriteDao().getAll().size)
        }

    @Test
    fun `Favoriten ueberstehen wiederholten und erzwungenen Seed-Import`() =
        runBlocking {
            val importer = SeedImporter(db, fileSource, clock = { now })
            assertTrue(importer.seedIfNeeded())
            val ids = db.creatureDao().getAll().map { it.id }
            repo.setFavorite(ids[0], true)
            repo.setFavorite(ids[5], true)
            repo.setFavorite(ids[5], false)
            val before = db.favoriteDao().getAll()

            importer.importAll()
            reopen()
            val importerAfterRestart = SeedImporter(db, fileSource, clock = { now })
            assertFalse(importerAfterRestart.seedIfNeeded())
            importerAfterRestart.importAll()

            assertEquals(before, db.favoriteDao().getAll())
            assertEquals(setOf(ids[0]), repo.observeFavoriteIds().first())
            assertEquals(187, db.creatureDao().count())
        }
}
