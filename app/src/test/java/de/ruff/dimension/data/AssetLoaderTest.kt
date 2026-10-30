package de.ruff.dimension.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader

class AssetLoaderTest {
    @Test
    fun `fehlende Felder erhalten Standardwerte`() {
        val creatures = AssetLoader.parseCreatures(StringReader("""[{"name":"Testwesen"}]"""))

        val creature = creatures.single()
        assertEquals("Testwesen", creature.name)
        assertEquals("", creature.id)
        assertEquals("", creature.status)
        assertEquals("", creature.source)
        assertNull(creature.spielbar)
        assertEquals(emptyMap<String, String>(), creature.fields)
    }

    @Test
    fun `neue Felder werden aus dem JSON gelesen`() {
        val json =
            """
            [{"id":"V2-001","name":"Aesops Fuchs","source":"katalog_v2.json",
              "status":"Vorschlag","spielbar":true,"powerLevel":2,
              "portrait":"ruff_wesen_v2_001_aesops_fuchs_portrait"}]
            """.trimIndent()

        val creature = AssetLoader.parseCreatures(StringReader(json)).single()
        assertEquals("V2-001", creature.id)
        assertEquals("katalog_v2.json", creature.source)
        assertEquals("Vorschlag", creature.status)
        assertEquals(true, creature.spielbar)
        assertEquals(2, creature.powerLevel)
        assertEquals("ruff_wesen_v2_001_aesops_fuchs_portrait", creature.portrait)
    }

    @Test
    fun `Regelseiten mit Bloecken werden gelesen`() {
        val json = """[{"title":"Regel","source":"Regeln/a.html","blocks":[{"type":"h2","text":"Kopf"}]}]"""

        val page = AssetLoader.parseRules(StringReader(json)).single()
        assertEquals("Regel", page.title)
        assertEquals(listOf(RuleBlock(type = "h2", text = "Kopf")), page.blocks)
    }
}
