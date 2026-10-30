package de.ruff.dimension.domain

import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Die ausgelagerte Such-/Filterlogik liefert dieselben Ergebnisse wie die bisherige Inline-Logik. */
class LibraryFilterTest {
    private val assetsDir: File =
        listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
    private val creatures = File(assetsDir, AssetLoader.CREATURES_FILE).bufferedReader().use { AssetLoader.parseCreatures(it) }
    private val rules = File(assetsDir, AssetLoader.RULES_FILE).bufferedReader().use { AssetLoader.parseRules(it) }

    private val queries = listOf("", "   ", "fuchs", "Fuchs", "drache", "  DRACHE ", "feuer", "stufe", "a", "gibt-es-nicht-xyz")

    // Wörtliche Kopie der Logik aus MainActivity vor diesem PR (Stand 473240b).
    private fun oldCreatures(
        query: String,
        powerFilter: Int?,
    ): List<Creature> {
        val normalizedQuery = query.trim().lowercase()
        return creatures.filter { creature ->
            (powerFilter == null || creature.powerLevel == powerFilter) &&
                (
                    normalizedQuery.isEmpty() ||
                        creature.name.lowercase().contains(normalizedQuery) ||
                        creature.archetype.lowercase().contains(normalizedQuery) ||
                        creature.fields.values.any { it.lowercase().contains(normalizedQuery) }
                )
        }
    }

    private fun oldRules(query: String): List<RulePage> {
        val normalizedQuery = query.trim().lowercase()
        return rules.filter { rule ->
            normalizedQuery.isEmpty() ||
                rule.title.lowercase().contains(normalizedQuery) ||
                rule.blocks.any { it.text.lowercase().contains(normalizedQuery) }
        }
    }

    private fun oldLevels() =
        creatures
            .filter { it.powerLevel > 0 }
            .distinctBy { it.powerLevel }
            .sortedBy { it.powerLevel }

    @Test
    fun creatureSearchAndPowerFilterMatchOldLogic() {
        val levels = listOf<Int?>(null) + creatures.map { it.powerLevel }.distinct()
        for (query in queries) {
            for (level in levels) {
                assertEquals("Suche '$query', Stufe $level", oldCreatures(query, level), filterCreatures(creatures, query, level))
            }
        }
    }

    @Test
    fun ruleSearchMatchesOldLogic() {
        for (query in queries) {
            assertEquals("Suche '$query'", oldRules(query), filterRules(rules, query))
        }
    }

    @Test
    fun emptyQueryReturnsEverythingInOrder() {
        assertEquals(187, filterCreatures(creatures, "", null).size)
        assertEquals(creatures, filterCreatures(creatures, "  ", null))
        assertEquals(12, filterRules(rules, "").size)
    }

    @Test
    fun knownSearchTermsFindCreatures() {
        val fuchs = filterCreatures(creatures, "fuchs", null)
        val drache = filterCreatures(creatures, "drache", null)
        println("fuchs=${fuchs.size} drache=${drache.size}")
        assertTrue("'fuchs' findet Wesen", fuchs.isNotEmpty())
        assertTrue("'drache' findet Wesen", drache.isNotEmpty())
        assertEquals(fuchs, filterCreatures(creatures, " FUCHS ", null))
        assertTrue(filterCreatures(creatures, "gibt-es-nicht-xyz", null).isEmpty())
    }

    @Test
    fun powerLevelOptionsMatchOldMenu() {
        assertEquals(oldLevels(), powerLevelOptions(creatures))
        val options = powerLevelOptions(creatures).map { it.powerLevel }
        assertEquals(options.sorted(), options)
        assertTrue(options.all { it > 0 })
        for (level in options) {
            val filtered = filterCreatures(creatures, "", level)
            assertTrue(filtered.isNotEmpty() && filtered.all { it.powerLevel == level })
        }
    }
}
