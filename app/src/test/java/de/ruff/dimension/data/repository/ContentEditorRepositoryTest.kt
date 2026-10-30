package de.ruff.dimension.data.repository

import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RuleBlock
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.local.ContentEdit
import de.ruff.dimension.data.local.ContentEditStore
import de.ruff.dimension.domain.DataOrigin
import de.ruff.dimension.domain.LibraryData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentEditorRepositoryTest {
    private val store = InMemoryContentEditStore()
    private val repository = RoomContentEditorRepository(store)
    private val originalCreature = Creature(id = "creature-1", name = "Original")
    private val originalRule = RulePage(title = "Original", source = "rules/original", blocks = listOf(RuleBlock("p", "Text")))
    private val seedData = LibraryData(listOf(originalCreature), listOf(originalRule), DataOrigin.DATABASE)

    @Test
    fun savesAndAppliesLocalChangesWithoutChangingSeedModels() =
        runBlocking {
            repository.saveCreature(originalCreature.copy(name = "Bearbeitet"))
            repository.saveRule(originalRule.copy(title = "Bearbeitet"))

            val edited = repository.apply(seedData)

            assertEquals("Original", originalCreature.name)
            assertEquals("Original", originalRule.title)
            assertTrue(edited.hasLocalEdits)
            assertEquals("Bearbeitet", edited.creatures.single().name)
            assertEquals("Bearbeitet", edited.rules.single().title)
        }

    @Test
    fun restoringRemovesOverridesAndShowsSeedModelsAgain() =
        runBlocking {
            repository.saveCreature(originalCreature.copy(name = "Bearbeitet"))
            repository.saveRule(originalRule.copy(title = "Bearbeitet"))

            repository.restoreCreature(originalCreature.id)
            repository.restoreRule(originalRule.source)

            val restored = repository.apply(seedData)

            assertEquals(seedData, restored)
            assertFalse(restored.hasLocalEdits)
        }

    private class InMemoryContentEditStore : ContentEditStore {
        private val edits = mutableMapOf<Pair<String, String>, ContentEdit>()

        override suspend fun getAll(): List<ContentEdit> = edits.values.toList()

        override suspend fun upsert(edit: ContentEdit) {
            edits[edit.contentType to edit.refId] = edit
        }

        override suspend fun delete(
            contentType: String,
            refId: String,
        ) {
            edits.remove(contentType to refId)
        }
    }
}
