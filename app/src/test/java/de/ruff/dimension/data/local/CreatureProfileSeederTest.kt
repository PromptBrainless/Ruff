package de.ruff.dimension.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.ruff.dimension.data.AkteJson
import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.local.RuffDatabase.Companion.configure
import de.ruff.dimension.data.repository.RoomCreatureProfileRepository
import de.ruff.dimension.data.repository.RoomSearchRepository
import de.ruff.dimension.domain.akte.Angabe
import de.ruff.dimension.domain.akte.GanzzahlSpanne
import de.ruff.dimension.domain.akte.Herkunft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.Reader

/** Akten-Seed: idempotent, lässt Wesen unberührt, überschreibt nie Fynns Werte. */
@RunWith(RobolectricTestRunner::class)
class CreatureProfileSeederTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbFile: File = context.getDatabasePath("akten-seed-test.db")
    private val assetsDir: File =
        listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
    private var now = 1_000L
    private lateinit var db: RuffDatabase

    private val contentSource =
        object : SeedSource {
            override fun openCreatures(): Reader = File(assetsDir, AssetLoader.CREATURES_FILE).bufferedReader()

            override fun openRules(): Reader = File(assetsDir, AssetLoader.RULES_FILE).bufferedReader()
        }

    private val profileSource = ProfileSeedSource { File(assetsDir, AkteJson.SEED_FILE).bufferedReader() }

    private fun seeder(source: ProfileSeedSource = profileSource) = CreatureProfileSeeder(db, source, clock = { now })

    @Before
    fun setUp() {
        dbFile.parentFile?.mkdirs()
        context.deleteDatabase(dbFile.name)
        db = Room.databaseBuilder(context, RuffDatabase::class.java, dbFile.absolutePath).configure().build()
        runBlocking { SeedImporter(db, contentSource).seedIfNeeded() }
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbFile.name)
    }

    @Test
    fun `Seed legt sechs Akten an und laesst die 187 Wesen unveraendert`() =
        runBlocking {
            val before = db.creatureDao().getAll()
            assertEquals(187, before.size)
            val result = seeder().seedIfNeeded()
            assertEquals(ProfileSeedResult(inserted = 6, updated = 0, unchanged = 0, skippedDeleted = 0), result)
            assertEquals(6, db.creatureProfileDao().countActive())
            assertEquals(before, db.creatureDao().getAll())
            assertEquals(CreatureProfileSeeder.SEED_VERSION, db.seedMetaDao().get(CreatureProfileSeeder.SEED_KEY)?.version)
            // Inhalts-Seed-Version bleibt unberührt.
            assertEquals(SeedImporter.SEED_VERSION, db.seedMetaDao().get(SeedImporter.SEED_KEY)?.version)
        }

    @Test
    fun `zweiter Lauf ist ein No-op`() =
        runBlocking {
            seeder().seedIfNeeded()
            val rows = db.creatureProfileDao().getAll()
            now = 2_000L
            assertNull(seeder().seedIfNeeded())
            val again = seeder().importAll()
            assertEquals(ProfileSeedResult(0, 0, 6, 0), again)
            assertEquals(rows, db.creatureProfileDao().getAll())
        }

    @Test
    fun `manuelle und bestaetigte Werte uebersteht ein erneuter Seed`() =
        runBlocking {
            seeder().seedIfNeeded()
            val repo = RoomCreatureProfileRepository(db, clock = { now })
            val troll = repo.get("V2-090")!!
            repo.save(troll.copy(iq = Angabe(GanzzahlSpanne(90, 100), "Fynn"), eq = null))
            repo.confirmDerived("V2-006")
            now = 3_000L
            seeder().importAll()

            val nachher = repo.get("V2-090")!!
            assertEquals(GanzzahlSpanne(90, 100), nachher.iq!!.wert)
            assertEquals(Herkunft.MANUELL, nachher.iq!!.herkunft)
            // Bewusst geleert bleibt geleert.
            assertNull(nachher.eq)
            assertEquals(Herkunft.ABGELEITET, nachher.pulsBpm!!.herkunft)
            val kobold = repo.get("V2-006")!!
            assertEquals(0, kobold.offeneAbleitungen)
            assertTrue(kobold.vorhandeneFelder.all { kobold.angabe(it)!!.herkunft == Herkunft.VON_FYNN_BESTAETIGT })
        }

    @Test
    fun `verworfene Akten belebt der Seed nicht`() =
        runBlocking {
            seeder().seedIfNeeded()
            val dao = db.creatureProfileDao()
            val row = dao.getByCreatureId("V2-108")!!
            dao.update(row.copy(deletedAt = 5L, revision = row.revision + 1))
            val result = seeder().importAll()
            assertEquals(1, result.skippedDeleted)
            assertEquals(5, dao.countActive())
        }

    @Test
    fun `falsche Seed-Version bricht ohne Aenderung ab`() =
        runBlocking {
            val falsch =
                ProfileSeedSource {
                    """{"format":"ruff.kreaturen-akte","formatVersion":1,"seedVersion":99,"profile":[{"creatureId":"V2-090"}]}""".reader()
                }
            assertTrue(runCatching { seeder(falsch).importAll() }.isFailure)
            assertEquals(0, db.creatureProfileDao().countActive())
            assertNull(db.seedMetaDao().get(CreatureProfileSeeder.SEED_KEY))
        }

    @Test
    fun `Akten-Text ist in der Volltextsuche auffindbar und zeigt auf das Wesen`() =
        runBlocking {
            seeder().seedIfNeeded()
            val search = RoomSearchRepository(db)
            // „Flechten“ steht nur in der Troll-Akte, nicht im Katalog.
            val hits = search.search("Flechten")!!
            assertEquals(listOf("V2-090"), hits.creatureIds)
        }
}
