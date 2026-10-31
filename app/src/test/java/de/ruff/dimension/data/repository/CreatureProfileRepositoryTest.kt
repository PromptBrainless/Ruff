package de.ruff.dimension.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.ruff.dimension.data.AkteJson
import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.local.CreatureProfileSeeder
import de.ruff.dimension.data.local.ProfileSeedSource
import de.ruff.dimension.data.local.RuffDatabase
import de.ruff.dimension.data.local.RuffDatabase.Companion.configure
import de.ruff.dimension.data.local.SeedImporter
import de.ruff.dimension.data.local.SeedSource
import de.ruff.dimension.domain.akte.AkteEntwurf
import de.ruff.dimension.domain.akte.Angabe
import de.ruff.dimension.domain.akte.CreatureProfile
import de.ruff.dimension.domain.akte.GanzzahlSpanne
import de.ruff.dimension.domain.akte.Herkunft
import de.ruff.dimension.domain.akte.ProfilFeld
import de.ruff.dimension.domain.akte.Spanne
import kotlinx.coroutines.flow.first
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

/** Bearbeiten, Bestätigen und Sichern der Akten gegen eine echte Datenbankdatei (Neustart prüfbar). */
@RunWith(RobolectricTestRunner::class)
class CreatureProfileRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbFile: File = context.getDatabasePath("akten-repo-test.db")
    private val assetsDir: File =
        listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
    private var now = 10_000L
    private var nextId = 0
    private lateinit var db: RuffDatabase
    private lateinit var repo: RoomCreatureProfileRepository

    private val contentSource =
        object : SeedSource {
            override fun openCreatures(): Reader = File(assetsDir, AssetLoader.CREATURES_FILE).bufferedReader()

            override fun openRules(): Reader = File(assetsDir, AssetLoader.RULES_FILE).bufferedReader()
        }

    @Before
    fun setUp() {
        dbFile.parentFile?.mkdirs()
        context.deleteDatabase(dbFile.name)
        open()
        runBlocking {
            SeedImporter(db, contentSource).seedIfNeeded()
            CreatureProfileSeeder(db, ProfileSeedSource { File(assetsDir, AkteJson.SEED_FILE).bufferedReader() }, clock = { now })
                .seedIfNeeded()
        }
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbFile.name)
    }

    private fun open() {
        db = Room.databaseBuilder(context, RuffDatabase::class.java, dbFile.absolutePath).configure().build()
        repo = RoomCreatureProfileRepository(db, clock = { now }, newId = { "akte-${++nextId}" })
    }

    private fun reopen() {
        db.close()
        open()
    }

    @Test
    fun `nur geaenderte Felder werden manuell, der Rest bleibt abgeleitet`() =
        runBlocking {
            val troll = repo.get("V2-090")!!
            val gespeichert = repo.save(troll.copy(groesseM = Angabe(Spanne(1.9, 2.3), troll.groesseM!!.begruendung)))
            assertEquals(Herkunft.MANUELL, gespeichert.groesseM!!.herkunft)
            assertEquals(Spanne(1.9, 2.3), gespeichert.groesseM!!.wert)
            (ProfilFeld.entries - ProfilFeld.GROESSE).forEach { assertEquals(Herkunft.ABGELEITET, gespeichert.angabe(it)!!.herkunft) }
            assertEquals(2L, db.creatureProfileDao().getByCreatureId("V2-090")!!.revision)
        }

    @Test
    fun `neue Begruendung allein macht das Feld manuell`() =
        runBlocking {
            val troll = repo.get("V2-090")!!
            val gespeichert = repo.save(troll.copy(iq = troll.iq!!.copy(begruendung = "Fynn: eher schlau")))
            assertEquals(Herkunft.MANUELL, gespeichert.iq!!.herkunft)
        }

    @Test
    fun `Speichern ohne Aenderung schreibt nichts`() =
        runBlocking {
            val vorher = db.creatureProfileDao().getByCreatureId("V2-090")!!
            now = 99_000L
            // Der Editor liefert alles als MANUELL; unveränderte Werte behalten trotzdem ihre Herkunft.
            val entwurf = AkteEntwurf.aus(repo.get("V2-090"))
            val profil = (AkteEntwurf.zuProfil("V2-090", entwurf) as AkteEntwurf.Ergebnis.Ok).profile
            repo.save(profil)
            assertEquals(vorher, db.creatureProfileDao().getByCreatureId("V2-090"))
        }

    @Test
    fun `Bearbeitung uebersteht einen Neustart`() =
        runBlocking {
            val kobold = repo.get("V2-006")!!
            repo.save(kobold.copy(wahnhaft = Angabe(true, "Fynn"), pulsBpm = null))
            reopen()
            val nachher = repo.get("V2-006")!!
            assertEquals(true, nachher.wahnhaft!!.wert)
            assertEquals(Herkunft.MANUELL, nachher.wahnhaft!!.herkunft)
            assertNull(nachher.pulsBpm)
            assertEquals(Herkunft.MANUELL, CreatureProfileMappingProbe.herkunftPuls(db, "V2-006"))
        }

    @Test
    fun `neue Akte fuer ein Wesen ohne Akte`() =
        runBlocking {
            assertNull(repo.get("V2-001"))
            repo.save(CreatureProfile("V2-001", iq = Angabe(GanzzahlSpanne(100, 110), "Fynn", Herkunft.MANUELL)))
            val neu = repo.get("V2-001")!!
            assertEquals(Herkunft.MANUELL, neu.iq!!.herkunft)
            assertEquals(7, repo.observeProfiles().first().size)
        }

    @Test
    fun `Bestaetigen markiert nur abgeleitete Werte`() =
        runBlocking {
            val sphinx = repo.get("V2-108")!!
            repo.save(sphinx.copy(eq = Angabe(GanzzahlSpanne(60, 80), "Fynn")))
            assertEquals(11, repo.confirmDerived("V2-108"))
            val nachher = repo.get("V2-108")!!
            assertEquals(Herkunft.MANUELL, nachher.eq!!.herkunft)
            assertEquals(Herkunft.VON_FYNN_BESTAETIGT, nachher.iq!!.herkunft)
            assertEquals(0, repo.confirmDerived("V2-108"))
            assertEquals(0, repo.confirmDerived("gibt-es-nicht"))
        }

    @Test
    fun `Sicherung exportiert alle Akten und spielt sie wieder ein`() =
        runBlocking {
            val troll = repo.get("V2-090")!!
            repo.save(troll.copy(iq = Angabe(GanzzahlSpanne(95, 105), "Fynn")))
            val json = repo.exportJson()
            assertTrue(json.contains("\"name\": \"Troll\""))
            assertTrue(json.contains("\"herkunft\": \"MANUELL\""))
            val gesichert = repo.observeProfiles().first().mapValues { it.value.copy(revision = 0, updatedAt = 0) }

            repo.save(repo.get("V2-090")!!.copy(iq = Angabe(GanzzahlSpanne(1, 2), "Unsinn")))
            assertEquals(6, repo.importJson(json))
            val wieder = repo.observeProfiles().first().mapValues { it.value.copy(revision = 0, updatedAt = 0) }
            assertEquals(gesichert, wieder)
            assertEquals(187, db.creatureDao().count())
        }

    @Test
    fun `ungueltige Sicherung aendert nichts`() =
        runBlocking {
            val vorher = db.creatureProfileDao().getAll()
            assertTrue(
                runCatching {
                    repo.importJson(
                        """{"format":"ruff.kreaturen-akte","formatVersion":1,"profile":[{"creatureId":"V2-090","iq":{"min":9,"max":1}}]}""",
                    )
                }.isFailure,
            )
            assertEquals(vorher, db.creatureProfileDao().getAll())
        }
}

/** Liest die gespeicherte Herkunft eines bewusst geleerten Felds direkt aus der Zeile. */
private object CreatureProfileMappingProbe {
    suspend fun herkunftPuls(
        db: RuffDatabase,
        creatureId: String,
    ): Herkunft? = Herkunft.parse(db.creatureProfileDao().getByCreatureId(creatureId)?.pulsHerkunft)
}
