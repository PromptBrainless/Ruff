package de.ruff.dimension.domain

import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.repository.SearchHits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Filter „Nur Favoriten“ auf den echten Seed-Wesen, allein und kombiniert mit Suche und Machtstufe. */
class FavoritesFilterTest {
    private val assetsDir: File =
        listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
    private val creatures: List<Creature> =
        File(assetsDir, AssetLoader.CREATURES_FILE).bufferedReader().use { AssetLoader.parseCreatures(it) }

    @Test
    fun `ausgeschalteter Filter aendert nichts, auch mit gesetzten IDs`() {
        val ids = creatures.take(3).map { it.id }.toSet()
        assertEquals(creatures, filterCreatures(creatures, "", null, FavoritesFilter(only = false, ids = ids)))
        assertEquals(creatures, filterCreatures(creatures, "", null))
    }

    @Test
    fun `nur Favoriten behaelt Seed-Reihenfolge und ignoriert verwaiste IDs`() {
        val picked = listOf(creatures[10], creatures[2], creatures[40])
        val filter = FavoritesFilter(only = true, ids = picked.map { it.id }.toSet() + "verwaist")
        assertEquals(listOf(creatures[2], creatures[10], creatures[40]), filterCreatures(creatures, "", null, filter))
    }

    @Test
    fun `ohne Favoriten ist das Ergebnis leer`() {
        assertTrue(filterCreatures(creatures, "", null, FavoritesFilter(only = true)).isEmpty())
    }

    @Test
    fun `wirkt zusammen mit Machtstufe und Suchbegriff`() {
        val level = creatures.first { it.powerLevel > 0 }.powerLevel
        val ids = creatures.map { it.id }.toSet()
        val all = FavoritesFilter(only = true, ids = ids)
        assertEquals(filterCreatures(creatures, "", level), filterCreatures(creatures, "", level, all))

        val target = creatures.first()
        val onlyTarget = FavoritesFilter(only = true, ids = setOf(target.id))
        assertEquals(listOf(target), filterCreatures(creatures, target.name, null, onlyTarget))
        val otherLevel = creatures.first { it.powerLevel != target.powerLevel }.powerLevel
        assertTrue(filterCreatures(creatures, "", otherLevel, onlyTarget).isEmpty())
    }

    @Test
    fun `Volltext-Treffer werden ebenfalls auf Favoriten begrenzt`() {
        val target = creatures[3]
        val hits = SearchHits(query = target.name, creatureIds = creatures.map { it.id }, ruleIds = emptyList())
        val result = searchCreatures(creatures, target.name, null, hits, FavoritesFilter(only = true, ids = setOf(target.id)))
        assertEquals(listOf(target), result)
    }
}
