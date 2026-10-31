package de.ruff.dimension.data

import de.ruff.dimension.data.local.CreatureProfileSeeder
import de.ruff.dimension.domain.akte.Herkunft
import de.ruff.dimension.domain.akte.ProfilFeld
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.StringReader

/** Akten-Dateiformat und der mitgelieferte Muster-Seed. */
class AkteJsonTest {
    private val assetsDir: File =
        listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }

    private fun seed() = File(assetsDir, AkteJson.SEED_FILE).bufferedReader().use { AkteJson.read(it, Herkunft.ABGELEITET) }

    @Test
    fun `Muster-Seed ist gueltig und passt zur Seed-Version`() {
        val datei = seed()
        assertEquals(AkteJson.FORMAT_VERSION, datei.formatVersion)
        assertEquals(CreatureProfileSeeder.SEED_VERSION, datei.seedVersion)
        assertEquals(listOf("V2-090", "V2-006", "V2-115", "V2-108", "V2-078", "V2-112"), datei.profile.map { it.creatureId })
    }

    @Test
    fun `jede Muster-Akte gehoert zu einem vorhandenen Wesen`() {
        val ids =
            File(assetsDir, AssetLoader.CREATURES_FILE)
                .bufferedReader()
                .use { AssetLoader.parseCreatures(it) }
                .map { it.id }
                .toSet()
        seed().profile.forEach { assertTrue("${it.creatureId} fehlt in creatures.json", it.creatureId in ids) }
    }

    @Test
    fun `alle Muster-Werte sind vollstaendig, abgeleitet und begruendet`() {
        seed().profile.forEach { p ->
            assertEquals("${p.creatureId} unvollständig", ProfilFeld.entries.toList(), p.vorhandeneFelder)
            ProfilFeld.entries.forEach { feld ->
                val angabe = p.angabe(feld)!!
                assertEquals(Herkunft.ABGELEITET, angabe.herkunft)
                assertTrue("${p.creatureId}/$feld ohne Begründung", angabe.begruendung.isNotBlank())
            }
            assertTrue(p.quellen.isNotEmpty())
        }
    }

    @Test
    fun `Muster-Seed deckt Sonderfaelle ab`() {
        val byId = seed().profile.associateBy { it.creatureId }
        val draugr = byId.getValue("V2-078")
        assertTrue(draugr.pulsBpm!!.wert.istNull)
        assertEquals(null, draugr.blutdruck!!.wert.systolisch)
        assertTrue(byId.getValue("V2-112").wahnhaft!!.wert)
        assertTrue(
            byId
                .getValue("V2-115")
                .allergien!!
                .wert
                .isEmpty(),
        )
        assertFalse(byId.getValue("V2-090").wahnhaft!!.wert)
    }

    @Test
    fun `Sicherung schreibt Herkunft und liest sie unveraendert zurueck`() {
        val original =
            seed().profile.map { p ->
                p.mitHerkunft(ProfilFeld.IQ, Herkunft.MANUELL).mitHerkunft(ProfilFeld.GROESSE, Herkunft.VON_FYNN_BESTAETIGT)
            }
        val json = AkteJson.write(original, mapOf("V2-090" to "Troll"))
        assertTrue(json.contains("\"name\": \"Troll\""))
        val gelesen = AkteJson.read(StringReader(json))
        assertEquals(original.sortedBy { it.creatureId }, gelesen.profile)
    }

    private fun datei(profile: String) = """{"format":"ruff.kreaturen-akte","formatVersion":1,"seedVersion":1,"profile":[$profile]}"""

    @Test
    fun `ungueltige Dateien werden mit Grund abgelehnt`() {
        fun fails(json: String) = assertThrows(AkteJson.AkteFormatException::class.java) { AkteJson.read(StringReader(json)) }
        fails("""{"format":"anders","formatVersion":1,"profile":[]}""")
        fails("""{"format":"ruff.kreaturen-akte","formatVersion":99,"profile":[]}""")
        fails("kein json")
        fails(datei("""{"creatureId":"A"},{"creatureId":"A"}"""))
        fails(datei("""{"creatureId":""}"""))
        fails(datei("""{"creatureId":"A","groesseM":{"min":3,"max":1}}"""))
        fails(datei("""{"creatureId":"A","pulsBpm":{"min":-1}}"""))
        fails(datei("""{"creatureId":"A","verfassung":{"stufe":"UNBEKANNT"}}"""))
        fails(datei("""{"creatureId":"A","allergien":{"liste":[{"ausloeser":"x","art":"ALLERGIE","schwere":"EGAL"}]}}"""))
        fails(datei("""{"creatureId":"A","blutdruck":{"systolisch":120}}"""))
        fails(datei("""{"creatureId":"A","wahnhaft":{"begruendung":"ohne Wert"}}"""))
        fails(datei("""{"creatureId":"A","iq":{"min":100,"herkunft":"GERATEN"}}"""))
        val e = fails(datei("""{"creatureId":"X-1","groesseM":{"min":3,"max":1}}"""))
        assertTrue(e.message!!.contains("X-1"))
    }

    @Test
    fun `fehlende Herkunft gilt als abgeleitet, erzwungene Herkunft gewinnt`() {
        val json = datei("""{"creatureId":"A","iq":{"min":90,"max":110,"herkunft":"MANUELL"},"eq":{"min":50}}""")
        val frei = AkteJson.read(StringReader(json)).profile.single()
        assertEquals(Herkunft.MANUELL, frei.iq!!.herkunft)
        assertEquals(Herkunft.ABGELEITET, frei.eq!!.herkunft)
        assertEquals(50, frei.eq!!.wert.max)
        val seed = AkteJson.read(StringReader(json), Herkunft.ABGELEITET).profile.single()
        assertEquals(Herkunft.ABGELEITET, seed.iq!!.herkunft)
    }
}
