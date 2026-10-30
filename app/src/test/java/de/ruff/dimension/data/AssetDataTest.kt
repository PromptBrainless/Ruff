package de.ruff.dimension.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Prüft die gebündelten Seed-Daten direkt aus `src/main/assets` und
 * `src/main/res/drawable-nodpi`, ohne Android-Context und ohne Daten zu ändern.
 */
class AssetDataTest {
    private val mainDir: File =
        listOf(File("src/main"), File("app/src/main"))
            .firstOrNull { File(it, "assets").isDirectory }
            ?: error("src/main/assets nicht gefunden (Arbeitsverzeichnis: ${File(".").absolutePath})")

    private val creatures: List<Creature> by lazy {
        File(mainDir, "assets/${AssetLoader.CREATURES_FILE}").bufferedReader().use {
            AssetLoader.parseCreatures(it)
        }
    }

    private val rules: List<RulePage> by lazy {
        File(mainDir, "assets/${AssetLoader.RULES_FILE}").bufferedReader().use {
            AssetLoader.parseRules(it)
        }
    }

    private val drawableNames: Set<String> by lazy {
        File(mainDir, "res/drawable-nodpi")
            .listFiles()
            .orEmpty()
            .filter { it.isFile }
            .map { it.nameWithoutExtension }
            .toSet()
    }

    @Test
    fun `creatures json enthaelt 187 Wesen`() {
        assertEquals(EXPECTED_CREATURES, creatures.size)
    }

    @Test
    fun `alle Wesen haben eine nicht leere und eindeutige ID`() {
        val blank = creatures.filter { it.id.isBlank() }.map { it.name }
        assertTrue("Wesen ohne ID: $blank", blank.isEmpty())

        val duplicates =
            creatures
                .groupingBy { it.id }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        assertTrue("Doppelte IDs: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `neue Felder status spielbar und source werden gelesen`() {
        val missing =
            creatures
                .filter { it.status.isBlank() || it.source.isBlank() || it.spielbar == null }
                .map { it.id }
        assertTrue("Wesen mit fehlendem status/source/spielbar: $missing", missing.isEmpty())
    }

    @Test
    fun `rules json enthaelt 12 Regelseiten`() {
        assertEquals(EXPECTED_RULE_PAGES, rules.size)
    }

    @Test
    fun `jede nicht leere Bildreferenz existiert in drawable-nodpi`() {
        val missing =
            creatures.flatMap { creature ->
                listOf(
                    "portrait" to creature.portrait,
                    "icon" to creature.icon,
                    "cover" to creature.cover,
                ).filter { (_, ref) -> ref.isNotBlank() && ref !in drawableNames }
                    .map { (kind, ref) -> "${creature.id} $kind=$ref" }
            }
        assertTrue("Bildreferenzen ohne Datei: $missing", missing.isEmpty())
    }

    @Test
    fun `teilweise bebilderte Wesen sind nur die bekannten Ausnahmen`() {
        // Wesen mit mindestens einem, aber nicht allen drei Bildfeldern.
        // Bekannt: V2-I-012 Aziza hat Porträt und Cover, aber kein Icon
        // (docs/berichte/08-bilder-v2.md); die Liste zeigt dort die Glyphe.
        val partial =
            creatures
                .filter { creature ->
                    val refs = listOf(creature.portrait, creature.icon, creature.cover)
                    refs.any { it.isNotBlank() } && refs.any { it.isBlank() }
                }.map { it.id }
                .toSet()
        assertEquals(KNOWN_PARTIAL_IMAGE_IDS, partial)
    }

    private companion object {
        const val EXPECTED_CREATURES = 187
        const val EXPECTED_RULE_PAGES = 12
        val KNOWN_PARTIAL_IMAGE_IDS = setOf("V2-I-012")
    }
}
